# v2 Redis 튜닝3 결과 요약 (2026-03-11)

## 변경 내용 (튜닝2 버그수정 기준 → 튜닝3)

| 파라미터 | 튜닝2 (버그수정) | 튜닝3 | 변경 근거 |
|---|---|---|---|
| ttl-millis | 8000 | 8000 | 유지 |
| max-retries | 10 | **15** | 재시도 기회 증가 |
| base-backoff-millis | 10 | 10 | 유지 |
| max-backoff-millis | 200 | **500** | 고경합 구간 분산 향상 |
| virtual-concurrency | 200 | 200 | 유지 |

**Backoff 범위 변화:**
- attempt 0: 0~10ms
- attempt 1: 0~20ms
- attempt 2: 0~40ms
- attempt 3: 0~80ms
- attempt 4+: 0~500ms (cap 도달 — 기존 200ms → 500ms)

기존 200ms cap 대비 고지연 재시도 구간에서 경합 분산 효과 기대.

---

## 실행 정보

- 프로파일: `mysql-redis`
- 반복: 10회
- 결과 경로: `src/main/resources/k6/results/matrix-<날짜>/`

---

## 결과 요약 (평균, 10회)

> ⚠️ 실험 실행 후 아래 표를 채우세요.

| Thread | Strategy | Success Rate | Avg p95 (ms) | Avg Throughput (req/s) | Verdict |
|---|---|---|---|---|---|
| PLATFORM | NO_LOCK | - | - | - | - |
| PLATFORM | OPTIMISTIC_LOCK | - | - | - | - |
| PLATFORM | PESSIMISTIC_LOCK | - | - | - | - |
| PLATFORM | REDIS_DISTRIBUTED_LOCK | - | - | - | - |
| VIRTUAL | NO_LOCK | - | - | - | - |
| VIRTUAL | OPTIMISTIC_LOCK | - | - | - | - |
| VIRTUAL | PESSIMISTIC_LOCK | - | - | - | - |
| VIRTUAL | REDIS_DISTRIBUTED_LOCK | - | - | - | - |

---

## 튜닝2(버그수정) 대비 비교

> ⚠️ 실험 실행 후 아래 표를 채우세요.

| 항목 | 튜닝2 (버그수정) | 튜닝3 | 변화 |
|---|---|---|---|
| PLATFORM Redis SuccRate | 98.8% | - | - |
| VIRTUAL Redis SuccRate | 84.8% | - | - |
| PLATFORM Redis p95 | 540.8ms | - | - |
| VIRTUAL Redis p95 | 819.3ms | - | - |
| PLATFORM Redis Throughput | 1187.6 rps | - | - |
| VIRTUAL Redis Throughput | 721.7 rps | - | - |

---

## 판정

> ⚠️ 실험 실행 후 작성

**목표 기준:**
- VIRTUAL REDIS 성공률 ≥ 99% → PASS / FAIL
- VIRTUAL REDIS p95 ≤ 500ms → PASS / FAIL

**결론:**

---

## 다음 액션

만약 튜닝3 후에도 성공률 99% 미달이면:
- 별도 실험으로 virtual-concurrency=100 vs 200 비교 수행
- "구조적 한계" 판정 → v2-closeout에 권장 concurrency 수준 문서화
