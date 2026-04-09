/**
 * s10-routing-ds-lockbleed.js
 *
 * Routing DataSource(읽기/쓰기 풀 분리) 적용 전후 Lock Bleed 비교.
 * 동일 시나리오를 routing 활성/비활성 상태에서 각각 실행하여 비교.
 *
 * 실행:
 *   # 1) 단일 풀 (기존): ROUTING_DS_ENABLED=false 로 앱 시작
 *   k6 run -e LABEL=single-pool s10-routing-ds-lockbleed.js
 *
 *   # 2) 분리 풀: ROUTING_DS_ENABLED=true ROUTING_DS_WRITE_POOL=10 ROUTING_DS_READ_POOL=10 로 앱 시작
 *   k6 run -e LABEL=routing-pool s10-routing-ds-lockbleed.js
 */

import { postExperiment, getStock, baseUrl } from "../lib/common.js";
import { sleep } from "k6";

const PRODUCT_ID = Number.parseInt(__ENV.PRODUCT_ID || "1", 10);
const READ_RATE = Number.parseInt(__ENV.READ_RATE || "10", 10);
const READ_DURATION = __ENV.READ_DURATION || "30s";
const LABEL = __ENV.LABEL || "unknown";

export const options = {
  scenarios: {
    write_contention: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      exec: "doWrite",
      startTime: "0s",
    },
    read_probe: {
      executor: "constant-arrival-rate",
      rate: READ_RATE,
      timeUnit: "1s",
      duration: READ_DURATION,
      preAllocatedVUs: 5,
      maxVUs: 20,
      exec: "doRead",
      startTime: "2s",
    },
  },
  thresholds: {
    "http_req_duration{type:read}": ["p(95)<500"],
    "http_req_failed{type:read}": ["rate<0.05"],
  },
};

export function doWrite() {
  const result = postExperiment({
    lockStrategy: "PESSIMISTIC_LOCK",
    threadModel: "PLATFORM",
    totalRequests: 500,
    initialStock: 5000,
    concurrency: 200,
    processingDelayMillis: 50,
    tags: { type: "write" },
  });
  if (result.parsed) {
    console.log(
      `BLEED_WRITE [${LABEL}] strategy=${result.parsed.lockStrategy}` +
      ` success=${result.parsed.successCount}` +
      ` fail=${result.parsed.failureCount}` +
      ` p95=${result.parsed.p95Millis}ms`
    );
  }
}

export function doRead() {
  getStock(PRODUCT_ID);
}
