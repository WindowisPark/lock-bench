# v2 MySQL+Redis 튜닝2 결과 요약 (2026-02-19)

## 실행 정보
- 프로파일: `mysql-redis`
- 튜닝: TTL 8000ms, max-retries 10, backoff 10~200ms
- 결과 경로: `src/main/resources/k6/results/matrix-20260219-155525`

## 요약(평균)
| Thread | Strategy | Success Rate | Avg p95 (ms) | Avg Throughput (req/s) | Verdict |
| --- | --- | --- | --- | --- | --- |
| PLATFORM | NO_LOCK | 100% | 0.0 | 15848.172 | OK |
| PLATFORM | OPTIMISTIC_LOCK | 100% | 0.0 | 15011.099 | OK |
| PLATFORM | PESSIMISTIC_LOCK | 100% | 0.0 | 20863.878 | OK |
| PLATFORM | REDIS_DISTRIBUTED_LOCK | 100% | 1358.7 | 467.553 | WARN (지연↑) |
| VIRTUAL | NO_LOCK | 100% | 0.0 | 275330.014 | OK |
| VIRTUAL | OPTIMISTIC_LOCK | 100% | 9.3 | 58711.083 | OK |
| VIRTUAL | PESSIMISTIC_LOCK | 100% | 0.0 | 537873.162 | OK |
| VIRTUAL | REDIS_DISTRIBUTED_LOCK | 90.43% | 2103.0 | 344.044 | FAIL (LOCK_TIMEOUT) |

## 해석 요약
- Redis 성공률: PLATFORM 100%로 개선, VIRTUAL은 90%대.
- Redis p95 지연이 매우 큼(1~2초대).
- MySQL 경로는 안정적으로 100% 성공률 유지.
