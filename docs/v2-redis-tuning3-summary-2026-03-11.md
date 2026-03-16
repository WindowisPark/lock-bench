# v2 Redis 튜닝3 실험 결과 및 분석 (2026-03-16 실행)

작성 일자: 2026-03-16
실험 환경: Docker MySQL 8.0 (port 13306) + Docker Redis 7 (port 6379), Java 21 Temurin, Windows 11

---

## 1. 변경 내용 (튜닝2 버그수정 → 튜닝3)

| 파라미터 | 튜닝2 (버그수정) | 튜닝3 | 변경 근거 |
|---|---|---|---|
| ttl-millis | 8000 | 8000 | 유지 |
| max-retries | 10 | **15** | 재시도 기회 +50% |
| base-backoff-millis | 10 | 10 | 유지 |
| max-backoff-millis | 200 | **500** | 고경합 구간 분산 확대 |
| concurrency | 200 | 200 | 유지 |

**Backoff 범위 변화:**
- attempt 0: `[0, 10ms]`
- attempt 1: `[0, 20ms]`
- attempt 2: `[0, 40ms]`
- attempt 3: `[0, 80ms]`
- attempt 4: `[0, 160ms]`
- attempt 5+: `[0, 500ms]` (cap — 기존 200ms → 500ms)

가설: cap을 500ms로 올리면 고경합 구간에서 retry 시간 분포가 넓어져 경합이 분산되고, retry 횟수 증가(15)와 결합하면 LOCK_TIMEOUT이 줄어들 것으로 기대.

---

## 2. 실험 결과

### Run 1

| Thread | Strategy | Success | Fail | Reason | p50 | p95 | p99 | TPS |
|--------|----------|---------|------|--------|-----|-----|-----|-----|
| PLATFORM | NO_LOCK | 1000 (100%) | 0 | - | 687ms | 796ms | 1354ms | 274.5 |
| PLATFORM | OPTIMISTIC | 471 (47.1%) | 529 | VERSION_CONFLICT | 1502ms | 2591ms | 3021ms | 63.9 |
| PLATFORM | PESSIMISTIC | 1000 (100%) | 0 | - | 936ms | 987ms | 1864ms | 211.6 |
| **PLATFORM** | **REDIS** | **751 (75.1%)** | **249** | **LOCK_TIMEOUT** | **573ms** | **2953ms** | **3367ms** | **106.6** |
| VIRTUAL | NO_LOCK | 1000 (100%) | 0 | - | 25ms | 33ms | 64ms | 296.5 |
| VIRTUAL | OPTIMISTIC | 558 (55.8%) | 442 | VERSION_CONFLICT | 519ms | 1960ms | 1978ms | 71.4 |
| VIRTUAL | PESSIMISTIC | 1000 (100%) | 0 | - | 33ms | 43ms | 55ms | 228.7 |
| **VIRTUAL** | **REDIS** | **731 (73.1%)** | **269** | **LOCK_TIMEOUT** | **542ms** | **2971ms** | **3332ms** | **106.6** |

### Run 2 (재현 확인)

| Thread | Strategy | Success | Fail | Reason | p50 | p95 | p99 | TPS |
|--------|----------|---------|------|--------|-----|-----|-----|-----|
| PLATFORM | NO_LOCK | 1000 (100%) | 0 | - | 624ms | 703ms | 1083ms | 310.5 |
| PLATFORM | OPTIMISTIC | 459 (45.9%) | 541 | VERSION_CONFLICT | 1469ms | 2422ms | 2722ms | 62.2 |
| PLATFORM | PESSIMISTIC | 1000 (100%) | 0 | - | 1057ms | 1771ms | 2345ms | 166.2 |
| **PLATFORM** | **REDIS** | **739 (73.9%)** | **261** | **LOCK_TIMEOUT** | **735ms** | **3030ms** | **3353ms** | **101.9** |
| VIRTUAL | NO_LOCK | 1000 (100%) | 0 | - | 25ms | 45ms | 82ms | 275.4 |
| VIRTUAL | OPTIMISTIC | 566 (56.6%) | 434 | VERSION_CONFLICT | 756ms | 2410ms | 2440ms | 52.0 |
| VIRTUAL | PESSIMISTIC | 1000 (100%) | 0 | - | 34ms | 60ms | 73ms | 210.4 |
| **VIRTUAL** | **REDIS** | **741 (74.1%)** | **259** | **LOCK_TIMEOUT** | **656ms** | **2942ms** | **3250ms** | **108.9** |

