# Day 5 - k6 Scenario Automation

Location: `src/main/resources/k6`

## Prerequisites

1. Start LockBench server (`http://localhost:8080` by default).
2. Install k6 and ensure `k6` command is available in PATH.

## Scenario scripts (S1-S5)

- `scenarios/s1-baseline.js`: baseline single run
- `scenarios/s2-high-contention.js`: high contention single run
- `scenarios/s3-optimistic-retry.js`: optimistic retry focused run
- `scenarios/s4-pessimistic-stability.js`: pessimistic stability run
- `scenarios/s5-matrix-run.js`: calls `/api/experiments/matrix-run`

Example:

```powershell
k6 run .\src\main\resources\k6\scenarios\s1-baseline.js
```

Override example:

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e THREAD_MODEL=VIRTUAL `
  -e LOCK_STRATEGY=OPTIMISTIC_LOCK `
  -e TOTAL_REQUESTS=3000 `
  -e OPTIMISTIC_RETRIES=5 `
  .\src\main\resources\k6\scenarios\s1-baseline.js
```

## 8-combination x repeat automation

Use:

```powershell
.\src\main\resources\k6\run-matrix.ps1 -Repeats 3
```

This runs:
- Thread models: `PLATFORM`, `VIRTUAL`
- Lock strategies: `NO_LOCK`, `OPTIMISTIC_LOCK`, `PESSIMISTIC_LOCK`, `REDIS_DISTRIBUTED_LOCK`
- Total combinations per repeat: 8

Optional params:

```powershell
.\src\main\resources\k6\run-matrix.ps1 `
  -BaseUrl http://localhost:8080 `
  -Repeats 5 `
  -TotalRequests 2000 `
  -InitialStock 10000 `
  -Quantity 1 `
  -OptimisticRetries 5 `
  -OutDir .\tmp\k6-results
```

## Result outputs

Automation output directory contains:
- `aggregate.csv`: summary rows for all runs
- `aggregate.json`: same aggregate rows in JSON
- `*.json`: per-combination detailed output (request, result, k6 metrics)

`aggregate.csv` core columns:
- `repeat`
- `threadModel`
- `lockStrategy`
- `k6ExitCode`
- `runId`
- `successCount`
- `failureCount`
- `failureLockTimeout`
- `failureOutOfStock`
- `failureVersionConflict`
- `failureInvalidQuantity`
- `failureProductNotFound`
- `failureUnknown`
- `elapsedMillis`
- `elapsedNanos`
- `throughputConfidence`
- `throughputPerSec`
- `p95Millis`
- `checksPassRate`
- `httpReqFailedRate`
- `httpReqDurationAvgMs`
