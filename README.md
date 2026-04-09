# LockBench

Java 21 + Spring Boot 3 기반 동시성 벤치마크 플랫폼.  
Thread 모델(Platform/Virtual)과 Lock 전략(No/Optimistic/Pessimistic/Redis/Redisson)을 교체하며 성능을 비교하고, 재현 가능한 실험과 계측에 초점을 둡니다.

## 아키텍처

```text
io.lockbench
  ├─ api            # 실험 실행 REST API
  ├─ application    # 오케스트레이션, 메트릭 기록, 결과 저장
  ├─ domain         # 포트/도메인 모델 (StockAccessPort, OrderResult)
  ├─ concurrency    # Thread 모델 + Lock 전략 구현체
  │   ├─ thread     #   PlatformThreadModel, VirtualThreadModel
  │   └─ lock       #   NoLock, Optimistic, Pessimistic, Redis, Redisson
  └─ infra          # 스토리지 어댑터
      ├─ memory     #   InMemoryStockAccessAdapter (기본)
      ├─ mysql      #   MySqlStockAccessAdapter (JPA)
      └─ redis      #   Lettuce 클라이언트, Redisson 설정
```

## 지원 매트릭스

| | NO_LOCK | OPTIMISTIC | PESSIMISTIC | REDIS (Lettuce) | REDISSON (Pub-Sub) |
|---|:---:|:---:|:---:|:---:|:---:|
| **In-Memory** | v1 | v1 | v1 | v1 | - |
| **MySQL** | v3 | v3 | v3 | - | - |
| **MySQL + Redis** | - | - | - | v2 | v3 |

## 빠른 시작

### 1. 인메모리 (기본)

```bash
./gradlew bootRun
```

### 2. MySQL 단독

```bash
docker compose up -d mysql
./gradlew bootRun --args="--spring.profiles.active=mysql"
```

### 3. MySQL + Redis (Lettuce + Redisson)

```bash
docker compose up -d
./gradlew bootRun --args="--spring.profiles.active=mysql-redis"
```

### 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `HIKARI_MAX_POOL_SIZE` | 10 | HikariCP 최대 커넥션 풀 |
| `HIKARI_MIN_IDLE` | 10 | HikariCP 최소 유휴 커넥션 |
| `REDISSON_WAIT_TIME_MILLIS` | 10000 | Redisson 락 대기 시간 |
| `REDISSON_LEASE_TIME_MILLIS` | 8000 | Redisson 락 임대 시간 |

## 실험 API

### 단일 실행

`POST /api/experiments/run`

```json
{
  "threadModel": "VIRTUAL",
  "lockStrategy": "REDISSON_PUB_SUB_LOCK",
  "productId": 1,
  "initialStock": 10000,
  "quantity": 1,
  "totalRequests": 1000,
  "concurrency": 200,
  "optimisticRetries": 3,
  "processingDelayMillis": 0
}
```

### Lock 전략 값

`NO_LOCK` | `OPTIMISTIC_LOCK` | `PESSIMISTIC_LOCK` | `REDIS_DISTRIBUTED_LOCK` | `REDISSON_PUB_SUB_LOCK`

## k6 테스트 시나리오

```text
src/main/resources/k6/
  ├─ scenarios/
  │   ├─ run-single-combo.js      # 단일 조합 실행 (matrix에서 호출)
  │   ├─ s1-baseline.js           # 기본 베이스라인
  │   ├─ s2-high-contention.js    # 고경쟁 테스트
  │   ├─ s3-optimistic-retry.js   # Optimistic 재시도 집중
  │   ├─ s4-pessimistic-stability.js  # Pessimistic 안정성 (JFR 분석용)
  │   ├─ s5-matrix-run.js         # 매트릭스 API 호출
  │   ├─ s6-lock-bleed.js         # Lock Bleed 검증 (쓰기→읽기 간섭)
  │   ├─ s7-redis-concurrency.js  # Redis concurrency 임계치
  │   └─ s8-redisson-vs-lettuce.js  # Redisson vs Lettuce 비교
  ├─ run-matrix.ps1               # 전체 매트릭스 자동화
  └─ run-lockbleed-pool-tuning.ps1  # HikariCP 풀 튜닝 테스트
```

### 매트릭스 실행