### 평균 (2회)

| Thread | Strategy | Avg Success Rate | Avg p95 | Avg TPS |
|--------|----------|-----------------|---------|---------|
| PLATFORM | REDIS | **74.5%** | **2992ms** | **104.3** |
| VIRTUAL | REDIS | **73.6%** | **2957ms** | **107.8** |

---

## 3. 튜닝2(버그수정) 대비 비교

| 항목 | 튜닝2 (버그수정) | 튜닝3 | 변화 | 판정 |
|---|---|---|---|---|
| PLATFORM Redis SuccRate | 98.8% | 74.5% | **-24.3%p ↓↓** | FAIL |
| VIRTUAL Redis SuccRate | 84.8% | 73.6% | **-11.2%p ↓** | FAIL |
| PLATFORM Redis p95 | 540.8ms | 2992ms | **+454% ↑↑** | FAIL |
| VIRTUAL Redis p95 | 819.3ms | 2957ms | **+261% ↑↑** | FAIL |
| PLATFORM Redis Throughput | 1187.6 rps | 104.3 rps | **-91% ↓↓** | FAIL |
| VIRTUAL Redis Throughput | 721.7 rps | 107.8 rps | **-85% ↓↓** | FAIL |

---

## 4. 목표 대비 판정

| 기준 | 목표 | 실측 | 결과 |
|---|---|---|---|
| VIRTUAL REDIS 성공률 | ≥ 99% | 73.6% | **FAIL** |
| VIRTUAL REDIS p95 | ≤ 500ms | 2957ms | **FAIL** |

**종합 판정: FAIL — 튜닝3은 튜닝2 대비 전면 악화**

---

## 5. 원인 분석

### 5.1 max-backoff 500ms의 역효과: "슬롯 낭비" 문제

튜닝3의 핵심 변경은 `max-backoff-millis`를 200ms → 500ms로 올린 것이다.
의도는 고경합 구간에서 retry 시점을 넓게 분산시키는 것이었으나, **실제로는 반대 효과**가 발생했다.

**메커니즘:**
1. Redis 분산락 TTL = 8초. 락 보유 시간(DB 쿼리)은 ~1-5ms 수준.
2. 락이 풀리면 즉시 다음 요청이 획득할 수 있는 "빈 슬롯"이 생긴다.
3. backoff가 `[0, 500ms]`로 넓어지면, 대부분의 스레드가 0~500ms 중 랜덤 시점에 retry를 시도한다.
4. **빈 슬롯이 열려 있는 시간(~1ms) 대비 retry 간격(평균 ~250ms)이 너무 길다.**
5. 결과: 빈 슬롯을 놓치는 스레드가 급증 → 15회 retry를 모두 소진하고도 LOCK_TIMEOUT.

**비유:** 회전문이 1초에 100번 열리는데(빈 슬롯), 사람들이 평균 250ms마다 한 번씩 시도하면 대부분 문이 닫혀 있을 때 도착한다.

### 5.2 튜닝2가 더 나았던 이유

튜닝2(backoff max 200ms)에서는:
- attempt 4+ 범위: `[0, 200ms]` → 평균 retry 간격 ~100ms
- 빈 슬롯 매칭 확률이 상대적으로 높았음
- retries 10회 × 100ms 평균 = ~1초 내에 대부분 retry 소진 → 빠른 실패/성공 결정

