import { sleep } from "k6";
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
  postExperiment({
    threadModel: __ENV.THREAD_MODEL || "PLATFORM",
    lockStrategy: __ENV.LOCK_STRATEGY || "NO_LOCK",
    totalRequests: Number.parseInt(__ENV.TOTAL_REQUESTS || "1000", 10),
    initialStock: Number.parseInt(__ENV.INITIAL_STOCK || "10000", 10),
    quantity: Number.parseInt(__ENV.QUANTITY || "1", 10),
    optimisticRetries: Number.parseInt(__ENV.OPTIMISTIC_RETRIES || "3", 10),
  });
  sleep(1);
}

