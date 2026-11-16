$filePath = "C:\Users\rohit\AIFitnessTracker\app\src\main\java\com\pro\aifitnesstracker\MainActivity.kt"
$lines = Get-Content -Path $filePath

$functionsToDelete = @(
    "DashboardScreen",
    "SummaryItem",
    "VitalGaugeCard",
    "ActionCard",
    "MiniFeatureCard",
    "AchievementScreen",
    "BadgeCard",
    "AnalyticsScreen",
    "AnalyticsCard",
    "BarChart",
    "WaterTrackerScreen",
    "WaterTrackingScreen",
    "WaterStatCard",
    "AlgorithmLabScreen",
    "LungTestScreen",
    "AICoachScreen",
    "InjuryRiskScreen",
    "RiskBar",
    "SettingsScreen"
)

# We want to also delete settings helper duplicates, but only the first ones (defined inside MainActivity settings section)
# Settings helper names:
$helpersToDelete = @(
    "BadgeIcon",
    "SettingsRow",
    "SettingsListDialog"
)

# Let's collect ranges to delete
$ranges = @()

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    
    # Check if this line defines a function we want to delete
    $matchedFun = $null
    foreach ($fun in $functionsToDelete) {
        if ($line -match "fun\s+$fun\b") {
            $matchedFun = $fun
            break
        }
    }
    
    # Also check helpers, but we only delete the first definitions (before line 5110)
    if (-not $matchedFun) {
        foreach ($helper in $helpersToDelete) {
            if ($line -match "fun\s+$helper\b" -and $i -lt 5110) {
                $matchedFun = $helper
                break
            }
        }
    }
    
    if ($matchedFun) {
        # Find the start line of this block (including annotations like @Composable, @OptIn)
        $startLineIdx = $i
        while ($startLineIdx -gt 0 -and ($lines[$startLineIdx - 1] -match '@Composable' -or $lines[$startLineIdx - 1] -match '@OptIn' -or $lines[$startLineIdx - 1] -match '^\s*$')) {
            $startLineIdx--
        }
        
        # Find the end line by matching braces starting from $i
        $braceCount = 0
        $started = $false
        $endLineIdx = -1
        for ($k = $i; $k -lt $lines.Length; $k++) {
            $curLine = $lines[$k]
            for ($charIdx = 0; $charIdx -lt $curLine.Length; $charIdx++) {
                $char = $curLine[$charIdx]
                if ($char -eq '{') {
                    $braceCount++
                    $started = $true
                } elseif ($char -eq '}') {
                    $braceCount--
                }
            }
            if ($started -and $braceCount -eq 0) {
                $endLineIdx = $k
                break
            }
        }
        
        if ($endLineIdx -ne -1) {
            $ranges += [PSCustomObject]@{
                StartIdx = $startLineIdx
                EndIdx = $endLineIdx
                Name = $matchedFun
            }
            # Skip past this function
            $i = $endLineIdx
        }
    }
}

# Now delete the ranges in reverse order so indices don't shift
$ranges = $ranges | Sort-Object StartIdx -Descending

$newLines = [System.Collections.Generic.List[string]]::new($lines)
foreach ($range in $ranges) {
    Write-Output "Deleting function $($range.Name) from line $($range.StartIdx + 1) to $($range.EndIdx + 1)"
    $count = $range.EndIdx - $range.StartIdx + 1
    $newLines.RemoveRange($range.StartIdx, $count)
}

# Save updated lines
Set-Content -Path $filePath -Value $newLines -Encoding utf8
Write-Output "Successfully cleaned up MainActivity.kt!"
