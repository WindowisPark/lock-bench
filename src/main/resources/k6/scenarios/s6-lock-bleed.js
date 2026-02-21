/**
 * s6-lock-bleed.js
 *
 * 가설 검증: 락 전략이 처리 지연(processingDelayMillis)을 유발할 때,
 * 락과 무관한 읽기 API(GET /stock)가 얼마나 영향받는가?
 *
 * 예상:
 *   PESSIMISTIC_LOCK  → DB 커넥션 풀 고갈 → 읽기 p95 급등
 *   REDIS_LOCK        → 커넥션 반환 후 sleep → 읽기 영향 적음
 *   NO_LOCK           → 락 없음 → 읽기 최소 영향
 *
 * 실행 예:
 *   k6 run -e LOCK_STRATEGY=PESSIMISTIC_LOCK -e PROCESSING_DELAY_MILLIS=100 s6-lock-bleed.js
 */

import { postExperiment, getStock, baseUrl } from "../lib/common.js";
import { sleep } from "k6";

const PRODUCT_ID = Number.parseInt(__ENV.PRODUCT_ID || "1", 10);
const READ_RATE = Number.parseInt(__ENV.READ_RATE || "10", 10);
const READ_DURATION = __ENV.READ_DURATION || "90s";
const READ_START_DELAY = __ENV.READ_START_DELAY || "3s";

export const options = {
  scenarios: {
    // 쓰기 실험: 1회 실행 (서버 내부에서 동시 처리)
    write_contention: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      exec: "doWrite",
      startTime: "0s",
    },
    // 읽기 프로브: 쓰기 실험 중 지속적으로 읽기 요청을 보내 영향 측정
    read_probe: {
      executor: "constant-arrival-rate",
      rate: READ_RATE,
      timeUnit: "1s",
      duration: READ_DURATION,
      preAllocatedVUs: 5,
      maxVUs: 20,
      exec: "doRead",
      startTime: READ_START_DELAY,
    },
  },
  thresholds: {
    "http_req_duration{type:read}": ["p(95)<500"],
    "http_req_failed{type:read}": ["rate<0.05"],
  },
};

export function doWrite() {
  const result = postExperiment({
    tags: { type: "write" },
  });
  if (result.parsed) {
    console.log(
      `BLEED_WRITE strategy=${result.parsed.lockStrategy}` +
      ` success=${result.parsed.successCount}` +
      ` fail=${result.parsed.failureCount}` +
      ` p95=${result.parsed.p95Millis}ms` +
      ` tput=${result.parsed.throughputPerSec.toFixed(1)}rps`
    );
  }
}

export function doRead() {
  getStock(PRODUCT_ID);
}
