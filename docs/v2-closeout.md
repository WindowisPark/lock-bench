# 프로젝트 v2 종료 보고서

작성 일자: 2026-03-11
버전: v2.0.0

---

## 1. 종료 판정

| 항목 | 상태 |
|---|---|
| Sprint 1: 측정 정밀도 | ✅ 완료 |
| Sprint 2: Redis 튜닝 | ⚠️ 튜닝3 실험 결과 필요 |
| Sprint 3: 관측 가능성 | ⚠️ JFR 실험 결과 필요 (Lock Bleed ✅) |
| Sprint 4: CI/자동화 | ❌ v3 이관 |

> **판정**: 튜닝3 및 JFR 실험 완료 후 최종 판정 갱신 필요

---

## 2. Sprint별 완료도

### Sprint 1: 측정 정밀도 ✅

- `elapsedNanos` 기반 고정밀 처리량 계산 구현
- `ThroughputConfidence` — `LOW_CONFIDENCE` 런 태깅 규칙 도입
- 기준 문서: `docs/v2-experiment-design.md`

### Sprint 2: VIRTUAL + REDIS 성공률 개선 ⚠️

| 튜닝 단계 | 파라미터 | VIRTUAL Redis 성공률 | VIRTUAL Redis p95 | 판정 |
|---|---|---|---|---|
| 튜닝1 | TTL 5000, retries 5, backoff 50~500ms | ~79% | ~3500ms | FAIL |
| 튜닝2 (버그 포함) | TTL 8000, retries 10, backoff 10~200ms | 90.4% | 2103ms | FAIL |
| 버그수정 후 재실험 | 동일 파라미터, full-jitter 수정 | 84.8% | 819ms | FAIL |
| **튜닝3** | retries 15, backoff 10~**500**ms | *실험 결과 대기* | *실험 결과 대기* | *대기* |

**핵심 버그 수정**: `RedisDistributedLockStrategy.backoff()` full-jitter 버그
- 수정 전: `sleep = baseDelay + [0, baseDelay]` → 실제 범위 `[baseDelay, 2×baseDelay]`
- 수정 후: `sleep = [0, cap]` (표준 full-jitter exponential backoff)
- 참조: `docs/v2-backoff-fix-summary-2026-02-20.md`

### Sprint 3: 관측 가능성 ⚠️ (부분 완료)

#### Lock Bleed 실험 ✅

PESSIMISTIC_LOCK이 HikariCP 풀(기본 10) 고갈 시 무관한 읽기 API까지 차단됨을 검증.

| 전략 | 읽기 p95 | 읽기 실패율 | 결과 |
|---|---|---|---|
| NO_LOCK | 15.06ms | 0.00% | PASS |
| OPTIMISTIC_LOCK | 9.33ms | 0.00% | PASS |
| PESSIMISTIC_LOCK | 30,020ms | 20.00% | **FAIL** |
| REDIS_DISTRIBUTED_LOCK | 9.62ms | 0.00% | PASS |

참조: `docs/v2-lock-bleed-summary-2026-02-21.md`

#### JFR + MySQL Slow Query 분석 ⚠️

- 설정 완료: `build.gradle.kts` bootRun JFR jvmArgs 추가
- 시나리오: `s4-pessimistic-stability.js` (PESSIMISTIC_LOCK 고부하)
- **실험 실행 필요** → 완료 후 `docs/v2-jfr-pessimistic-summary-2026-03-11.md` 갱신

### Sprint 4: CI / 자동화 ❌ → v3 이관

범위에서 제외. v3 로드맵으로 이관.

---

## 3. 산출물 패키지

