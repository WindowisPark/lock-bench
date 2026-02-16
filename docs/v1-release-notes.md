# LockBench V1 릴리즈 노트

릴리즈 일자: 2026-02-16
버전: v1.0.0

## 요약
LockBench v1은 1주 로드맵 범위 내에서 스레드 모델/락 전략 비교 벤치마크를 수행할 수 있도록 API 오케스트레이션, 메트릭 노출, k6 자동화를 포함해 마무리된 버전입니다.

## v1 포함 범위
1. 실험 단건 실행 API 및 매트릭스 실행 API.
2. 스레드 모델 전환: `PLATFORM`, `VIRTUAL`.
3. 락 전략 전환: `NO_LOCK`, `OPTIMISTIC_LOCK`, `PESSIMISTIC_LOCK`, `REDIS_DISTRIBUTED_LOCK`(기능 플래그 기반).
4. `runId` / `matrixRunId` 기준 실행 결과 저장 및 조회.
5. Micrometer + Prometheus 메트릭 연동.
6. k6 시나리오 자동화 및 집계 산출물(`aggregate.csv`, `aggregate.json`) 생성.

## 검증된 벤치마크 기준선
- 배치 산출물: `src/main/resources/k6/results/matrix-20260216-144103`
- 실행 명령: `./src/main/resources/k6/run-matrix.ps1 -Repeats 5 -TotalRequests 3000`

상태 요약:
- `NO_LOCK`, `OPTIMISTIC_LOCK`, `PESSIMISTIC_LOCK`: 검증 완료(`OK`)
- `REDIS_DISTRIBUTED_LOCK`: 현재 기준 설정에서 `API_ERROR`

## v1 권장 기본 운영 프로필
- 스레드 모델: `PLATFORM`
- 락 전략: `PESSIMISTIC_LOCK`

선정 이유:
- 반복 실행에서 안정적으로 성공.
- 정확성 우선 기준에서 보수적으로 운용 가능.
- 초단기 실행 구간의 측정 왜곡 영향이 상대적으로 낮음.

## v1 알려진 한계
1. Redis 분산락 경로는 명시적 활성화와 Redis 연결 검증이 필요함.
2. 매우 짧은 실행에서는 `elapsedMillis` 단위 한계로 처리량 해석이 왜곡될 수 있음.
3. MySQL 기반 심층 DB 병목 분석은 v1 이후 범위로 이관됨.

## 후속(v1 이후) 우선 과제
1. 실환경 Redis에서 분산락 벤치마크 경로 활성화 및 재검증.
2. 고정밀 elapsed 시간 지표 도입 및 신뢰도 태깅 추가.
3. 집계 산출물에 실행 메타데이터(commit/profile/JVM/host) 확장.
