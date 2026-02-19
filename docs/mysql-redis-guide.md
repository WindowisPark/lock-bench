# MySQL + Redis 동시 실행 가이드

## 실행
```powershell
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="1234"
$env:MYSQL_JDBC_URL="jdbc:mysql://localhost:3306/lockbench?serverTimezone=UTC&characterEncoding=UTF-8"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
.\gradlew bootRun --args="--spring.profiles.active=mysql-redis"
```

간단 실행:
```powershell
.\scripts\run-mysql-redis.ps1
```

## 사전 점검
```powershell
.\scripts\redis-healthcheck.ps1 -RedisHost localhost -Port 6379
```