```powershell
# 전체 (5 lock × 2 thread = 10 조합)
.\src\main\resources\k6\run-matrix.ps1 -Repeats 3

# MySQL 단독 (Redis 제외)
.\src\main\resources\k6\run-matrix.ps1 -LockStrategies @("NO_LOCK","OPTIMISTIC_LOCK","PESSIMISTIC_LOCK") -Repeats 5
```

## 실험 결과 요약

### v3 핵심 결과 (2026-04-08)

> 상세 인터랙티브 리포트: [`docs/v3-experiment-report-2026-04-08.html`](docs/v3-experiment-report-2026-04-08.html)

#### MySQL 단독 매트릭스 (c=200)

| Thread | Lock | 성공률 | p95 | Throughput |
|---|---|---|---|---|
| PLATFORM | NO_LOCK | 100% | 628ms | 311 rps |
| PLATFORM | PESSIMISTIC | 100% | 996ms | 221 rps |
| PLATFORM | OPTIMISTIC | 45% | 2147ms | 69 rps |
| VIRTUAL | NO_LOCK | 100% | 39ms | 274 rps |
| VIRTUAL | PESSIMISTIC | 100% | 768ms | 75 rps |
| VIRTUAL | OPTIMISTIC | 56% | 3148ms | 48 rps |

#### HikariCP Pool Tuning — Lock Bleed

| Pool Size | Read p95 | 개선 |
|---|---|---|
| 10 | 25.2s | baseline |
| 20 | 23.8s | -6% |
| 50 | 10.1s | **-60%** |

Pool 확대로 완화 가능하나 완전 해소 불가. 구조적 해결 필요 (커넥션 풀 분리/CQRS).

#### Redisson Pub-Sub vs Lettuce Spinlock

| Concurrency | Lettuce 성공률 | Redisson 성공률 | Redisson p95 |
|---|---|---|---|
| c=10 | 93% | **100%** | 222ms |
| c=20 | 85% | **100%** | 490ms |
| c=50 | 64% | **100%** | 1095ms |

Redisson이 전 구간 100% 성공. Lettuce의 concurrency ceiling(c=20)을 c=50까지 확장.

### 권장 조합

| 유스케이스 | 추천 | 근거 |
|---|---|---|
| 단일 DB, 안정성 우선 | PLATFORM + PESSIMISTIC | 100%, 221 rps |
| 분산 환경, 중간 부하 | Redisson + PLATFORM c≤20 | 100%, p95<500ms |
| 최저 지연시간 | VIRTUAL + NO_LOCK | p95 39ms (무결성 미보장) |

## 버전 히스토리

| 버전 | 날짜 | 주요 내용 |
|---|---|---|
| **v3** | 2026-04-08 | Redisson pub-sub 락, MySQL 단독 매트릭스, HikariCP 풀 튜닝, 인터랙티브 리포트 |
| **v2** | 2026-03-11 | Redis 튜닝(1~4), Lock Bleed 검증, JFR 분석, concurrency 임계치 |
| **v1** | 2026-02-16 | 4 lock × 2 thread 인메모리 베이스라인, k6 자동화 |

## 문서

| 문서 | 설명 |
|---|---|
| [`docs/v3-experiment-report-2026-04-08.html`](docs/v3-experiment-report-2026-04-08.html) | v3 인터랙티브 대시보드 리포트 |
| [`docs/roadmap-v3.md`](docs/roadmap-v3.md) | v3 로드맵 |
| [`docs/v2-closeout.md`](docs/v2-closeout.md) | v2 종료 리포트 |
| [`docs/v2-lock-bleed-summary-2026-02-21.md`](docs/v2-lock-bleed-summary-2026-02-21.md) | Lock Bleed 분석 |
| [`docs/v2-jfr-pessimistic-summary-2026-03-11.md`](docs/v2-jfr-pessimistic-summary-2026-03-11.md) | JFR + InnoDB 분석 |
| [`docs/v3-redis-concurrency-threshold-2026-03-16.md`](docs/v3-redis-concurrency-threshold-2026-03-16.md) | Redis concurrency 임계치 |
| [`docs/v1-release-notes.md`](docs/v1-release-notes.md) | v1 릴리즈 노트 |

## 인프라

```yaml
# docker-compose.yml
MySQL 8.0  → localhost:13306
Redis 7    → localhost:6379
```

## 기술 스택

- Java 21 (Virtual Threads)
- Spring Boot 3.3.5
- Spring Data JPA + Redis
- Redisson 3.27.2
- HikariCP
- Micrometer + Prometheus
- k6 (부하 테스트)
- Docker Compose
