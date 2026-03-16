# v2 JFR + MySQL Slow Query 병목 분석 (2026-03-16 실행)

작성 일자: 2026-03-16
실험 환경: Docker MySQL 8.0 (port 13306) + Docker Redis 7 (port 6379), Java 21 Temurin, Windows 11

---

## 1. 분석 목표

PESSIMISTIC_LOCK 전략에서 JFR(Java Flight Recorder)과 MySQL performance_schema/slow query log를 통해
JVM 레벨 스레드 대기 및 DB 레벨 락 병목을 정량화한다.

---

## 2. 실험 설정

### JFR 설정 (`build.gradle.kts`)

```kotlin
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs(
        "-XX:+FlightRecorder",
        "-XX:StartFlightRecording=duration=120s,filename=jfr/lockbench.jfr,settings=profile"
    )
}
```

### MySQL Slow Query 활성화

```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.1;
```

### 실험 시나리오

| 항목 | 값 |
|---|---|
| 시나리오 | `s4-pessimistic-stability.js` |
| 전략 | PESSIMISTIC_LOCK |
| 스레드 모델 | PLATFORM |
| totalRequests | 3000 |
| initialStock | 3000 |
| concurrency | 200 |
| 총 소요 시간 | 22.8초 |

---

## 3. MySQL 쿼리 분석 (performance_schema)

### 3.1 쿼리별 실행 통계

| 쿼리 | 실행 횟수 | 평균 쿼리시간 | 최대 쿼리시간 | 총 Lock 대기 | 평균 Lock 대기 |
|---|---|---|---|---|---|
| `SELECT ... FOR UPDATE` | 13,000 | **45.76ms** | 512.10ms | 591,788ms | **45.52ms** |
| `UPDATE stocks SET quantity = quantity - ?` | 12,972 | **19.86ms** | 182.78ms | 254,107ms | **19.59ms** |
| `UPDATE stocks SET quantity=?, version=?` (OPT) | 45,540 | 3.87ms | 273.60ms | 165,481ms | 3.63ms |
| `COMMIT` | 116,845 | 0.93ms | 271.08ms | 0ms | 0ms |
| `SELECT ... (read-only)` | 45,505 | 0.18ms | 12.06ms | 0.11ms | 0.0024ms |

### 3.2 핵심 발견: Lock_time ≈ Query_time

**`SELECT ... FOR UPDATE`의 Query_time 45.76ms 중 Lock_time이 45.52ms — 99.5%가 순수 InnoDB 행 락 대기.**

이는 쿼리 자체의 실행 비용(인덱스 탐색, 데이터 읽기)이 ~0.24ms에 불과하며,
나머지 시간은 전부 다른 트랜잭션이 같은 행의 배타 락을 해제하기를 기다리는 것임을 의미한다.

### 3.3 Slow Query 분포 (threshold: 100ms)

| 구간 | 건수 | 비고 |
|---|---|---|
| 100~200ms | 다수 | 일반적인 경합 구간 |
| 200~400ms | 중간 | 고경합 구간 |
| 400~512ms | 5건 | 최악 케이스 |

**최대 Slow Query:** 512ms (Lock_time 511ms) — 약 100개 트랜잭션이 직렬 대기열에 쌓인 상태.

---

## 4. JFR 분석 결과

### 4.1 CPU 프로파일

| 시간 구간 | JVM User | JVM System | Machine Total | 상태 |
|---|---|---|---|---|
| 11:50:32 (부팅) | 28.26% | 7.06% | 100% | 초기화 |
| 11:50:33~36 (부팅+워밍업) | 9~12% | 3~6% | 45~52% | 워밍업 |
| **11:50:37~59 (부하 구간)** | **0~0.94%** | **0~0.58%** | **27~54%** | **부하 중** |
| 11:51:00~ (유휴) | 0~0.19% | 0~0.38% | 27~40% | 유휴 |

**핵심: 부하 구간에서 JVM CPU 사용률이 0~1%로 극히 낮다.**

