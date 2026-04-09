param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$LockStrategy = "PESSIMISTIC_LOCK",
    [int]$ProcessingDelayMillis = 100,
    [int]$TotalRequests = 1000,
    [int]$InitialStock = 10000,
    [string]$ReadDuration = "90s",
    [int]$ReadRate = 10,
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutDir = Join-Path $PSScriptRoot ("results\lockbleed-pool-" + $timestamp)
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$scenarioPath = Join-Path $PSScriptRoot "scenarios\s6-lock-bleed.js"

Write-Host "=== Lock Bleed Pool Tuning Test ==="
Write-Host "Lock Strategy: $LockStrategy"
Write-Host "Processing Delay: ${ProcessingDelayMillis}ms"
Write-Host "Note: Restart the app with different HIKARI_MAX_POOL_SIZE between runs."
Write-Host ""

$caseId = "${LockStrategy}__delay${ProcessingDelayMillis}ms"
$stdoutPath = Join-Path $OutDir ($caseId + ".stdout.log")
$stderrPath = Join-Path $OutDir ($caseId + ".stderr.log")

Write-Host "[RUN] $caseId"

$k6Args = @(
    "run",
    "-e", "BASE_URL=$BaseUrl",
    "-e", "LOCK_STRATEGY=$LockStrategy",
    "-e", "TOTAL_REQUESTS=$TotalRequests",
    "-e", "INITIAL_STOCK=$InitialStock",
    "-e", "PROCESSING_DELAY_MILLIS=$ProcessingDelayMillis",
    "-e", "READ_DURATION=$ReadDuration",
    "-e", "READ_RATE=$ReadRate",
    $scenarioPath
)

$process = Start-Process `
    -FilePath "k6" `
    -ArgumentList $k6Args `
    -NoNewWindow `
    -PassThru `
    -Wait `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath

$exitCode = $process.ExitCode

if (Test-Path $stdoutPath) { Get-Content $stdoutPath | ForEach-Object { Write-Host $_ } }
if (Test-Path $stderrPath) { Get-Content $stderrPath | ForEach-Object { Write-Host $_ } }

Write-Host ""
Write-Host "Exit code: $exitCode"
Write-Host "Logs: $OutDir"
Write-Host ""
Write-Host "=== Next Steps ==="
Write-Host "1. Stop the app"
Write-Host "2. Restart with different pool size: HIKARI_MAX_POOL_SIZE=20 ./gradlew bootRun --args='--spring.profiles.active=mysql'"
Write-Host "3. Run this script again"
