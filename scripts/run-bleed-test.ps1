param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$ProcessingDelayMillis = 100,
    [string]$ThreadModel = "PLATFORM",
    [int]$TotalRequests = 1000,
    [int]$InitialStock = 10000,
    [int]$ReadRate = 10,
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutDir = Join-Path $PSScriptRoot "..\src\main\resources\k6\results\bleed-$timestamp"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$scenarioPath = Join-Path $PSScriptRoot "..\src\main\resources\k6\scenarios\s6-lock-bleed.js"

$strategies = @("NO_LOCK", "OPTIMISTIC_LOCK", "PESSIMISTIC_LOCK", "REDIS_DISTRIBUTED_LOCK")

$rows = @()

foreach ($strategy in $strategies) {
    $caseId = "$ThreadModel`__$strategy"
    $stdoutPath = Join-Path $OutDir ($caseId + ".stdout.log")
    $stderrPath = Join-Path $OutDir ($caseId + ".stderr.log")

    Write-Host ""
    Write-Host "[BLEED] $caseId (delay=${ProcessingDelayMillis}ms)"

    $args = @(
        "run",
        "-e", "BASE_URL=$BaseUrl",
        "-e", "THREAD_MODEL=$ThreadModel",
        "-e", "LOCK_STRATEGY=$strategy",
        "-e", "TOTAL_REQUESTS=$TotalRequests",
        "-e", "INITIAL_STOCK=$InitialStock",
        "-e", "PROCESSING_DELAY_MILLIS=$ProcessingDelayMillis",
        "-e", "READ_RATE=$ReadRate",
        $scenarioPath
    )

    $process = Start-Process `
        -FilePath "k6" `
        -ArgumentList $args `
        -NoNewWindow `
        -PassThru `
        -Wait `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath

    $allLines = @()
    if (Test-Path $stdoutPath) { $allLines += Get-Content $stdoutPath }
    if (Test-Path $stderrPath) { $allLines += Get-Content $stderrPath }
    $allLines | ForEach-Object { Write-Host $_ }

    # read p95 파싱 (k6 summary에서 http_req_duration{type:read} p95 추출)
    $readP95Line = $allLines | Where-Object { $_ -match "http_req_duration.*type:read" } | Select-Object -Last 1
    $readP95 = if ($readP95Line -match "p\(95\)=([0-9.]+)") { [double]$matches[1] } else { -1 }

    $readFailLine = $allLines | Where-Object { $_ -match "http_req_failed.*type:read" } | Select-Object -Last 1
    $readFailRate = if ($readFailLine -match "([0-9.]+)%") { [double]$matches[1] } else { -1 }

    $writeLog = $allLines | Where-Object { $_ -match "BLEED_WRITE" } | Select-Object -Last 1
    $writeTput = if ($writeLog -match "tput=([0-9.]+)") { [double]$matches[1] } else { -1 }
    $writeP95 = if ($writeLog -match "p95=([0-9.]+)") { [double]$matches[1] } else { -1 }

    $rows += [PSCustomObject]@{
        strategy              = $strategy
        threadModel           = $ThreadModel
        processingDelayMillis = $ProcessingDelayMillis
        readP95Ms             = $readP95
        readFailRatePct       = $readFailRate
        writeP95Ms            = $writeP95
        writeTputRps          = $writeTput
        k6ExitCode            = $process.ExitCode
    }
}

Write-Host ""
Write-Host "===== Lock Bleed Summary (delay=${ProcessingDelayMillis}ms) ====="
"{0,-28} {1,10} {2,14} {3,10} {4,12}" -f "Strategy","Read p95(ms)","Read Fail(%)","Write p95","Write Tput"
"-" * 80
foreach ($r in $rows) {
    $rp95  = if ($r.readP95Ms -ge 0)       { "{0,10:F1}" -f $r.readP95Ms }       else { "     N/A" }
    $rfail = if ($r.readFailRatePct -ge 0)  { "{0,14:F2}" -f $r.readFailRatePct } else { "           N/A" }
    $wp95  = if ($r.writeP95Ms -ge 0)       { "{0,10:F1}" -f $r.writeP95Ms }      else { "     N/A" }
    $wtput = if ($r.writeTputRps -ge 0)     { "{0,12:F1}" -f $r.writeTputRps }    else { "         N/A" }
    "{0,-28} {1} {2} {3} {4}" -f $r.strategy, $rp95, $rfail, $wp95, $wtput
}

$csvPath = Join-Path $OutDir "bleed-summary.csv"
$rows | Export-Csv -Path $csvPath -NoTypeInformation -Encoding utf8
Write-Host ""
Write-Host "결과 저장: $csvPath"
