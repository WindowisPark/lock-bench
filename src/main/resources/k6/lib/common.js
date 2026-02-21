import http from "k6/http";
import { check } from "k6";

function intEnv(name, defaultValue) {
  const raw = __ENV[name];
  if (raw === undefined || raw === "") {
    return defaultValue;
  }
  return Number.parseInt(raw, 10);
}

export function baseUrl() {
  return (__ENV.BASE_URL || "http://localhost:8080").replace(/\/$/, "");
}

export function defaultExperimentPayload() {
  return {
    threadModel: __ENV.THREAD_MODEL || "PLATFORM",
    lockStrategy: __ENV.LOCK_STRATEGY || "NO_LOCK",
    productId: intEnv("PRODUCT_ID", 1),
    initialStock: intEnv("INITIAL_STOCK", 10000),
    quantity: intEnv("QUANTITY", 1),
    totalRequests: intEnv("TOTAL_REQUESTS", 1000),
    concurrency: intEnv("CONCURRENCY", 200),
    optimisticRetries: intEnv("OPTIMISTIC_RETRIES", 3),
    processingDelayMillis: intEnv("PROCESSING_DELAY_MILLIS", 0),
  };
}

export function getStock(productId) {
  const res = http.get(
    `${baseUrl()}/api/experiments/stock/${productId}`,
    { tags: { type: "read" } }
  );
  check(res, { "read status 200": (r) => r.status === 200 });
  return res;
}

export function postExperiment(overrides = {}) {
  const payload = { ...defaultExperimentPayload(), ...overrides };
  const res = http.post(
    `${baseUrl()}/api/experiments/run`,
    JSON.stringify(payload),
    { headers: { "Content-Type": "application/json" } }
  );

  let parsed = null;
  try {
    parsed = res.json();
  } catch (e) {
    parsed = null;
  }

  check(res, {
    "status is 200": (r) => r.status === 200,
    "runId exists": () => parsed !== null && typeof parsed.runId === "string",
  });

  return { response: res, payload, parsed };
}

export function postMatrixRun(overrides = {}) {
  const payload = {
    productId: intEnv("PRODUCT_ID", 1),
    initialStock: intEnv("INITIAL_STOCK", 10000),
    quantity: intEnv("QUANTITY", 1),
    totalRequests: intEnv("TOTAL_REQUESTS", 1000),
    optimisticRetries: intEnv("OPTIMISTIC_RETRIES", 3),
    ...overrides,
  };

  const res = http.post(
    `${baseUrl()}/api/experiments/matrix-run`,
    JSON.stringify(payload),
    { headers: { "Content-Type": "application/json" } }
  );

  let parsed = null;
  try {
    parsed = res.json();
  } catch (e) {
    parsed = null;
  }

  check(res, {
    "status is 200": (r) => r.status === 200,
    "matrixRunId exists": () => parsed !== null && typeof parsed.matrixRunId === "string",
  });

  return { response: res, payload, parsed };
}

