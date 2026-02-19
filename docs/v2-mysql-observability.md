# v2 MySQL 관측 가이드

## MySQL 프로파일 실행
```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
.\gradlew bootRun
```

## Slow Query Log (권장)
MySQL에서 슬로우쿼리 로그를 활성화합니다.
```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.1;
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

## InnoDB 락 대기 확인
```sql
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
```

## Performance Schema 대기 이벤트
```sql
SELECT EVENT_NAME, COUNT_STAR, SUM_TIMER_WAIT
FROM performance_schema.events_waits_summary_global_by_event_name
WHERE EVENT_NAME LIKE 'wait/lock/innodb/%'
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 10;
```
