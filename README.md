# LockBench

Java 21 + Spring Boot 3 기반 동시성 실험 플랫폼 골격입니다.

## 목표

- Thread 모델(Platform/Virtual)과 Lock 전략(No/Optimistic/Pessimistic/Redis)을 교체하며 성능을 비교
- 실제 서비스보다 재현 가능한 실험과 계측에 초점

## 패키지 구조

```text
io.lockbench
  ├─ api            # 실험 실행 API
  ├─ application    # 실험 오케스트레이션
  ├─ domain         # 포트/도메인 모델
  ├─ concurrency    # thread/lock 전략
  └─ infra          # 인메모리/redis 어댑터
```

## 실행

```bash
./gradlew bootRun
```

Windows:

```powershell
gradlew.bat bootRun
```

## 실험 API 예시

`POST /api/experiments/run`

```json
{
  "threadModel": "VIRTUAL",
  "lockStrategy": "OPTIMISTIC_LOCK",
  "productId": 1,
  "initialStock": 10000,
  "quantity": 1,
  "totalRequests": 1000,
  "concurrency": 200,
  "optimisticRetries": 3
}
```

## 현재 상태

- 기본 스토리지는 인메모리(`InMemoryStockAccessAdapter`)
- Redis 분산락은 `lockbench.redis-lock.enabled=true`일 때만 활성화
- MySQL/JPA는 다음 단계에서 어댑터로 연결할 수 있도록 의존성과 경계를 준비한 상태