튜닝3(backoff max 500ms)에서는:
- attempt 5+ 범위: `[0, 500ms]` → 평균 retry 간격 ~250ms
- retries 15회 × 250ms 평균 = 최대 ~3.75초를 retry에 소모
- **이 시간 동안 다른 스레드도 계속 retry** → retry 스레드가 누적되어 경합 악화 (thundering herd의 느린 버전)

### 5.3 TPS 90% 폭락의 원인

튜닝2: 1187 rps → 튜닝3: 104 rps (**-91%**)

- 성공하는 요청의 소요 시간이 크게 증가 (p50: 540 → 650ms, p95: 540 → 2990ms)
- 15회 retry × 긴 backoff로 인해 **한 요청이 스레드를 점유하는 시간이 늘어남**
- PLATFORM의 경우 FixedThreadPool(200)이 모두 긴 retry 루프에 빠져 새 요청 처리 불가
- 결과적으로 "retry 증가 → 점유 시간 증가 → 동시 retry 스레드 증가 → 경합 격화"의 악순환

### 5.4 VIRTUAL vs PLATFORM 차이가 사라진 이유

튜닝2에서는 PLATFORM(98.8%) > VIRTUAL(84.8%)의 차이가 있었으나,
튜닝3에서는 PLATFORM(74.5%) ≈ VIRTUAL(73.6%)로 거의 동일해졌다.

- 튜닝2에서 PLATFORM이 유리했던 이유: 짧은 backoff(200ms cap)에서 OS 스케줄러의 선점 방식이 Redis 연결 재사용에 유리
- 튜닝3에서는 backoff 자체가 길어져 **스케줄링 방식의 차이보다 "슬롯 놓침"이 지배적 요인**이 됨
- 양쪽 모두 같은 비율로 빈 슬롯을 놓치므로 성공률이 수렴

---

## 6. 결론

### 튜닝3 판정: FAIL

`max-backoff-millis`를 200ms → 500ms로 올린 것은 **역효과**였다.
분산 락의 특성상, 빈 슬롯 지속 시간이 매우 짧기 때문에 backoff를 넓히면 오히려 슬롯 매칭 확률이 떨어진다.

### Redis 분산락 concurrency=200의 구조적 한계 확인

| 튜닝 | 전략 | PLATFORM 성공률 | VIRTUAL 성공률 | 결론 |
|---|---|---|---|---|
| 튜닝1 | TTL↑, retries↑ | ~63% | ~56% | FAIL |
| 튜닝2 (버그수정) | full-jitter 수정 | 98.8% | 84.8% | WARN |
| **튜닝3** | **backoff cap↑, retries↑↑** | **74.5%** | **73.6%** | **FAIL (악화)** |

3회의 튜닝을 거쳤으나, concurrency=200에서 Redis 분산락 성공률 99% 달성은 **구조적으로 불가능**하다고 판단한다.

**근본 원인:** 단일 Redis 키에 200개 스레드가 동시 경합하면:
- 락 획득은 한 시점에 1개만 가능 (mutual exclusion)
- 빈 슬롯 지속 시간 ~1ms vs retry 간격 ~10-250ms → 대부분의 retry가 헛수고
- retry 횟수/간격을 조정해도 "200:1 경합비"라는 본질은 변하지 않음

### 최적 구성 확정

**튜닝2(버그수정) 파라미터가 현재 최선:**

```yaml
lockbench:
  redis-lock:
    ttl-millis: 8000
    max-retries: 10
    base-backoff-millis: 10
    max-backoff-millis: 200
```

---

## 7. 다음 액션

1. **concurrency 감소 실험** (v3 범위)
   - concurrency=100, 50에서 Redis 분산락 성공률 재측정
   - 99% 달성 가능한 임계 concurrency 수준 확인

2. **v2-closeout 갱신**
   - 튜닝3 FAIL 확정 반영
   - 권장 파라미터를 튜닝2(버그수정) 값으로 확정
   - Redis 분산락 concurrency ≤ 100 권장 명시

3. **v3 로드맵에 추가**
   - Redis Pub/Sub 기반 lock notification (polling → event-driven) 검토
   - Redisson 등 성숙한 분산락 라이브러리 벤치마크 비교
