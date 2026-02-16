# Day 6 Batch #1 Result Notes

Reference: `docs/roadmap-1week.md`  
Executed date: 2026-02-16

## Run metadata
- Command: `.\src\main\resources\k6\run-matrix.ps1 -Repeats 5 -TotalRequests 3000`
- Valid result set: `src/main/resources/k6/results/matrix-20260216-144103/`
- Initial attempt (`matrix-20260216-143842`) was invalid because app was down (`connection refused`).

## Scenario execution status
- `NO_LOCK`: 10 runs, all `OK` (`k6ExitCode=0`)
- `OPTIMISTIC_LOCK`: 10 runs, all `OK` (`k6ExitCode=0`)
- `PESSIMISTIC_LOCK`: 10 runs, all `OK` (`k6ExitCode=0`)
- `REDIS_DISTRIBUTED_LOCK`: 10 runs, all `API_ERROR` (`k6ExitCode=99`, HTTP 500)

Reason for Redis failures:
- Current configuration keeps Redis lock feature disabled (`lockbench.redis-lock.enabled=false`).

## Aggregated performance snapshot (successful runs only)

| Thread | Lock | Runs | Avg throughput (req/s) | Min | Max | Avg elapsed (ms) | Avg p95 (ms) |
|---|---|---:|---:|---:|---:|---:|---:|
| PLATFORM | NO_LOCK | 5 | 34,834.79 | 30,612.24 | 40,000.00 | 87.0 | 0 |
| PLATFORM | OPTIMISTIC_LOCK | 5 | 29,528.52 | 20,833.33 | 37,974.68 | 106.2 | 0 |
| PLATFORM | PESSIMISTIC_LOCK | 5 | 35,416.91 | 25,641.03 | 41,666.67 | 87.2 | 0 |
| VIRTUAL | NO_LOCK | 5 | 332,380.95 | 33,333.33 | 600,000.00 | 24.4 | 0 |
| VIRTUAL | OPTIMISTIC_LOCK | 5 | 96,720.86 | 73,170.73 | 125,000.00 | 32.0 | 5 |
| VIRTUAL | PESSIMISTIC_LOCK | 5 | 232,778.95 | 3,000.00 | 500,000.00 | 6.2 | 0 |

## Anomalies and risks
1. `REDIS_DISTRIBUTED_LOCK` always failed as expected by current feature flag state.
2. Some runs report `elapsedMillis=0`, which triggers fallback throughput calculation and can distort interpretation.
3. `VIRTUAL + PESSIMISTIC_LOCK` shows very wide throughput range (`3,000` to `500,000`), indicating timing granularity or measurement artifacts rather than stable behavior.

## Bottleneck hypothesis (Day 6 scope)
1. Main bottleneck signal in this batch is not DB/lock wait but measurement granularity (`elapsedMillis` in ms at very short run durations).
2. For extremely fast paths, current latency/throughput accounting is too coarse to rank strategies reliably.
3. Redis lock path is blocked by config gate, so Redis-specific bottleneck analysis is deferred.

## Minimal tuning proposals for next run
1. Increase workload duration to reduce `elapsedMillis=0` cases.
2. Compute throughput from `elapsedNanos` (or at least microsecond precision) in application response.
3. Add run validation rule in aggregator and mark `elapsedMillis < 5` runs as `LOW_CONFIDENCE`.
4. If Redis comparison is needed, enable `lockbench.redis-lock.enabled=true` and verify Redis connectivity before batch start.

## Day 6 checklist status
- [x] First batch executed with fixed command and artifact set
- [x] Anomaly list documented
- [x] Bottleneck hypothesis documented (measurement-focused)
- [x] Minimal tuning options proposed

