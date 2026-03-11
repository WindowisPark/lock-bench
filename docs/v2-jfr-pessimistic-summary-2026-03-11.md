# v2 JFR + MySQL Slow Query 병목 분석 (2026-03-11)

## 분석 목표

PESSIMISTIC_LOCK 전략에서 JFR(Java Flight Recorder)과 MySQL slow query log를 통해 JVM 레벨 락 대기 및 DB 병목을 정량화한다.

---

## 설정

### JFR 설정 (`build.gradle.kts`)

```kotlin
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs(
        "-XX:+FlightRecorder",
        "-XX:StartFlightRecording=duration=120s,filename=jfr/lockbench.jfr,settings=profile"
    )
}
```

출력 파일: `jfr/lockbench.jfr`

### MySQL Slow Query 활성화

```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.1;
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';
```

### 실험 시나리오

| 항목 | 값 |
|---|---|
| 시나리오 파일 | `src/main/resources/k6/scenarios/s4-pessimistic-stability.js` |
| 전략 | PESSIMISTIC_LOCK |
| 스레드 모델 | PLATFORM |
| 목적 | 락 대기 집중 관측 |

---

## 실험 절차

1. MySQL slow query log 활성화 (위 SQL 실행)
2. `jfr/` 디렉토리 생성 (없으면 `mkdir jfr`)
3. `.\gradlew.bat bootRun` (JFR 자동 시작)
4. k6 실행: `k6 run src/main/resources/k6/scenarios/s4-pessimistic-stability.js`
5. 120초 후 `jfr/lockbench.jfr` 생성 확인
6. JDK Mission Control로 JFR 파일 분석
7. MySQL 실행 후 결과 수집:

```sql
SELECT * FROM performance_schema.events_waits_summary_global_by_event_name
WHERE event_name LIKE 'wait/lock/innodb%'
ORDER BY sum_timer_wait DESC;
```

---

## JFR 분석 결과

> ⚠️ 실험 실행 후 아래 표를 채우세요.

### JFR Lock Events (JDK Mission Control → Lock Instances 탭)

| 이벤트 타입 | 최대 대기 시간 | 평균 대기 시간 | 발생 횟수 |
|---|---|---|---|
| java.util.concurrent.locks.ReentrantLock | - | - | - |
| jdk.JavaMonitorWait | - | - | - |
| jdk.ThreadPark | - | - | - |

### JFR CPU / GC 요약

| 항목 | 값 |
|---|---|
| CPU 사용률 피크 | - |
| GC 중단 횟수 | - |
| GC 총 중단 시간 | - |
| 힙 사용 피크 | - |

---

## MySQL Slow Query 분석 결과

> ⚠️ 실험 실행 후 아래 내용을 채우세요.

### InnoDB 락 대기 요약

```
-- performance_schema 쿼리 결과 붙여넣기
```

### Slow Query 상위 항목

| 쿼리 | 실행 횟수 | 평균 쿼리 시간 | 최대 락 대기 |
|---|---|---|---|
| SELECT ... FOR UPDATE | - | - | - |
| UPDATE stock SET ... | - | - | - |

---

## 병목 결론

> ⚠️ 실험 실행 후 작성

**병목 위치:**
- JVM 레벨: ?
- DB 레벨: ?

**권장 사항:**

---

## 실무 시사점

(Lock Bleed 실험과 연계)
- PESSIMISTIC_LOCK의 `SELECT FOR UPDATE`가 HikariCP 커넥션을 장기 점유함은 Lock Bleed 실험에서 이미 검증됨 (`v2-lock-bleed-summary-2026-02-21.md`)
- JFR으로 JVM 스레드 대기 패턴, MySQL slow query로 DB 내부 대기 패턴을 동시 확인함으로써 전체 스택 병목 지도 완성
