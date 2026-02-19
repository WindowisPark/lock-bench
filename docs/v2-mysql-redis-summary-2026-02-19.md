# v2 MySQL+Redis 결과 요약 (2026-02-19)

## 실행 정보
- 프로파일: `mysql-redis`
- 결과 경로: `src/main/resources/k6/results/matrix-20260219-145009`

## 요약(평균)
| Thread | Strategy | Success Rate | Avg p95 (ms) | Avg Throughput (req/s) | Verdict |
| --- | --- | --- | --- | --- | --- |
| PLATFORM | NO_LOCK | 100% | 0.0 | 10117.398 | OK |
| PLATFORM | OPTIMISTIC_LOCK | 100% | 0.0 | 7598.498 | OK |
| PLATFORM | PESSIMISTIC_LOCK | 100% | 0.7 | 8717.675 | OK |
| PLATFORM | REDIS_DISTRIBUTED_LOCK | 3.53% | 3727.7 | 138.681 | FAIL (LOCK_TIMEOUT) |
| VIRTUAL | NO_LOCK | 100% | 0.0 | 131208.867 | OK |
| VIRTUAL | OPTIMISTIC_LOCK | 100% | 6.3 | 44007.760 | OK |
| VIRTUAL | PESSIMISTIC_LOCK | 100% | 0.0 | 168794.743 | OK |
| VIRTUAL | REDIS_DISTRIBUTED_LOCK | 0.30% | 116.0 | 8.217 | FAIL (LOCK_TIMEOUT) |

## 해석 요약
- MySQL 경로는 NO/OPT/PESS 모두 100% 성공률.
- Redis 분산락은 LOCK_TIMEOUT 비중이 높아 성공률이 매우 낮음.
