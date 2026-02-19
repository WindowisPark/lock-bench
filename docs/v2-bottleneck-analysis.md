# v2 Bottleneck Analysis

Date: 2026-02-19

## Scope
- MySQL + Redis combined profile (`mysql-redis`)
- Focus: success rate, p95/p99 latency, throughput

## Data sources
- Baseline summary: `docs/v2-mysql-redis-summary-2026-02-19.md`
- Tuning 1 summary: `docs/v2-mysql-redis-tuning-summary-2026-02-19.md`
- Tuning 2 summary: `docs/v2-mysql-redis-tuning2-summary-2026-02-19.md`
- Raw runs: `src/main/resources/k6/results/matrix-20260219-145009`
- Raw runs (tuning 1): `src/main/resources/k6/results/matrix-20260219-154559`
- Raw runs (tuning 2): `src/main/resources/k6/results/matrix-20260219-155525`

## Key findings
1) Redis distributed lock is the primary bottleneck.
   - Baseline success rate was near 0% to low single digits.
   - Tuning 1 improved success to ~55-64%, but still below 99%.
   - Tuning 2 reached 100% success on PLATFORM, ~90% on VIRTUAL, but p95 stayed in the 1-2s range.
2) MySQL paths are stable.
   - NO_LOCK / OPTIMISTIC / PESSIMISTIC maintain 100% success rate.
   - p95 latency is consistently low compared to Redis path.
3) Lock acquisition is the dominant failure mode.
   - Redis failures are overwhelmingly LOCK_TIMEOUT.

## Hypotheses
- Redis lock TTL and retry policy were too short in baseline.
- High contention causes frequent lock acquisition failure and long tail latency.
- VIRTUAL thread model increases concurrency pressure, reducing lock success.

## Recommendations (next actions)
1) Continue Redis tuning with objective target: >= 99% success rate and p95 < 500ms.
2) Add lock acquisition metrics (attempts, retries, acquire latency).
3) Consider queueing / jittered backoff increases for VIRTUAL model.

