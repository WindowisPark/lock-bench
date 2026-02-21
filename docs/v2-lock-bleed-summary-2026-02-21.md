# v2 락 블리드(Lock Bleed) 실험 결과 (2026-02-21)

## 검증 가설

> **비관적 락(Pessimistic Lock)이 커넥션을 장기 점유할 때, 락과 무관한 읽기 API도 차단되는가?**

HikariCP 기본 풀 크기는 **10**. 200개 동시 스레드가 `SELECT FOR UPDATE` + sleep으로
커넥션을 장기 점유하면, 무관한 읽기 요청도 커넥션 대기 큐에 쌓여 지연·실패할 것이다.

---

## 실험 설정

| 항목 | 값 |
|---|---|
| 프로파일 | `mysql-redis` (MySQL + HikariCP pool=10 기본값) |
| processingDelayMillis | **100ms** (각 락 전략이 처리 후 sleep) |
| 쓰기 부하 | 1회 실험 실행 (200 PLATFORM 스레드 × 100ms holdMillis) |
| 읽기 프로브 | 초당 10req, 90초간, 최대 20 VUs |
| 읽기 시작 오프셋 | 쓰기 실험 시작 3초 후 |
| 읽기 임계값 | read p95 < 500ms, fail rate < 5% |
| 결과 경로 | `src/main/resources/k6/results/bleed-20260221-133028` |

### 락 전략별 커넥션 점유 방식

| 전략 | 구현 방식 | DB 커넥션 점유 기간 |
|---|---|---|
| NO_LOCK | `decreaseWithoutLock()` 후 sleep | 쿼리 후 즉시 반환 |
| OPTIMISTIC_LOCK | `decreaseWithOptimisticLock()` 후 sleep | 쿼리 후 즉시 반환 |
| PESSIMISTIC_LOCK | `@Transactional` 내에서 `SELECT FOR UPDATE` + sleep | **sleep 종료까지 커넥션 + 행 락 유지** |
| REDIS_DISTRIBUTED_LOCK | Redis setnx 후 `decreaseWithoutLock()`, sleep | 쿼리 후 즉시 반환 (Redis 키만 점유) |

---

## 실험 결과

| 전략 | 읽기 p95 | 읽기 실패율 | 쓰기 완료 시간 | 임계값 | 결과 |
|---|---|---|---|---|---|
| NO_LOCK | **15.06ms** | **0.00%** | 5.3s | ✓ p95<500, ✓ fail<5% | PASS |
| OPTIMISTIC_LOCK | **9.33ms** | **0.00%** | 2.9s | ✓ p95<500, ✓ fail<5% | PASS |
| PESSIMISTIC_LOCK | **30,020ms** | **20.00%** | 30.2s | ✗ p95<500, ✗ fail<5% | **FAIL** |
| REDIS_DISTRIBUTED_LOCK | **9.62ms** | **0.00%** | 0.2s | ✓ p95<500, ✓ fail<5% | PASS |

### 상세 수치

**PESSIMISTIC_LOCK 상세:**
```
http_req_duration{type:read}: avg=19.8s  min=1.86s  med=20.29s  max=30.02s
                               p(90)=30.01s  p(95)=30.02s
http_req_failed{type:read}:   20.00% (18 out of 90)
dropped_iterations:           810  (VU 20개 포화, 요청 처리 불가)
vus:                          최대 21/21 (preAllocatedVUs 상한 도달)
```

**NO_LOCK 상세:**
```
http_req_duration{type:read}: avg=21.97ms  min=4.15ms  med=8.3ms  max=807ms
                               p(90)=12.15ms  p(95)=15.06ms
http_req_failed{type:read}:   0.00%
```

---

## 원인 분석

### PESSIMISTIC_LOCK: 커넥션 풀 고갈 연쇄 반응

```
[쓰기 스레드 T1~T200]          [읽기 요청 R1~Rn]
     ↓                               ↓
 SELECT FOR UPDATE            findSnapshot() 호출
(HikariCP 커넥션 획득)          (readOnly=true)
     ↓                               ↓
 Thread.sleep(100ms)          HikariCP 커넥션 대기 큐
(커넥션 + 행 락 유지)               ↓
     ↓                          30s 대기 후 타임아웃
 unlock + 커넥션 반환              → 20% 실패
```

- 200개 스레드가 `SELECT FOR UPDATE` 직렬화로 순차 처리: 200 × 100ms ≈ **20초**
- 그 동안 HikariCP pool(10) 전체 소진 → 읽기 요청이 커넥션 대기
- 대기 시간이 HikariCP 기본 connectionTimeout(30s) 도달 → 타임아웃 → 20% 실패
- read probe VU 20개 전부 포화 → 810개 요청 dropped (처리 자체 불가)

### 다른 전략: 커넥션 조기 반환

- **NO_LOCK / OPTIMISTIC**: DB 쿼리 직후 커넥션 반환. sleep은 커넥션 없이 수행.
  읽기가 충분한 커넥션 확보 → p95 9~15ms, 실패 0%.
- **REDIS**: Redis 키만 장기 점유, DB 커넥션은 쿼리 후 즉시 반환.
  읽기 영향 없음 → p95 9.62ms, 실패 0%.
  (단, Redis 락 직렬화 + 경합으로 쓰기 자체는 빠르게 실패/포기해 0.2s 완료)

---

## 가설 검증 결과

| 가설 | 결과 |
|---|---|
| PESSIMISTIC_LOCK이 커넥션 풀을 고갈시킨다 | **검증됨** — p95 30s, fail 20% |
| 읽기 API가 차단된다 | **검증됨** — 무관한 GET /stock도 30s 타임아웃 |
| REDIS_DISTRIBUTED_LOCK은 읽기에 영향 없다 | **검증됨** — p95 9.62ms, fail 0% |
| NO_LOCK / OPTIMISTIC은 읽기에 영향 없다 | **검증됨** — p95 15ms 이하, fail 0% |

---

## 실무 시사점

1. **PESSIMISTIC_LOCK은 락 범위를 최소화해야 한다**: 비즈니스 로직(외부 API 호출, 이벤트 발행 등)을 트랜잭션 내에서 실행하면 무관한 읽기 API까지 차단됨.

2. **HikariCP pool 크기를 과소평가하면 안 된다**: 기본값 10은 고부하 환경에서 즉시 한계. 예상 동시 트랜잭션 수에 맞게 설정 필요.

3. **REDIS 분산 락의 장점**: DB 커넥션을 조기 반환하므로 쓰기 부하가 읽기를 차단하지 않음. 단, 스레드(대기 큐) 압박은 여전히 존재.

4. **측정 방법**: k6 multi-scenario + `tags: { type: "read" }` 필터링으로 쓰기 부하 중 읽기 지연을 정량 측정 가능.

---

## 구현된 코드 변경

- `ExperimentRequest`: `processingDelayMillis` 필드 추가 (`@Min(0) @Max(10000)`)
- `StockLockStrategy`: `placeOrder(..., long holdMillis)` 시그니처 변경
- `PessimisticLockStrategy`: `@Transactional` 추가 (커넥션 점유 유지가 핵심)
- `ExperimentController`: `GET /api/experiments/stock/{productId}` 읽기 전용 엔드포인트 추가
- `application.yml`: `spring.profiles.group.mysql-redis: [mysql, mysql-redis]` — mysql-redis 프로파일에서 실제 MySQL 사용 보장
- k6: `s6-lock-bleed.js` (멀티 시나리오), `run-bleed-test.ps1` (자동화 스크립트)
