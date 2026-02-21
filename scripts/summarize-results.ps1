param([string]$ResultDir)

$data = Get-Content "$ResultDir\aggregate.json" -Raw | ConvertFrom-Json
$rows = $data | Where-Object { $_.runStatus -eq 'OK' }
$groups = $rows | Group-Object threadModel, lockStrategy

"{0,-10} {1,-28} {2,4} {3,9} {4,9} {5,14}" -f 'Thread','Strategy','Runs','SuccRate','Avg p95','Avg Tput(rps)'
"-" * 80
foreach ($g in $groups | Sort-Object Name) {
    $items = $g.Group
    $n = $items.Count
    $srList = $items | ForEach-Object {
        $total = $_.successCount + $_.failureCount
        if ($total -gt 0) { $_.successCount / $total * 100 } else { 0 }
    }
    $sr  = ($srList | Measure-Object -Average).Average
    $p95 = ($items | ForEach-Object { [double]$_.p95Millis } | Measure-Object -Average).Average
    $tp  = ($items | ForEach-Object { [double]$_.throughputPerSec } | Measure-Object -Average).Average
    "{0,-10} {1,-28} {2,4} {3,8:F1}% {4,9:F1} {5,14:F1}" -f $items[0].threadModel, $items[0].lockStrategy, $n, $sr, $p95, $tp
}
