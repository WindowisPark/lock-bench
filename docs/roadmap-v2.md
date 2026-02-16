# LockBench v2 로드맵

작성 일자: 2026-02-16
계획 기간: 4주 (스프린트 기준)

## v2 목표
1. 성능 지표의 신뢰도를 높인다(고정밀 시간/신뢰도 태깅).
2. Redis 분산락과 MySQL 기반 실환경 비교 실험을 완성한다.
3. 실행 재현성과 자동화를 강화한다(CI 스모크 + 메타데이터 표준화).

## 범위 외(이번 v2에서 제외)
1. 대규모 멀티노드 분산 부하 실험
2. 완전한 프로덕션 운영 플랫폼화(알림/오토스케일링)

## Sprint 1: 측정 정밀도 개선
### 목표
- `elapsedMillis` 한계를 제거하고 처리량 계산의 신뢰도 기준을 도입한다.

### 작업
1. `ExperimentResponse`에 `elapsedNanos` 추가.
2. 처리량 계산을 `elapsedNanos` 기준으로 변경.
3. `throughputConfidence` 필드(`HIGH`, `LOW`) 추가.
4. k6 집계(`run-matrix.ps1`)에 신규 필드 반영.

### 완료 기준(DoD)
- [ ] `elapsedMillis=0`이어도 처리량 계산이 정상 동작
- [ ] 집계 CSV/JSON에서 신뢰도 라벨 확인 가능
- [ ] 기존 API 소비자 하위호환 유지

## Sprint 2: Redis 분산락 실환경 검증
### 목표
- Redis 경로를 실제 연결 환경에서 성공 시나리오까지 포함해 검증한다.

### 작업
1. `lockbench.redis-lock.enabled=true` 프로파일 구성.
2. Redis 연결 사전 점검 스크립트/헬스체크 추가.
3. 2x4 조합 재실행 후 Redis 결과 비교표 작성.
4. 실패 유형(`LOCK_TIMEOUT` 등) 관측 강화.

### 완료 기준(DoD)
- [ ] Redis 전략 `API_ERROR`가 아닌 정상 성공 케이스 확보
- [ ] Redis 포함 비교 리포트 1회 이상 작성
- [ ] Redis 비활성/활성 운영 가이드 문서화

## Sprint 3: MySQL 어댑터 및 병목 분석
### 목표
- In-memory 외에 DB 기반 실험 경로를 추가해 병목 근거를 확보한다.

### 작업
1. MySQL/JPA 기반 `StockAccessPort` 어댑터 구현.
2. 슬로우쿼리/락 대기 지표 수집 절차 문서화.
3. 핵심 시나리오(JFR + DB 지표) 병행 수집.
4. 전략별 병목 차이 분석 리포트 작성.

### 완료 기준(DoD)
- [ ] DB 프로파일에서 기본 시나리오 성공
- [ ] 최소 1개 시나리오에 대해 JFR/DB 근거 첨부
- [ ] 병목 원인-개선안 연결 가능한 분석표 확보

## Sprint 4: 재현성 및 자동화 강화
### 목표
- 실험 실행을 팀 단위로 반복 가능하게 표준화한다.

### 작업
1. 배치 산출물에 메타데이터(commit/profile/JVM/host) 추가.
2. CI 스모크 벤치마크(소규모 고정 시나리오) 추가.
3. 기준선 대비 변동폭 경고 규칙 정의.
4. v2 결과 종합 리포트 템플릿 고정.

### 완료 기준(DoD)
- [ ] CI에서 스모크 벤치 실행/아티팩트 저장
- [ ] 기준선 대비 급격한 성능 저하 감지 가능
- [ ] v2 종료 보고서 템플릿으로 결과 재현 가능

## 산출물 목록
1. `docs/v2-weekly-report-*.md`
2. `docs/v2-bottleneck-analysis.md`
3. `docs/v2-closeout.md`
4. `src/main/resources/k6/results/v2-*`

## 리스크 및 대응
1. 실험 환경 편차: 고정 실행 조건 체크리스트 의무화
2. Redis/DB 외부 의존성: 사전 연결 검증 단계 분리
3. 지표 해석 편향: 신뢰도 라벨 + 반복 횟수 최소 기준 도입

## 우선순위
1. Sprint 1(측정 정밀도) 
2. Sprint 2(Redis 실환경)
3. Sprint 3(MySQL 병목)
4. Sprint 4(CI 자동화)
