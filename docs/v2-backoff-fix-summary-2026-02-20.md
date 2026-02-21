# v2 Backoff 버그 수정 후 재실험 결과 (2026-02-20)

## 변경 내용
- `RedisDistributedLockStrategy.backoff()` full-jitter 버그 수정
- 수정 전: `sleep = baseDelay + [0, baseDelay]` → 실제 범위 `[baseDelay, 2×baseDelay]`
- 수정 후: `sleep = [0, cap]` (표준 full-jitter exponential backoff)

## 실행 정보
- 프로파일: `mysql-redis`
- 튜닝 파라미터: TTL 8000ms, max-retries 10, backoff 10~200ms (튜닝2와 동일)
- 반복: 10회
- 결과 경로: `src/main/resources/k6/results/matrix-20260220-011057`

## 결과 요약 (평균, 10회)
| Thread | Strategy | Success Rate | Avg p95 (ms) | Avg Throughput (req/s) | Verdict |
| --- | --- | --- | --- | --- | --- |
| PLATFORM | NO_LOCK | 100% | 0.0 | 43948.4 | OK |
| PLATFORM | OPTIMISTIC_LOCK | 100% | 0.0 | 34141.3 | OK |
| PLATFORM | PESSIMISTIC_LOCK | 100% | 0.0 | 47665.4 | OK |
| PLATFORM | REDIS_DISTRIBUTED_LOCK | 98.8% | 540.8 | 1187.6 | WARN (p95↑) |
| VIRTUAL | NO_LOCK | 100% | 0.0 | 999436.9 | OK |
| VIRTUAL | OPTIMISTIC_LOCK | 100% | 11.8 | 37569.3 | OK |
| VIRTUAL | PESSIMISTIC_LOCK | 100% | 0.0 | 920843.9 | OK |
| VIRTUAL | REDIS_DISTRIBUTED_LOCK | 84.8% | 819.3 | 721.7 | FAIL (LOCK_TIMEOUT) |

## 튜닝2(버그 포함) 대비 비교
| 항목 | 튜닝2 (버그) | 오늘 (수정) | 변화 |
| --- | --- | --- | --- |
| PLATFORM Redis p95 | 1358.7ms | 540.8ms | **-60%** |
| VIRTUAL Redis p95 | 2103.0ms | 819.3ms | **-61%** |
| PLATFORM Redis SuccRate | 100% | 98.8% | -1.2%p |
| VIRTUAL Redis SuccRate | 90.4% | 84.8% | -5.6%p |
| PLATFORM Redis Throughput | 467.6 rps | 1187.6 rps | **+154%** |
| VIRTUAL Redis Throughput | 344.0 rps | 721.7 rps | **+110%** |

## 해석
- p95 latency 약 60% 감소 확인 → backoff 버그가 latency 주원인이었음
- 성공률 소폭 하락: 이전 버그로 sleep이 2배 길어 경합을 우연히 회피했던 것.
  수정 후 retry 간격이 짧아져 경합 빈도 증가 → 성공률 하락
- VIRTUAL Redis 84.8%는 여전히 FAIL 기준(99%) 미달
- 처리량은 2배 이상 개선

## 다음 액션
- VIRTUAL Redis 성공률 개선 필요 (현재 84.8%, 목표 99% 이상)
- 방향: VU 수 감소 또는 backoff max 값 상향 조정 (경합 완화)
- p95 목표(200ms)는 아직 미달 → Redis 분산락 구조적 한계 가능성 검토
