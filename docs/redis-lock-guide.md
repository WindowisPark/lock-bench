# Redis 분산락 운영 가이드

## 활성화 방법
1. 애플리케이션 실행 시 프로파일 `redis-lock` 활성화.
2. 환경 변수로 Redis 접속 정보 설정.

예시:

```powershell
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
.\gradlew bootRun --args="--spring.profiles.active=redis-lock"
```

프로파일 적용이 안 될 경우 아래처럼 명시적으로 활성화할 수 있습니다.

```powershell
.\gradlew bootRun --args="--spring.profiles.active=redis-lock --lockbench.redis-lock.enabled=true"
```

## 비활성화 방법
- 기본 설정은 비활성화입니다.
- `lockbench.redis-lock.enabled=false` (기본값) 상태에서 Redis 전략을 호출하면 예외가 발생합니다.

## 사전 연결 점검
Redis 연결을 먼저 확인한 뒤 실행합니다.

```powershell
.\scripts\redis-healthcheck.ps1 -RedisHost localhost -Port 6379
```

`redis-cli`가 설치되어 있으면 `PING`으로 확인하고, 없으면 TCP 연결로 대체합니다.
