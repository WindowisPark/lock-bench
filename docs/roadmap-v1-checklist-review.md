# v1 로드맵 체크리스트 및 검토

기준 일자: 2026-02-16  
기준 문서: `docs/roadmap-1week.md`

## 체크리스트

### Day 1 프로젝트 기반 구성
- [x] Spring Boot + Java 21 구성
- [x] 패키지 경계 구성(`api`, `application`, `domain`, `concurrency`, `infra`)
- [x] 실험 실행 API 초안(`POST /api/experiments/run`)
- [x] Thread 전략 교체 구조(Platform/Virtual)
- [x] Lock 전략 교체 구조(No/Optimistic/Pessimistic/Redis)
- [x] In-memory 기반 실행 가능 상태
- [x] Actuator/Prometheus 노출 설정

근거:
- `src/main/java/io/lockbench/api/ExperimentController.java`
- `src/main/java/io/lockbench/concurrency/thread/ThreadExecutionStrategyFactory.java`
- `src/main/java/io/lockbench/concurrency/lock/StockLockStrategyFactory.java`
- `src/main/resources/application.yml`

### Day 2 주문 유스케이스 + 기본 Lock 고도화
- [x] 주문/재고 규칙 및 실패 분류 정리
- [x] Optimistic retry 정책 반영
- [x] Pessimistic 경계 명시
- [x] 불변식 검증 기준 정리

근거:
- `docs/day2-prep.md`
- `src/main/java/io/lockbench/domain/model/OrderFailureReason.java`
- `src/main/java/io/lockbench/concurrency/lock/OptimisticLockStrategy.java`
- `src/main/java/io/lockbench/concurrency/lock/PessimisticLockStrategy.java`

### Day 3 Thread/Lock 매트릭스 완성
- [x] Thread 모델별 실행 파이프라인 고정
- [x] Redis 전략 게이트 처리
- [x] 2 x 4 조합 매트릭스 실행 API 제공

근거:
- `src/main/java/io/lockbench/api/ExperimentController.java` (`/matrix-run`)
- `src/main/java/io/lockbench/application/MatrixExperimentOrchestrator.java`
- `src/main/resources/application.yml` (`lockbench.redis-lock.enabled`)

### Day 4 관측/메트릭 구성
- [x] Micrometer 커스텀 메트릭 반영
- [x] Prometheus 수집 경로 노출
- [x] runId/matrixRunId 조회 API
- [x] Grafana 초안 문서화

근거:
- `docs/day4-prep.md`
- `src/main/java/io/lockbench/application/ExperimentMetricsRecorder.java`
- `src/main/java/io/lockbench/application/InMemoryRunResultStore.java`

### Day 5 k6 시나리오 자동화
- [x] S1~S5 스크립트 구성
- [x] 8조합 x 반복 자동 실행
- [x] JSON/CSV 집계 산출물 생성

근거:
- `docs/day5-prep.md`
- `src/main/resources/k6/scenarios/`
- `src/main/resources/k6/run-matrix.ps1`

### Day 6 1차 배치 + 병목 분석
- [x] 고정 조건 1차 배치 실행
- [x] 이상치 정리
- [x] 병목 가설 도출
- [x] 최소 튜닝안 제시

근거:
- `docs/day6-batch1.md`
- `src/main/resources/k6/results/matrix-20260216-144103/`

### Day 7 리포트 + 다음 백로그
- [x] 조합별 비교 리포트 작성
- [x] v1 권장 운영 전략 확정
- [x] v1 이후 백로그 정리

근거:
- `docs/day7-v1-finalization.md`
- `docs/v1-release-notes.md`

## 검토 결론
1. 로드맵 기준 Day 1~7 핵심 산출물은 v1 범위에서 충족됨.
2. Redis 분산락 실환경 검증과 고정밀 elapsed 측정은 v1 제외 범위로 타당하게 이관됨.
3. v1는 문서/코드/실행 산출물 기준으로 종료 가능 상태임.

## 오픈 리스크(인수인계)
1. `elapsedMillis` 단위 한계로 초단기 실행 처리량 왜곡 가능성.
2. Redis 경로는 현재 설정(`enabled=false`)에서 실패가 정상 동작으로 기록됨.
3. DB(MySQL) 기반 병목 분석은 아직 실측 근거가 부족함.
