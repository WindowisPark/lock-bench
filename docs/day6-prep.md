# Day 6 Preparation Notes

Reference: `docs/roadmap-1week.md`  
Planned date: 2026-02-17

## Day 6 target
1. Run first full benchmark batch.
2. Organize anomalies and outliers.
3. Analyze bottlenecks (JFR / lock wait / slow query).
4. Propose minimal tuning candidates.

## Current baseline from Day 5 (as of 2026-02-16)
- k6 automation is ready (`run-matrix.ps1`).
- Output schema is stable (`aggregate.csv`, `aggregate.json`, per-run JSON).
- Redis lock scenario fails by configuration unless Redis lock is enabled.

## Pre-run decisions for Day 6
1. Scope for first batch:
- Option A: exclude `REDIS_DISTRIBUTED_LOCK` until Redis env is ready.
- Option B: include Redis after enabling `lockbench.redis-lock.enabled=true` and validating connectivity.

2. Repetition and load:
- Suggested initial batch: `Repeats=5`, `TotalRequests=3000`.
- If run-to-run variance is high, increase repeats to `10`.

3. Fix environment conditions:
- Keep same machine and same app profile for all runs.
- Fix JVM options and background load.
- Record absolute timestamp and git commit hash for each batch.

## Suggested Day 6 run sequence
1. Run non-Redis combinations (or all if Redis is ready):
```powershell
.\src\main\resources\k6\run-matrix.ps1 -Repeats 5 -TotalRequests 3000
```

2. Save metadata:
- app profile, branch/commit, request params, run window start/end.

3. Basic anomaly scan:
- per combination p95/p99 spikes
- throughput drops
- non-zero failureCount or HTTP failure rate

4. Deep dive candidates:
- If CPU/latency outliers occur: capture JFR for same scenario.
- If DB is enabled in Day 6 scope: collect lock wait and slow query.

## Analysis output template for Day 6 end
1. Combination ranking by throughput and p95.
2. Outlier list (scenario, repeat, metric, hypothesis).
3. Bottleneck evidence (JFR/DB/lock metrics).
4. Top 1-3 minimal tuning actions for Day 7 validation.

## Day 6 completion checklist
- [ ] First full batch executed with fixed conditions
- [ ] Anomaly table written
- [ ] Bottleneck evidence attached
- [ ] Minimal tuning candidates proposed