200개 스레드가 3000건을 처리하는 22.8초 동안 CPU가 거의 쉬고 있다는 것은,
스레드 대부분이 **DB 락 대기(I/O blocked)** 상태에서 CPU를 양보하고 있다는 의미이다.

### 4.2 GC (G1)

| 항목 | 값 |
|---|---|
| GC 횟수 (Young) | 17회 |
| GC 횟수 (Old) | 6회 |
| 최대 GC Pause | 4.95ms |
| 총 GC Pause 시간 | ~75ms (120초 중) |
| 힙 사용 피크 | ~36MB |

**GC는 전혀 병목이 아님.** G1 Pause가 최대 5ms이고, 전체 120초 중 총 75ms만 차지.

### 4.3 JFR Lock Events

| 이벤트 타입 | 발생 횟수 | 비고 |
|---|---|---|
| jdk.ThreadPark | 290 | HikariCP 커넥션 대기, CountDownLatch 대기 포함 |
| jdk.ThreadSleep | 1166 | HikariCP connection adder (30ms 간격) |
| jdk.JavaMonitorWait | 1 | JFR scheduler (무관) |
| jdk.JavaMonitorEnter | 0 | synchronized 경합 없음 |

**ThreadPark 상세:**
- `CountDownLatch$Sync` 대기: HTTP 요청 스레드가 전체 실험 완료를 기다리는 패턴 (291ms)
- `AbstractQueuedSynchronizer$ConditionObject`: HikariCP 풀에서 커넥션 대기
- `CompletableFuture$Signaller`: Lettuce(Redis) 응답 대기

**JVM 레벨 synchronized 경합(JavaMonitorEnter)은 0건** — JVM 내부 락은 전혀 병목이 아님.

### 4.4 스레드 분석

JFR에서 `jdk.NativeMethodSample` 5,617건 중 대부분이 `Unsafe.park()` — 스레드가 park 상태에서 대기 중.
이는 JDBC 커넥션 풀(HikariCP)에서 `getConnection()` 호출 시 풀이 고갈되어 대기하거나,
MySQL이 행 락을 잡고 있어 JDBC 드라이버가 응답을 기다리는 패턴.

---

## 5. 전체 스택 병목 지도

```
[k6 Client] ──HTTP──▶ [Tomcat NIO Thread] ──submit──▶ [FixedThreadPool (200)]
                                                          │
                                                    ┌─────┴─────┐
                                                    ▼           ▼
                                              getConnection()  (HikariCP pool=10)
                                                    │
                                              ┌─────┴─────────────────────────┐
                                              ▼ <<< 병목 1: 커넥션 풀 대기 >>>│
                                         SELECT ... FOR UPDATE                │
                                              │                               │
                                        ┌─────┴───────────────────────┐       │
                                        ▼ <<< 병목 2: InnoDB 행 락 >>> │       │
                                   Lock_time 평균 45ms, 최대 512ms    │       │
                                        │                             │       │
                                        ▼                             │       │
                                   UPDATE stocks ...                  │       │
                                        │                             │       │
                                   Lock_time 평균 20ms, 최대 183ms   │       │
                                        │                             │       │
                                        ▼                             │       │
                                     COMMIT                           │       │
                                        │                             │       │
                                        ▼                             │       │
                                   returnConnection()  ◀──────────────┘       │
                                        │                                     │
                                        └─────────────────────────────────────┘
```

### 병목 계층 요약

| 계층 | 병목 | 기여도 | 증거 |
|---|---|---|---|
| **DB: InnoDB 행 락** | `SELECT FOR UPDATE` Lock_time | **~70%** | Query_time의 99.5%가 Lock_time |
| **JVM: HikariCP 풀 고갈** | 커넥션 10개 < 동시 요청 200개 | **~30%** | ThreadPark on AQS$ConditionObject |
| JVM: GC | G1 Pause | **~0%** | 최대 5ms, 총 75ms/120s |
| JVM: synchronized | Monitor 경합 | **0%** | JavaMonitorEnter 0건 |

---

## 6. 병목 결론

