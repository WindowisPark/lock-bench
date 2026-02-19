# v2 Redis 비교표 (2026-02-19)

## 실행 요약
- 실행: `run-matrix.ps1 -Repeats 3`
- 결과 경로: `src/main/resources/k6/results/matrix-20260219-121414`
- Redis 전략: 성공 케이스 확보(LOCK_TIMEOUT 다수 발생)

## 비교표 (OK 결과 평균)
| Thread | Lock Strategy | OK Runs | API Errors | Avg Throughput (req/s) | Avg Elapsed (ms) |
| --- | --- | --- | --- | --- | --- |
| PLATFORM | NO_LOCK | 3 | 0 | 31047.520 | 32.333 |
| PLATFORM | OPTIMISTIC_LOCK | 3 | 0 | 25776.607 | 39.667 |
| PLATFORM | PESSIMISTIC_LOCK | 3 | 0 | 35823.110 | 27.667 |
| PLATFORM | REDIS_DISTRIBUTED_LOCK | 3 | 0 | 327.934 | 368.333 |
| VIRTUAL | NO_LOCK | 3 | 0 | 686696.789 | 6.000 |
| VIRTUAL | OPTIMISTIC_LOCK | 3 | 0 | 36704.115 | 28.667 |
| VIRTUAL | PESSIMISTIC_LOCK | 3 | 0 | 730073.273 | 1.333 |
| VIRTUAL | REDIS_DISTRIBUTED_LOCK | 3 | 0 | 78.707 | 38.333 |

## 관측 사항
- Redis 분산락은 정상 응답이나 `LOCK_TIMEOUT` 비중이 매우 높음.
- `VIRTUAL + NO_LOCK` 반복 2에서 `elapsedMillis=0`, `throughputConfidence=LOW` 1건 존재.

## 다음 액션
- Redis 락 획득 성공률 개선(락 TTL/경합 조건 재조정) 필요.

## 참고: MySQL 프로파일 2x4 결과 (2026-02-19)
- 결과 경로: `src/main/resources/k6/results/matrix-20260219-142616`
- 요약: Redis 전략은 mysql 프로파일에서 비활성(`lockbench.redis-lock.enabled=false`)로 API_ERROR 발생.
