/**
 * s9-optimistic-retry-tuning.js
 *
 * OPTIMISTIC_LOCK retry 횟수별 성공률/지연 비교.
 * retry 5/10/20/30/50 에서 PLATFORM/VIRTUAL 각각 측정.
 *
 * 실행:
 *   k6 run s9-optimistic-retry-tuning.js
 *   k6 run -e TOTAL_REQUESTS=1000 -e CONCURRENCY=200 s9-optimistic-retry-tuning.js
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

const retryLevels = [5, 10, 20, 30, 50];
const threadModels = ["PLATFORM", "VIRTUAL"];

export default function () {
  const totalRequests = Number.parseInt(__ENV.TOTAL_REQUESTS || "1000", 10);
  const initialStock = Number.parseInt(__ENV.INITIAL_STOCK || "10000", 10);
  const concurrency = Number.parseInt(__ENV.CONCURRENCY || "200", 10);

  const results = [];

  for (const threadModel of threadModels) {
    for (const retries of retryLevels) {
      console.log(`\n=== ${threadModel} / OPTIMISTIC_LOCK / retries=${retries} / c=${concurrency} ===`);

      const output = postExperiment({
        threadModel: threadModel,
        lockStrategy: "OPTIMISTIC_LOCK",
        totalRequests: totalRequests,
        initialStock: initialStock,
        quantity: 1,
        concurrency: concurrency,
        optimisticRetries: retries,
      });

      const r = output.parsed;
      if (r) {
        const successRate = ((r.successCount / r.totalRequests) * 100).toFixed(2);
        const verdict = parseFloat(successRate) >= 99.0 && r.p95Millis <= 500 ? "PASS" : "FAIL";
        console.log(
          `  successRate=${successRate}% (${verdict})  p95=${r.p95Millis}ms  throughput=${r.throughputPerSec.toFixed(1)}/s  elapsed=${r.elapsedMillis}ms`
        );
        results.push({
          threadModel, retries, concurrency,
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

  console.log("\n=== OPTIMISTIC RETRY TUNING SUMMARY ===");
  console.log(JSON.stringify(results, null, 2));
  console.log(`LOCKBENCH_RESULTS ${JSON.stringify(results)}`);
}
