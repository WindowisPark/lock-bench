/**
 * s11-redisson-fair-vs-unfair.js
 *
 * Redisson Fair Lock vs Unfair(Pub-Sub) Lock 성능 비교.
 * 동일 concurrency(10, 20, 50)에서 공정/비공정 락의 throughput, latency, 성공률 비교.
 *
 * 실행:
 *   k6 run s11-redisson-fair-vs-unfair.js
 */

import { postExperiment } from "../lib/common.js";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ["rate==1.0"],
    http_req_failed: ["rate==0"],
  },
};

const concurrencyLevels = [10, 20, 50];
const lockStrategies = ["REDISSON_PUB_SUB_LOCK", "REDISSON_FAIR_LOCK"];
const threadModels = ["PLATFORM", "VIRTUAL"];

export default function () {
  const totalRequests = Number.parseInt(__ENV.TOTAL_REQUESTS || "1000", 10);
  const initialStock = Number.parseInt(__ENV.INITIAL_STOCK || "10000", 10);

  const results = [];

  for (const threadModel of threadModels) {
    for (const concurrency of concurrencyLevels) {
      for (const lockStrategy of lockStrategies) {
        console.log(`\n=== ${threadModel} / ${lockStrategy} / c=${concurrency} ===`);

        const output = postExperiment({
          threadModel: threadModel,
          lockStrategy: lockStrategy,
          totalRequests: totalRequests,
          initialStock: initialStock,
          quantity: 1,
          concurrency: concurrency,
          optimisticRetries: 3,
        });

        const r = output.parsed;
        if (r) {
          const successRate = ((r.successCount / r.totalRequests) * 100).toFixed(2);
          const verdict = parseFloat(successRate) >= 99.0 && r.p95Millis <= 500 ? "PASS" : "FAIL";
          console.log(
            `  successRate=${successRate}% (${verdict})  p95=${r.p95Millis}ms  throughput=${r.throughputPerSec.toFixed(1)}/s`
          );
          results.push({
            threadModel, lockStrategy, concurrency,
            successRate, verdict,
            p95Millis: r.p95Millis,
            throughputPerSec: r.throughputPerSec,
            elapsedMillis: r.elapsedMillis,
            failureBreakdown: r.failureBreakdown,
          });
        } else {
          console.log("  ERROR: no response parsed");
        }
      }
    }
  }

  console.log("\n=== FAIR vs UNFAIR SUMMARY ===");
  console.log(JSON.stringify(results, null, 2));
  console.log(`LOCKBENCH_RESULTS ${JSON.stringify(results)}`);
}
