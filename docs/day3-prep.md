# Day 3 적용 기록

## 결정 사항
1. Redis 비활성 상태에서 Redis 락 전략 요청 시 구성 오류로 즉시 실패
2. Thread 모델별 concurrency는 요청값이 아닌 설정 고정값 사용
3. 2(Thread) x 4(Lock) 조합은 `/api/experiments/matrix-run`으로 일괄 실행
4. 각 시나리오 실행 전 `initialStock`으로 재고 재초기화

## 적용 결과
- `lockbench.thread-fixed.platform-concurrency`, `lockbench.thread-fixed.virtual-concurrency` 도입
- `RedisDistributedLockStrategy`에 구성검증 예외 추가
- 매트릭스 실행 DTO/오케스트레이터/API 추가
- 조합 점검 테스트 보강 (Redis/Thread 설정)
