import { postExperiment } from "../lib/common.js";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ["rate==1.0"],
    http_req_failed: ["rate==0"],
  },
};

export default function () {
  const output = postExperiment({
    threadModel: __ENV.THREAD_MODEL || "PLATFORM",
    lockStrategy: __ENV.LOCK_STRATEGY || "NO_LOCK",
    totalRequests: Number.parseInt(__ENV.TOTAL_REQUESTS || "1000", 10),
    initialStock: Number.parseInt(__ENV.INITIAL_STOCK || "10000", 10),
    quantity: Number.parseInt(__ENV.QUANTITY || "1", 10),
    optimisticRetries: Number.parseInt(__ENV.OPTIMISTIC_RETRIES || "3", 10),
  });
  console.log(`LOCKBENCH_REQUEST ${JSON.stringify(output.payload)}`);
  console.log(`LOCKBENCH_RESULT ${JSON.stringify(output.parsed)}`);
}

export function handleSummary(data) {
  const summary = {
    generatedAt: new Date().toISOString(),
    k6: {
      checksPassRate: data.metrics.checks.values.rate,
      httpReqFailedRate: data.metrics.http_req_failed.values.rate,
      httpReqDurationAvgMs: data.metrics.http_req_duration.values.avg,
      httpReqDurationP95Ms: data.metrics.http_req_duration.values["p(95)"],
      iterationDurationAvgMs: data.metrics.iteration_duration.values.avg,
    },
  };

  const outPath = __ENV.RESULT_JSON_PATH || "result.json";
  return {
    [outPath]: JSON.stringify(summary, null, 2),
  };
}