### 주 병목: InnoDB 행 락 직렬화

PESSIMISTIC_LOCK (`SELECT ... FOR UPDATE`)은 **동일 행에 대한 모든 트랜잭션을 직렬화**한다.
200개 스레드가 동일 product_id=1에 대해 경합하면:

- 한 시점에 1개 트랜잭션만 진행 가능
- 나머지 199개는 InnoDB의 lock wait queue에서 대기
- 평균 대기: 45ms, 최악 512ms
- **JVM CPU가 1% 미만으로 떨어지는 것은 모든 스레드가 I/O blocked 상태이기 때문**

### 부 병목: HikariCP 풀 크기 (10)

HikariCP 기본 풀 크기 10개에 200개 스레드가 경합:
- 190개 스레드는 커넥션을 얻기도 전에 대기
- 이 대기가 InnoDB 행 락 대기와 **겹쳐서** 직렬화 효과를 증폭

단, 풀 크기를 늘려도 **InnoDB 행 락의 직렬화는 해소되지 않으므로** 근본 해결은 아님.
오히려 풀이 크면 더 많은 커넥션이 DB에서 행 락 대기를 하게 되어 MySQL 부하만 증가.

### Lock Bleed 실험과의 연계

`v2-lock-bleed-summary-2026-02-21.md`에서 PESSIMISTIC_LOCK이 읽기 API p95를 30초로 증가시킨 것과
본 분석 결과가 정합:

- **원인**: `SELECT FOR UPDATE` → `UPDATE` → `COMMIT` 트랜잭션 동안 커넥션을 점유
- **결과**: HikariCP 풀 10개가 모두 장기 트랜잭션에 묶임 → 읽기 API도 커넥션 획득 불가
- **JFR 증거**: CPU 1%인데 처리량은 131 rps (3000/22.8s) — 전형적인 "락 대기 직렬화" 패턴

---

## 7. 실무 시사점

### PESSIMISTIC_LOCK 사용 가이드라인

| 상황 | 권장 |
|---|---|
| 동시성 ≤ 10, 정합성 필수 | PESSIMISTIC_LOCK 사용 가능 |
| 동시성 > 50, 동일 행 경합 | **사용 금지** — OPTIMISTIC 또는 Redis 분산락 |
| 읽기/쓰기 혼합 워크로드 | **사용 금지** — Lock Bleed로 읽기 차단됨 |

### 완화 전략 (PESSIMISTIC_LOCK을 써야 하는 경우)

1. **트랜잭션 범위 최소화**: `SELECT FOR UPDATE` → `UPDATE` → 즉시 `COMMIT`
2. **HikariCP 풀 크기 적정화**: concurrency의 50% 이하 (200 동시성이면 풀 ≤ 100)
   - 풀이 너무 크면 MySQL에 과도한 행 락 대기 커넥션이 쌓임
3. **쿼리 레벨 timeout 설정**: `innodb_lock_wait_timeout` (기본 50초) 축소
4. **모니터링**: `performance_schema.events_waits_*`에서 Lock_time 추이 관측

---

## 8. 수치 요약

| 지표 | 값 | 의미 |
|---|---|---|
| SELECT FOR UPDATE 평균 Lock_time | 45.52ms | 한 행의 락 대기 평균 |
| SELECT FOR UPDATE 최대 Lock_time | 512ms | 100+ 트랜잭션 직렬 대기열 |
| Lock_time / Query_time 비율 | 99.5% | 쿼리 비용 ≈ 0, 락 비용 ≈ 전부 |
| 부하 중 JVM CPU | 0~1% | CPU 유휴 — 스레드 전원 I/O blocked |
| GC 최대 Pause | 4.95ms | GC 무관 |
| JavaMonitorEnter | 0건 | JVM 내부 동기화 경합 없음 |
| Slow Query (>100ms) | ~280건 / 13,000건 (2.2%) | 고경합 순간 |
| 총 처리량 | 131 rps (3000 / 22.8s) | 직렬화로 인한 처리량 제한 |
