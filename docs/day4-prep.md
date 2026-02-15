# Day 4 Implementation Notes

Reference: `docs/roadmap-1week.md`  
Applied date: 2026-02-15

## Goals
1. Add Micrometer custom metrics.
2. Define Prometheus queries.
3. Prepare Grafana dashboard draft.
4. Define runId-based result lookup.

## Applied changes
- Added experiment and matrix metrics recording via `MeterRegistry`.
- Kept Prometheus scrape endpoint available at `/actuator/prometheus`.
- Added runId and matrixRunId lookup APIs.
- Added Grafana import JSON draft.

Related files:
- `src/main/java/io/lockbench/application/ExperimentMetricsRecorder.java`
- `src/main/java/io/lockbench/application/InMemoryRunResultStore.java`
- `src/main/java/io/lockbench/api/ExperimentController.java`
- `docs/grafana/day4-dashboard.json`

## Metric names in Prometheus
- `lockbench_experiment_run_count_total`
- `lockbench_experiment_request_success_count_total`
- `lockbench_experiment_request_failure_count_total`
- `lockbench_experiment_elapsed_seconds_count`
- `lockbench_experiment_elapsed_seconds_sum`
- `lockbench_experiment_throughput_req_per_sec_count`
- `lockbench_experiment_throughput_req_per_sec_sum`
- `lockbench_matrix_run_count_total`
- `lockbench_matrix_scenario_success_count_total`
- `lockbench_matrix_scenario_failure_count_total`

Note:
- Micrometer names convert `.` to `_`.
- Counter metrics are exposed with `_total`.

## Prometheus query draft

1. Experiment run rate per minute (by thread and lock)
```promql
sum by (thread_model, lock_strategy) (
  rate(lockbench_experiment_run_count_total[1m])
)
```

2. Success request TPS (by thread and lock)
```promql
sum by (thread_model, lock_strategy) (
  rate(lockbench_experiment_request_success_count_total[1m])
)
```

3. Failure request TPS (by thread and lock)
```promql
sum by (thread_model, lock_strategy) (
  rate(lockbench_experiment_request_failure_count_total[1m])
)
```

4. Failure ratio percentage (by thread and lock)
```promql
100 *
sum by (thread_model, lock_strategy) (rate(lockbench_experiment_request_failure_count_total[1m]))
/
clamp_min(
  sum by (thread_model, lock_strategy) (
    rate(lockbench_experiment_request_success_count_total[1m]) +
    rate(lockbench_experiment_request_failure_count_total[1m])
  ),
  1e-9
)
```

5. Average experiment elapsed time in ms (by thread and lock)
```promql
1000 *
(
  sum by (thread_model, lock_strategy) (rate(lockbench_experiment_elapsed_seconds_sum[5m]))
  /
  clamp_min(sum by (thread_model, lock_strategy) (rate(lockbench_experiment_elapsed_seconds_count[5m])), 1e-9)
)
```

6. Average throughput in req/s (by thread and lock)
```promql
sum by (thread_model, lock_strategy) (rate(lockbench_experiment_throughput_req_per_sec_sum[5m]))
/
clamp_min(sum by (thread_model, lock_strategy) (rate(lockbench_experiment_throughput_req_per_sec_count[5m])), 1e-9)
```

7. Matrix run rate
```promql
sum(rate(lockbench_matrix_run_count_total[5m]))
```

8. Matrix scenario failure ratio percentage
```promql
100 *
sum(rate(lockbench_matrix_scenario_failure_count_total[5m]))
/
clamp_min(
  sum(rate(lockbench_matrix_scenario_success_count_total[5m]) + rate(lockbench_matrix_scenario_failure_count_total[5m])),
  1e-9
)
```

## Grafana usage
1. Open Grafana and go to Dashboards > Import.
2. Upload `docs/grafana/day4-dashboard.json`.
3. Select Prometheus datasource.
4. Use `thread_model` and `lock_strategy` template variables for filtering.

## runId lookup API
- Experiment run: `GET /api/experiments/runs/{runId}`
- Matrix run: `GET /api/experiments/matrix-runs/{matrixRunId}`
- Store size limit: `lockbench.run-store.max-size` (default `500`)
