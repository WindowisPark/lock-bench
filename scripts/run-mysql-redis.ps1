param(
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "1234",
    [string]$MysqlJdbcUrl = "jdbc:mysql://localhost:3306/lockbench?serverTimezone=UTC&characterEncoding=UTF-8",
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379
)

$ErrorActionPreference = "Stop"

$env:MYSQL_USERNAME = $MysqlUser
$env:MYSQL_PASSWORD = $MysqlPassword
$env:MYSQL_JDBC_URL = $MysqlJdbcUrl
$env:REDIS_HOST = $RedisHost
$env:REDIS_PORT = "$RedisPort"

.\gradlew bootRun --args="--spring.profiles.active=mysql-redis"
