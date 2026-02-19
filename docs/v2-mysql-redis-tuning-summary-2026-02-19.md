# v2 MySQL+Redis 튜닝 결과 요약 (2026-02-19)

## 실행 정보
- 프로파일: `mysql-redis`
- 튜닝: TTL 3000ms, max-retries 5, backoff 5~50ms
- 결과 경로: `src/main/resources/k6/results/matrix-20260219-154559`

## 요약(평균)
| Thread | Strategy | Success Rate | Avg p95 (ms) | Avg Throughput (req/s) | Verdict |
| --- | --- | --- | --- | --- | --- |
| PLATFORM | NO_LOCK | 100% | 0.0 | 27598.901 | OK |
| PLATFORM | OPTIMISTIC_LOCK | 100% | 0.0 | 26442.151 | OK |
| PLATFORM | PESSIMISTIC_LOCK | 100% | 0.0 | 34227.851 | OK |
| PLATFORM | REDIS_DISTRIBUTED_LOCK | 63.50% | 534.0 | 827.686 | FAIL (LOCK_TIMEOUT) |
| VIRTUAL | NO_LOCK | 100% | 0.0 | 658100.313 | OK |
| VIRTUAL | OPTIMISTIC_LOCK | 100% | 8.0 | 50776.439 | OK |
| VIRTUAL | PESSIMISTIC_LOCK | 100% | 0.0 | 675476.257 | OK |
| VIRTUAL | REDIS_DISTRIBUTED_LOCK | 55.73% | 267.0 | 771.219 | FAIL (LOCK_TIMEOUT) |

## 해석 요약
- Redis 성공률은 개선됐지만(이전 0~3% 수준 → 55~64%), 여전히 99% 목표 미달.
- MySQL 경로는 안정적으로 100% 성공률 유지.
