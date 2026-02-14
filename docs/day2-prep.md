# Day 2 준비안 (roadmap-1week)

기준 문서: `docs/roadmap-1week.md`
현재 코드 기준: In-memory 어댑터 + 전략 교체 구조 완료

## 목표 재확인
Day 2 목표는 아래 4개를 "코드 작성 전에" 합의하는 것이다.

1. 주문/재고 도메인 규칙 명확화
2. Optimistic retry 정책 고정 (백오프/최대 횟수)
3. Pessimistic 트랜잭션 경계 명시
4. 재고 불변식 검증 및 실패 케이스 분류

## 현재 상태 요약
- Optimistic 재시도는 `optimisticRetries` 횟수만큼 즉시 반복(백오프 없음)
- Pessimistic은 in-memory `ReentrantLock`으로 임계영역 보호
- 실패 원인은 boolean(false)로만 반환되어 구분 정보가 없음
- 주문/재고 도메인 타입(명시적 Order, FailureReason, Invariant) 부재

## Day 2 선행 논의 게이트 (필수)
아래 항목은 구현 또는 정책 확정 전에 반드시 논의한다.

1. 주문 실패 분류 체계
- 옵션 A: `OUT_OF_STOCK`, `VERSION_CONFLICT`, `LOCK_TIMEOUT`, `INVALID_QUANTITY`, `PRODUCT_NOT_FOUND`
- 옵션 B: 벤치 목적 최소 분류(`OUT_OF_STOCK`, `CONFLICT`, `OTHER`)로 시작
- 결정 영향: 응답 DTO, 메트릭 라벨, 테스트 케이스 범위

2. Optimistic retry 정책
- 최대 재시도: 요청값 사용 vs 서버 상한 고정
- 백오프: 없음 vs 고정 지연 vs 지수 + jitter
- 권장 초안: Day 2는 고정 소량 백오프(예: 1~3ms 랜덤) + 서버 상한 도입

3. Pessimistic 경계 정의
- in-memory 단계: 락 획득~재고 감소까지
- JPA 단계 대비: 트랜잭션 시작/종료를 어디서 관리할지(서비스 vs 전략)
- 권장 초안: 전략 내부는 임계영역만, 트랜잭션 경계는 application/service 계층으로 분리 가능하게 유지

4. 불변식 정의
- 재고는 음수가 될 수 없음
- 성공 주문 수량 합 <= 초기 재고
- 버전은 성공 업데이트마다 단조 증가
- 동일 입력에서 전략별 검증 방식 동일

## 실행 순서 (Day 2)
1. 실패 분류/불변식 합의
2. Optimistic retry 정책 합의
3. Pessimistic 경계 합의
4. 테스트 명세 작성(코드 구현 전)
5. 구현
6. 검증(단위 + 동시성 반복 테스트)

## 구현 전 테스트 명세 초안
1. `optimistic` 충돌 시 재시도 내 성공/실패 경계 검증
2. `pessimistic` 동시 주문 시 음수 재고 미발생 검증
3. 실패 원인별 분류 정확성 검증
4. 총 성공/실패 건수 합 == 요청 수 검증
5. 버전 증가 단조성 검증

## Day 2 완료 기준 (체크리스트)
- [x] 실패 분류 enum 또는 동등 구조 확정
- [x] retry 정책(횟수/백오프) 코드 및 설정 반영
- [x] pessimistic 경계 주석/문서/코드로 명시
- [x] 불변식 테스트 통과
- [x] 실험 응답 또는 로그에 실패 분류 추적 가능

---

## 정책 확정안 (2026-02-14)
1. 실패 분류는 상세형으로 확정
- `OUT_OF_STOCK`, `VERSION_CONFLICT`, `LOCK_TIMEOUT`, `INVALID_QUANTITY`, `PRODUCT_NOT_FOUND`

2. Optimistic retry 정책 확정
- 최대 재시도: `min(request.optimisticRetries, 5)` (서버 상한 5)
- 백오프: 지수 백오프 + jitter
- 제안 값: base 2ms, 최대 32ms, 시도마다 랜덤(0~base) 추가

선정 이유:
- 충돌 상황에서 즉시 재시도 폭주(thundering herd)를 줄임
- 무한/과도 재시도로 tail latency가 악화되는 것을 방지
- 요청값을 일부 반영하면서도 서버 보호 상한으로 운영 안전성 확보
- 랜덤 지터로 동시 재시도 정렬을 깨서 충돌 확률 완화

3. Pessimistic 경계는 권장초안대로 확정
- 전략 내부 책임: 임계영역(락 획득~재고 갱신)
- application/service 책임: 트랜잭션 경계

아키텍처 이유(간단):
- 동시성 제어(락)와 데이터 일관성 단위(트랜잭션)를 분리하면 교체 가능성과 테스트 용이성이 높아짐
- Day 3/4 이후 Redis, JPA로 확장할 때도 전략은 동일 인터페이스를 유지하고, 트랜잭션 정책만 서비스 계층에서 조정 가능
