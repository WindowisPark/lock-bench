# LockBench 1주 타이트 개발 로드맵

## Day 1: 프로젝트 기반 구축 (완료)

1. Spring Boot + Java 21 골격 구성
2. 패키지 경계 구성 (`api`, `application`, `domain`, `concurrency`, `infra`)
3. 실험 실행 API 초안 (`POST /api/experiments/run`)
4. Thread 전략 교체 구조 추가 (Platform/Virtual)
5. Lock 전략 교체 구조 추가 (No/Optimistic/Pessimistic/Redis 인터페이스)
6. 기본 인메모리 어댑터로 실행 가능 상태 확보
7. Actuator/Prometheus 노출 기본 설정

완료 기준 체크:

- [x] 애플리케이션 기동
- [x] 실험 API 호출 가능
- [x] 전략 교체 구조 동작

---

## Day 2: 주문 유스케이스 + 기본 Lock 전략 고도화

1. 주문/재고 도메인 규칙 명확화
2. Optimistic retry 정책 고정 (백오프/최대 횟수)
3. Pessimistic 트랜잭션 경계 명시
4. 재고 불변식 검증 및 실패 케이스 분류

---

## Day 3: Thread/Lock 매트릭스 완성

1. Thread 전략별 실행 설정 고정
2. Redis 락 실 구현 및 토글 검증
3. 2(Thread) x 4(Lock) 조합 실행 점검

---

## Day 4: 관측/메트릭 구성

1. Micrometer 커스텀 메트릭 추가
2. Prometheus 수집 구성
3. Grafana 대시보드 초안
4. runId 기준 실험 결과 저장 구조 정의

---

## Day 5: k6 시나리오 자동화

1. S1~S5 스크립트 작성
2. 조합 자동 실행 스크립트 작성 (8조합 x 반복)
3. 결과 JSON/CSV 출력 정리

---

## Day 6: 1차 벤치 + 병목 분석

1. 전체 조합 1차 실행
2. 이상치 기준 정리
3. 병목 분석 (JFR/DB lock wait/slow query)
4. 최소 튜닝 후 재실행

---

## Day 7: 리포트 + 다음 백로그

1. 조합별 결과 비교 리포트 작성
2. 권장 전략/한계 정리
3. 후속 실험 항목 백로그화

---

## 운영 원칙 (매일 공통)

1. 실험 조건 고정 (데이터/요청수/워밍업)
2. 전략별 동일 입력 유지
3. 실패 로그 및 메트릭 누락 점검
4. 변경 이력(runId, 설정, 시각) 기록
