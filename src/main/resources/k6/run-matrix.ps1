param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Repeats = 3,
    [int]$TotalRequests = 1000,
    [int]$InitialStock = 10000,
    [int]$Quantity = 1,
    [int]$OptimisticRetries = 5,
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutDir = Join-Path $PSScriptRoot ("results\matrix-" + $timestamp)
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$scenarioPath = Join-Path $PSScriptRoot "scenarios\run-single-combo.js"

$threads = @("PLATFORM", "VIRTUAL")
$locks = @("NO_LOCK", "OPTIMISTIC_LOCK", "PESSIMISTIC_LOCK", "REDIS_DISTRIBUTED_LOCK")

$rows = @()

for ($repeat = 1; $repeat -le $Repeats; $repeat++) {
    foreach ($thread in $threads) {
        foreach ($lock in $locks) {
            $caseId = "$thread`__$lock`__R$repeat"
            $jsonPath = Join-Path $OutDir ($caseId + ".json")
            $stdoutPath = Join-Path $OutDir ($caseId + ".stdout.log")
            $stderrPath = Join-Path $OutDir ($caseId + ".stderr.log")

            Write-Host "[RUN] $caseId"

            $args = @(
                "run",
                "-e", "BASE_URL=$BaseUrl",
                "-e", "THREAD_MODEL=$thread",
                "-e", "LOCK_STRATEGY=$lock",
                "-e", "TOTAL_REQUESTS=$TotalRequests",
                "-e", "INITIAL_STOCK=$InitialStock",
                "-e", "QUANTITY=$Quantity",
                "-e", "OPTIMISTIC_RETRIES=$OptimisticRetries",
                "-e", "RESULT_JSON_PATH=$jsonPath",
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

            $exitCode = $process.ExitCode
            $k6OutputStrings = @()
            if (Test-Path $stdoutPath) {
                $k6OutputStrings += Get-Content $stdoutPath
            }
            if (Test-Path $stderrPath) {
                $k6OutputStrings += Get-Content $stderrPath
            }
            $k6OutputStrings | ForEach-Object { Write-Host $_ }

            $requestMarker = $k6OutputStrings | Where-Object { $_ -like "*LOCKBENCH_REQUEST *" } | Select-Object -Last 1
            $resultMarker = $k6OutputStrings | Where-Object { $_ -like "*LOCKBENCH_RESULT *" } | Select-Object -Last 1

            $requestJson = $null
            $resultJson = $null
            if ($null -ne $requestMarker) {
                if ($requestMarker -match "LOCKBENCH_REQUEST\s+(\{.*\})""\s+source=console") {
                    $requestJson = $matches[1]
                } elseif ($requestMarker -match "LOCKBENCH_REQUEST\s+(\{.*\})") {
                    $requestJson = $matches[1]
                }
            }
            if ($null -ne $resultMarker) {
                if ($resultMarker -match "LOCKBENCH_RESULT\s+(\{.*\})""\s+source=console") {
                    $resultJson = $matches[1]
                } elseif ($resultMarker -match "LOCKBENCH_RESULT\s+(\{.*\})") {
                    $resultJson = $matches[1]
                }
            }

            $request = $null
            $result = $null
            if (-not [string]::IsNullOrWhiteSpace($requestJson)) {
                try {
                    $requestJson = $requestJson -replace '\\"', '"'
                    $request = $requestJson | ConvertFrom-Json
                } catch {
                    $request = $null
                }
            }
            if (-not [string]::IsNullOrWhiteSpace($resultJson)) {
                try {
                    $resultJson = $resultJson -replace '\\"', '"'
                    $result = $resultJson | ConvertFrom-Json
                } catch {
                    $result = $null
                }
            }

            $item = $null

            if (Test-Path $jsonPath) {
                $item = Get-Content $jsonPath -Raw | ConvertFrom-Json
            }

            $k6 = if ($null -ne $item) { $item.k6 } else { $null }

            $perRun = [PSCustomObject]@{
                generatedAt = if ($null -ne $item) { $item.generatedAt } else { (Get-Date).ToString("o") }
                request = $request
                result = $result
                k6 = $k6
            }
            $perRun | ConvertTo-Json -Depth 10 | Out-File -FilePath $jsonPath -Encoding utf8

            $rows += [PSCustomObject]@{
                repeat = $repeat
                threadModel = $thread
                lockStrategy = $lock
                k6ExitCode = $exitCode
                runStatus = if ($null -eq $result) { "MISSING_RESULT" } elseif ($null -ne $result.runId) { "OK" } elseif ($null -ne $result.status) { "API_ERROR" } else { "UNKNOWN_RESULT" }
                apiStatus = if ($null -ne $result -and $null -ne $result.status) { $result.status } else { "" }
                runId = if ($null -ne $result -and $null -ne $result.runId) { $result.runId } else { "" }
                successCount = if ($null -ne $result) { $result.successCount } else { 0 }
                failureCount = if ($null -ne $result) { $result.failureCount } else { 0 }
                failureLockTimeout = if ($null -ne $result -and $null -ne $result.failureBreakdown) { $result.failureBreakdown.LOCK_TIMEOUT } else { 0 }
                failureOutOfStock = if ($null -ne $result -and $null -ne $result.failureBreakdown) { $result.failureBreakdown.OUT_OF_STOCK } else { 0 }
                failureVersionConflict = if ($null -ne $result -and $null -ne $result.failureBreakdown) { $result.failureBreakdown.VERSION_CONFLICT } else { 0 }
                failureInvalidQuantity = if ($null -ne $result -and $null -ne $result.failureBreakdown) { $result.failureBreakdown.INVALID_QUANTITY } else { 0 }
                failureProductNotFound = if ($null -ne $result -and $null -ne $result.failureBreakdown) { $result.failureBreakdown.PRODUCT_NOT_FOUND } else { 0 }
                failureUnknown = if ($null -ne $result -and $null -ne $result.failureBreakdown) { $result.failureBreakdown.UNKNOWN } else { 0 }
                elapsedMillis = if ($null -ne $result) { $result.elapsedMillis } else { 0 }
                elapsedNanos = if ($null -ne $result) { $result.elapsedNanos } else { 0 }
                throughputConfidence = if ($null -ne $result) { $result.throughputConfidence } else { "" }
                throughputPerSec = if ($null -ne $result) { $result.throughputPerSec } else { 0 }
                p95Millis = if ($null -ne $result) { $result.p95Millis } else { 0 }
                checksPassRate = if ($null -ne $k6) { $k6.checksPassRate } else { 0 }
                httpReqFailedRate = if ($null -ne $k6) { $k6.httpReqFailedRate } else { 1 }
                httpReqDurationAvgMs = if ($null -ne $k6) { $k6.httpReqDurationAvgMs } else { 0 }
            }
        }
    }
}

$csvPath = Join-Path $OutDir "aggregate.csv"
$jsonPath = Join-Path $OutDir "aggregate.json"

$rows | Export-Csv -Path $csvPath -NoTypeInformation -Encoding utf8
$rows | ConvertTo-Json -Depth 5 | Out-File -FilePath $jsonPath -Encoding utf8

Write-Host ""
Write-Host "Done."
Write-Host "Aggregate CSV: $csvPath"
Write-Host "Aggregate JSON: $jsonPath"
Write-Host "Per-run JSON: $OutDir\*.json"
