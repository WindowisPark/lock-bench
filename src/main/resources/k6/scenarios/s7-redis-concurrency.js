import { postExperiment } from "../lib/common.js";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ["rate==1.0"],
    http_req_failed: ["rate==0"],
  },
};

const experiments = [
  { threadModel: "PLATFORM", concurrency: 50 },
  { threadModel: "PLATFORM", concurrency: 100 },
  { threadModel: "VIRTUAL", concurrency: 50 },
  { threadModel: "VIRTUAL", concurrency: 100 },
];

export default function () {
  const totalRequests = Number.parseInt(__ENV.TOTAL_REQUESTS || "1000", 10);
  const initialStock = Number.parseInt(__ENV.INITIAL_STOCK || "10000", 10);

  const results = [];

  for (const exp of experiments) {
    console.log(`\n=== ${exp.threadModel} / REDIS_DISTRIBUTED_LOCK / concurrency=${exp.concurrency} ===`);

    const output = postExperiment({
      threadModel: exp.threadModel,
      lockStrategy: "REDIS_DISTRIBUTED_LOCK",
      totalRequests: totalRequests,
      initialStock: initialStock,
      quantity: 1,
      concurrency: exp.concurrency,
      optimisticRetries: 3,
    });

    const r = output.parsed;
    if (r) {
      const successRate = ((r.successCount / r.totalRequests) * 100).toFixed(2);
      const verdict = parseFloat(successRate) >= 99.0 ? "PASS" : "FAIL";
      console.log(
        `  successRate=${successRate}% (${verdict})  p95=${r.p95Millis}ms  throughput=${r.throughputPerSec.toFixed(1)}/s  elapsed=${r.elapsedMillis}ms`
      );
      results.push({
        threadModel: exp.threadModel,
        concurrency: exp.concurrency,
        successRate: successRate,
        verdict: verdict,
        p95Millis: r.p95Millis,
        throughputPerSec: r.throughputPerSec,
        elapsedMillis: r.elapsedMillis,
        failureBreakdown: r.failureBreakdown,
      });
    } else {
      console.log("  ERROR: no response parsed");
    }
  }

  console.log("\n=== SUMMARY ===");
  console.log(JSON.stringify(results, null, 2));
  console.log(`LOCKBENCH_RESULTS ${JSON.stringify(results)}`);
}
