# Day 7 v1 결과 보고서

기준 문서: `docs/roadmap-1week.md`  
작성 일자: 2026-02-16

## 1. 보고 목적
1. 최신 유효 배치 결과를 기준으로 v1 성능 비교 결과를 확정한다.
2. v1 기본 운영 전략(권장 프로필)을 결정한다.
3. v1 포함/제외 범위를 확정하고 후속 과제를 정리한다.

## 2. 근거 산출물
- Day 6 배치 노트: `docs/day6-batch1.md`
- 집계 CSV: `src/main/resources/k6/results/matrix-20260216-144103/aggregate.csv`
- 집계 JSON: `src/main/resources/k6/results/matrix-20260216-144103/aggregate.json`

## 3. 조합별 비교 결과(성공 실행 기준)

| Thread | Lock | Runs | 평균 처리량(req/s) | 평균 elapsed(ms) | 평균 p95(ms) | 상태 |
|---|---|---:|---:|---:|---:|---|
| PLATFORM | NO_LOCK | 5 | 34,834.79 | 87.0 | 0 | OK |
| PLATFORM | OPTIMISTIC_LOCK | 5 | 29,528.52 | 106.2 | 0 | OK |
| PLATFORM | PESSIMISTIC_LOCK | 5 | 35,416.91 | 87.2 | 0 | OK |
| PLATFORM | REDIS_DISTRIBUTED_LOCK | 5 | - | - | - | API_ERROR (HTTP 500) |
| VIRTUAL | NO_LOCK | 5 | 332,380.95 | 24.4 | 0 | OK |
| VIRTUAL | OPTIMISTIC_LOCK | 5 | 96,720.86 | 32.0 | 5 | OK |
| VIRTUAL | PESSIMISTIC_LOCK | 5 | 232,778.95 | 6.2 | 0 | OK |
| VIRTUAL | REDIS_DISTRIBUTED_LOCK | 5 | - | - | - | API_ERROR (HTTP 500) |

## 4. 결과 해석
1. Redis를 제외한 조합은 반복 실행에서 정상 완료되었다.
2. Virtual 조합은 높은 처리량이 관측되었지만, 짧은 실행 시간 구간에서 런 간 편차가 크다.
3. `elapsedMillis`가 0 또는 매우 작은 값일 경우 처리량이 과대/왜곡될 수 있다.

## 5. v1 권장 운영안
1. 기본 스레드 모델: `PLATFORM`
2. 기본 락 전략: `PESSIMISTIC_LOCK`
3. 선정 근거:
- 반복 실행 안정성 확보
- 정확성 우선(보수적) 운영에 적합
- 초단기 측정 왜곡 영향 상대적으로 낮음

## 6. v1 범위 확정
v1 포함:
- 매트릭스 벤치마크 API 및 k6 자동화 실행 흐름
- Platform/Virtual + No/Optimistic/Pessimistic 비교 실행
- Prometheus 메트릭 및 runId 기반 조회 API

v1 제외(이관):
- 실환경 Redis 분산락 비교 벤치마크
- 고정밀 elapsed(`nanos`/`micros`) 기반 처리량 개선
- MySQL 기반 심층 DB 병목 분석

## 7. 후속(v1 이후) 백로그
1. Redis 활성화(`lockbench.redis-lock.enabled=true`) 및 연결 검증 후 재실행
2. 고정밀 elapsed 필드 추가 및 처리량 계산 정밀도 개선
3. 짧은 실행 구간 `LOW_CONFIDENCE` 태깅 규칙 도입
4. 집계 산출물에 실행 메타데이터(commit/profile/JVM/host) 확장
5. CI 스모크 벤치마크 1개 시나리오 추가

## 8. v1 종료 체크리스트
- [x] Day 1~6 산출물 문서화 및 재현 가능 상태 확인
- [x] 유효 기준 배치 고정(`matrix-20260216-144103`)
- [x] Day 7 비교 결과 보고서 작성
- [x] v1 권장 운영 프로필 확정
- [x] 후속 백로그 정리

## 9. 결론
본 문서는 1주 로드맵 기준 v1 종료 보고서이며, 다음 스프린트의 출발 기준선으로 사용한다.
