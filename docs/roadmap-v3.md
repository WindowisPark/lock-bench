# v3 로드맵

작성 일자: 2026-03-16
기준: `docs/v2-closeout.md` 섹션 5 미해결 항목

---

## 목표

v2에서 확인된 구조적 한계와 미완료 항목을 v3에서 해소한다.

---

## 항목

### 1. CI/자동화 (Sprint 4 이관분)

- k6 실험 스크립트 CI 파이프라인 통합
- 실험 결과 자동 파싱 및 Slack/GitHub 알림
- 회귀 감지 임계치 설정 (처리량·성공률·p95 기준)

### 2. Redis concurrency 임계치 실험

- VIRTUAL concurrency=50, 100에서 Redis 분산락 성공률 측정
- 목표: 99% 이상 달성 가능한 concurrency 상한 정립
- 참고: v2에서 concurrency=200은 구조적 한계 확인 (최대 84.8%)

### 3. Redisson/Pub-Sub 기반 분산락 벤치마크

- 현재 Lettuce + polling 방식 대비 Redisson Pub-Sub 락의 성능·성공률 비교
- 분산 환경(별도 Redis 서버) 실험 포함

### 4. HikariCP 풀 튜닝 실험

- PESSIMISTIC_LOCK Lock Bleed 완화를 위한 커넥션 풀 크기 조정 가이드라인
- pool size = 동시 트랜잭션 수 매칭 시 읽기 차단 해소 여부 검증

### 5. JFR 정기 프로파일링 체계

- CI 연동: 릴리즈별 JFR 스냅샷 자동 수집·보관
- 주요 이벤트 기준선 설정 (CPU, GC, Monitor Wait)
- 성능 회귀 시 JFR diff 리포트 자동 생성

---

## 우선순위

| 순위 | 항목 | 근거 |
|---|---|---|
| 1 | CI/자동화 | 반복 실험의 효율성 확보 — 이후 항목 모두 의존 |
| 2 | Redis concurrency 임계치 | v2 핵심 미해결 — 권장 설정 완성에 필수 |
| 3 | HikariCP 풀 튜닝 | Lock Bleed 실질적 완화 방안 |
| 4 | Redisson/Pub-Sub 벤치마크 | 구조 전환 비용 크므로 후순위 |
| 5 | JFR 정기 프로파일링 | CI 완성 후 연동 |
