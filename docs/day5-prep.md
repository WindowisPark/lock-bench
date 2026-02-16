# Day 5 Implementation Notes

Reference: `docs/roadmap-1week.md`  
Applied date: 2026-02-16

## Goals
1. Write k6 scripts for S1~S5 scenarios.
2. Add automated matrix runner (8 combinations x repeats).
3. Export and organize JSON/CSV result artifacts.

## Applied changes
- Added reusable k6 HTTP helpers and payload builders.
- Added S1~S5 scenario scripts under `k6/scenarios`.
- Added matrix automation script for 2(thread) x 4(lock) combinations.
- Added per-run JSON, aggregate JSON, aggregate CSV output flow.
- Added parser flow to include API response payload in result files.
- Added `k6/results/` ignore rule for git hygiene.

Related files:
- `src/main/resources/k6/lib/common.js`
- `src/main/resources/k6/scenarios/s1-baseline.js`
- `src/main/resources/k6/scenarios/s2-high-contention.js`
- `src/main/resources/k6/scenarios/s3-optimistic-retry.js`
- `src/main/resources/k6/scenarios/s4-pessimistic-stability.js`
- `src/main/resources/k6/scenarios/s5-matrix-run.js`
- `src/main/resources/k6/scenarios/run-single-combo.js`
- `src/main/resources/k6/run-matrix.ps1`
- `src/main/resources/k6/README.md`
- `src/main/resources/k6/.gitignore`

## Execution command used
```powershell
.\src\main\resources\k6\run-matrix.ps1 -Repeats 3
```

## Result artifact path
- `src/main/resources/k6/results/matrix-20260216-130821/aggregate.csv`
- `src/main/resources/k6/results/matrix-20260216-130821/aggregate.json`
- `src/main/resources/k6/results/matrix-20260216-130821/*.json`

## Observed result summary
- `NO_LOCK`, `OPTIMISTIC_LOCK`, `PESSIMISTIC_LOCK`: successful response collection and metrics output.
- `REDIS_DISTRIBUTED_LOCK`: expected failure in current setup (`k6ExitCode=99`) because Redis lock is disabled (`lockbench.redis-lock.enabled=false` in `application.yml`).

## Day 5 completion checklist
- [x] S1~S5 k6 script set
- [x] 8-combination repeated automation script
- [x] JSON/CSV export and aggregation
- [x] Real execution verification