| 문서 | 경로 | 상태 |
|---|---|---|
| v2 실험 설계 | `docs/v2-experiment-design.md` | ✅ |
| v2 MySQL+Redis 비교 | `docs/v2-redis-comparison-2026-02-19.md` | ✅ |
| v2 MySQL+Redis 첫 실험 | `docs/v2-mysql-redis-summary-2026-02-19.md` | ✅ |
| v2 Redis 튜닝1 | `docs/v2-mysql-redis-tuning-summary-2026-02-19.md` | ✅ |
| v2 Redis 튜닝2 | `docs/v2-mysql-redis-tuning2-summary-2026-02-19.md` | ✅ |
| v2 Backoff 버그수정 재실험 | `docs/v2-backoff-fix-summary-2026-02-20.md` | ✅ |
| v2 Lock Bleed 실험 | `docs/v2-lock-bleed-summary-2026-02-21.md` | ✅ |
| v2 Redis 관측 가능성 절차 | `docs/v2-mysql-observability.md` | ✅ |
| v2 병목 분석 | `docs/v2-bottleneck-analysis.md` | ✅ |
| **v2 Redis 튜닝3** | `docs/v2-redis-tuning3-summary-2026-03-11.md` | ⚠️ 실험 필요 |
| **v2 JFR 분석** | `docs/v2-jfr-pessimistic-summary-2026-03-11.md` | ⚠️ 실험 필요 |

---

## 4. 최종 권장 설정

### 전략별 권장 구성

| Lock 전략 | Thread 모델 | 권장 상황 | 주의사항 |
|---|---|---|---|
| PESSIMISTIC_LOCK | PLATFORM | 정합성이 최우선이고 동시성이 낮을 때 | 트랜잭션 범위 최소화 필수 (Lock Bleed 위험) |
| OPTIMISTIC_LOCK | VIRTUAL | 읽기 비율이 높고 충돌 빈도가 낮을 때 | 충돌 시 재시도 로직 구현 필요 |
| REDIS_DISTRIBUTED_LOCK | PLATFORM | 분산 환경에서 낙관적 락 대안 | concurrency ≤ 100 권장 (200에서 성공률 미달) |
| NO_LOCK | VIRTUAL | 성능 측정 기준선 전용 | 프로덕션 금지 |

### Redis 분산락 권장 파라미터 (튜닝3 결과 반영 예정)

```yaml
lockbench:
  redis-lock:
    ttl-millis: 8000
    max-retries: 15
    base-backoff-millis: 10
    max-backoff-millis: 500
```

> ⚠️ 튜닝3 실험 결과에 따라 권장값 갱신 필요

---

## 5. 미해결 항목 (v3 이관)

1. **CI/자동화** (Sprint 4 전체)
   - k6 실험 CI 파이프라인 통합
   - 결과 자동 분석 및 알림

2. **Redis 성공률 구조적 한계 분석**
   - VIRTUAL concurrency=200에서 99% 목표 달성 어려울 경우, 권장 concurrency 수준 정립
   - 분산환경(별도 서버) 실험

3. **JFR 정기 프로파일링 체계**
   - CI 연동, 릴리즈별 JFR 스냅샷 보관

4. **MySQL 커넥션 풀 조정 실험**
   - HikariCP pool 크기를 동시 트랜잭션 수에 맞게 조정하는 가이드라인

---

## 6. 승인 체크

- [x] Sprint 1 완료 (측정 정밀도)
- [x] Sprint 3 부분 완료 (Lock Bleed)
- [ ] Sprint 2 튜닝3 실험 완료
- [ ] Sprint 3 JFR 실험 완료
- [ ] 산출물 전체 링크 확인
- [ ] v3 로드맵 작성

---

## 7. 결론

v2에서 달성한 핵심 성과:
1. **측정 정밀도 향상** — nanos 기반 처리량 + LOW_CONFIDENCE 태깅으로 v1 대비 신뢰도 개선
2. **Backoff 버그 발견·수정** — full-jitter 구현 오류 제거, Redis 처리량 2배 향상
3. **Lock Bleed 검증** — PESSIMISTIC_LOCK의 커넥션 풀 고갈 → 읽기 차단 정량화 (p95 30초, fail 20%)
4. **Redis 한계 명확화** — VIRTUAL concurrency=200에서 분산락 성공률 목표(99%) 미달 → 구조적 한계 식별

Redis 성공률 개선(튜닝3) 및 JFR 분석은 실험 완료 후 본 문서를 갱신한다.
