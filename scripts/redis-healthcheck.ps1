param(
    [string]$RedisHost = "localhost",
    [int]$Port = 6379,
    [int]$TimeoutSeconds = 2
)

$ErrorActionPreference = "Stop"

Write-Host "Redis healthcheck 시작: $RedisHost`:$Port"

$pinged = $false
$redisCli = Get-Command "redis-cli" -ErrorAction SilentlyContinue
if ($null -ne $redisCli) {
    try {
        $ping = & $redisCli -h $RedisHost -p $Port ping 2>$null
        if ($ping -eq "PONG") {
            Write-Host "redis-cli ping: PONG"
            $pinged = $true
        } else {
            Write-Host "redis-cli ping 실패: $ping"
        }
    } catch {
        Write-Host "redis-cli 실행 실패: $($_.Exception.Message)"
    }
} else {
    Write-Host "redis-cli 미설치 - TCP 연결 확인으로 대체합니다."
}

if (-not $pinged) {
    $probe = Test-NetConnection -ComputerName $RedisHost -Port $Port -InformationLevel Detailed -WarningAction SilentlyContinue
    if ($probe.TcpTestSucceeded) {
        Write-Host "TCP 연결 성공 (Ping 대체)."
        $pinged = $true
    } else {
        Write-Host "TCP 연결 실패."
    }
}

if (-not $pinged) {
    Write-Error "Redis 헬스체크 실패."
    exit 1
}

Write-Host "Redis 헬스체크 OK."
