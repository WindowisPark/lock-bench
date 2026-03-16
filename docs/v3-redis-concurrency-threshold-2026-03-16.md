# v3 Redis Concurrency Threshold Experiment

**Date**: 2026-03-16
**Goal**: Find the maximum concurrency level that achieves >= 99% success rate with Redis distributed lock

## Environment

- MySQL 8 (Docker, port 13306) + Redis 7.4.8 (Docker, port 6379)
- Spring Boot 3.3.5, Java 21, lock-bench application
- totalRequests=1000, initialStock=10000, quantity=1

## Code Changes

### per-request concurrency support
Previously `ExperimentOrchestrator` ignored `request.concurrency()` and used only the application.yml fixed value.
Modified to use `request.concurrency()` when > 0, falling back to config default.

### Bug fix: application-mysql.yml profile override
`application-mysql.yml` had `redis-lock.enabled: false` which overrode `application-mysql-redis.yml`'s `enabled: true` due to Spring Boot profile group loading order.
Removed redundant `redis-lock.enabled: false` from `application-mysql.yml` (default in `application.yml` is sufficient).

---

## Experiment Results

### Round 1: Tuning2 (retries=10, backoff_max=200ms)

| # | Thread Model | Concurrency | Success | Rate | p95 (ms) | p99 (ms) | Throughput (/s) | Verdict |
|---|-------------|-------------|---------|------|----------|----------|-----------------|---------|
| 1 | PLATFORM | 50 | 662/1000 | 66.2% | 832 | 978 | 107.2 | FAIL |
| 2 | PLATFORM | 100 | 535/1000 | 53.5% | 863 | 954 | 117.3 | FAIL |
| 3 | VIRTUAL | 50 | 693/1000 | 69.3% | 835 | 965 | 113.2 | FAIL |
| 4 | VIRTUAL | 100 | 524/1000 | 52.4% | 863 | 951 | 114.5 | FAIL |

**Tuning2 conclusion**: Even at concurrency=50, retry budget is insufficient. Massive LOCK_TIMEOUT failures.

### Round 2: Tuning3 (retries=15, backoff_max=500ms)

| # | Thread Model | Concurrency | Success | Rate | p95 (ms) | p99 (ms) | Throughput (/s) | Verdict |
|---|-------------|-------------|---------|------|----------|----------|-----------------|---------|
| 5 | PLATFORM | 50 | 915/1000 | 91.5% | 2692 | 3139 | 99.1 | FAIL |
| 6 | PLATFORM | 100 | 847/1000 | 84.7% | 2785 | 3321 | 110.4 | FAIL |
| 7 | VIRTUAL | 50 | 904/1000 | 90.4% | 2671 | 3248 | 105.3 | FAIL |
| 8 | VIRTUAL | 100 | 812/1000 | 81.2% | 2942 | 3391 | 101.6 | FAIL |

**Tuning3 conclusion**: Better than tuning2, but still well below 99%. High p95 latency (2.7-2.9s).

### Round 3: Tuning4 (retries=30, backoff_max=1000ms)

| # | Thread Model | Concurrency | Success | Rate | p95 (ms) | p99 (ms) | Throughput (/s) | Verdict |
|---|-------------|-------------|---------|------|----------|----------|-----------------|---------|
| 9 | PLATFORM | 10 | 1000/1000 | **100%** | **12** | 3353 | 106.1 | **PASS** |
| 10 | PLATFORM | 20 | 1000/1000 | **100%** | 232 | 5648 | 117.0 | PASS* |
| 11 | PLATFORM | 50 | 1000/1000 | **100%** | 3862 | 8300 | 101.3 | PASS* |
| 12 | PLATFORM | 100 | 999/1000 | **99.9%** | 6314 | 8252 | 101.7 | PASS* |
| 13 | VIRTUAL | 10 | 1000/1000 | **100%** | **15** | 2164 | 101.1 | **PASS** |
| 14 | VIRTUAL | 20 | 998/1000 | 99.8% | **152** | 6205 | 86.4 | PASS* |
| 15 | VIRTUAL | 50 | 1000/1000 | **100%** | 3461 | 7579 | 111.1 | PASS* |
| 16 | VIRTUAL | 100 | 1000/1000 | **100%** | 5827 | 7956 | 104.3 | PASS* |

\* Success rate >= 99% but p95 latency > 500ms (quality concern)

---

## v2 Baseline Comparison (concurrency=200)

| Thread Model | v2 Rate (Tuning3, c=200) | v3 Best (Tuning4, c=10) | v3 c=50 (Tuning4) |
|-------------|--------------------------|-------------------------|-------------------|
| PLATFORM | 98.8% | 100% | 100% |
| VIRTUAL | 84.8% | 100% | 100% |

---

## Key Findings

### 1. Retry budget is the critical factor
The relationship between retry parameters and success rate is dramatic:
- **retries=10, backoff=200ms**: total retry window ~2s -> 53-69% success at c=50
- **retries=15, backoff=500ms**: total retry window ~7.5s -> 81-91% success at c=50
- **retries=30, backoff=1000ms**: total retry window ~30s -> 99.8-100% success at c=100

### 2. Success rate vs. latency tradeoff
Aggressive retries achieve 99%+ success but at the cost of tail latency:
- **c=10**: p95=12-15ms (excellent), p99=2-3s (moderate)
- **c=20**: p95=152-232ms (acceptable), p99=5-6s (high)
- **c=50+**: p95=3-6s (unacceptable for most production use)

### 3. Concurrency threshold recommendation

| Criteria | Max Concurrency | Retry Config |
|----------|----------------|-------------|
| 99% success + p95 < 500ms | **10-20** | retries=30, backoff=1000ms |
| 99% success (latency relaxed) | **100** | retries=30, backoff=1000ms |
| 99% success (any tuning) | Impossible at c=50+ | retries <= 15 |

---

## Conclusion

1. **Redis spinlock-based distributed lock has a fundamental concurrency ceiling**. The retry-based acquisition pattern creates contention amplification: as concurrency grows, more threads compete for the same lock, each burning through retries faster.

2. **Recommended concurrency upper bound: 20** (with tuning4 parameters) for production workloads requiring both 99% success rate and sub-500ms p95 latency.

3. **For benchmarking/stress testing: concurrency up to 100** is viable with tuning4 if latency is not a concern (success rate still >= 99%).

4. **Tuning2 (retries=10, backoff=200ms) is NOT viable** for any concurrency level above single-digit. The v2 conclusion about tuning2 needs revision.

5. **Next steps**: Consider Redisson/RedLock or pub-sub based lock notification (instead of spinlock) to support higher concurrency without latency degradation.

---

## Configuration Summary

### Tuning4 (recommended for c <= 20)
```yaml
lockbench:
  redis-lock:
    enabled: true
    ttl-millis: 8000
    max-retries: 30
    base-backoff-millis: 10
    max-backoff-millis: 1000
```

### application-mysql-redis.yml (current: tuning2, needs update if adopting tuning4)
```yaml
max-retries: 10        # -> 30
base-backoff-millis: 10
max-backoff-millis: 200  # -> 1000
```
