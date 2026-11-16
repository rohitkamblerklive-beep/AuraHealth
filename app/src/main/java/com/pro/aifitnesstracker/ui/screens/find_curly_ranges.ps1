$lines = Get-Content -Path C:\Users\rohit\AIFitnessTracker\app\src\main\java\com\pro\aifitnesstracker\MainActivity.kt
$composables = @()

for ($i = 0; $i -lt $lines.Length; $i++) {
    if ($lines[$i] -match '@Composable') {
        for ($j = 1; $j -le 3; $j++) {
            if ($i + $j -lt $lines.Length) {
                $nextLine = $lines[$i + $j]
                if ($nextLine -match 'fun\s+(\w+)\s*\(') {
                    $funName = $Matches[1]
                    
                    # Find end brace
                    $braceCount = 0
                    $started = $false
                    $endLine = -1
                    for ($k = $i + $j; $k -lt $lines.Length; $k++) {
                        $line = $lines[$k]
                        for ($charIdx = 0; $charIdx -lt $line.Length; $charIdx++) {
                            $char = $line[$charIdx]
                            if ($char -eq '{') {
                                $braceCount++
                                $started = $true
                            } elseif ($char -eq '}') {
                                $braceCount--
                            }
                        }
                        if ($started -and $braceCount -eq 0) {
                            $endLine = $k + 1
                            break
                        }
                    }
                    
                    $composables += [PSCustomObject]@{
                        Name = $funName
                        StartLine = $i + 1
                        EndLine = $endLine
                    }
                    break
                }
            }
        }
    }
}

$composables | Format-Table -AutoSize
