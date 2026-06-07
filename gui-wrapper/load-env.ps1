param(
    [string]$EnvFile = ".env",
    [switch]$PersistUser
)

$envPath = if ([System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile
} else {
    Join-Path $PSScriptRoot $EnvFile
}

if (-not (Test-Path $envPath)) {
    throw "Env file not found: $envPath"
}

Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith("#")) { return }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) { return }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim()

    if ($value.StartsWith('"') -and $value.EndsWith('"') -and $value.Length -ge 2) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    $value = [Environment]::ExpandEnvironmentVariables($value)

    if ($PersistUser) {
        if ($key -ieq "PATH") {
            $javaBin = Join-Path $env:JAVA_HOME "bin"
            $userPath = [Environment]::GetEnvironmentVariable("PATH", "User")

            if ([string]::IsNullOrWhiteSpace($userPath)) {
                $newUserPath = $javaBin
            } elseif ($userPath -split ';' | Where-Object { $_ -eq $javaBin }) {
                $newUserPath = $userPath
            } else {
                $newUserPath = "$javaBin;$userPath"
            }

            [Environment]::SetEnvironmentVariable("PATH", $newUserPath, "User")
            Set-Item -Path "Env:PATH" -Value ([Environment]::ExpandEnvironmentVariables("$newUserPath;%PATH%"))
            Write-Host "Set PATH (User + current session)"
        } else {
            [Environment]::SetEnvironmentVariable($key, $value, "User")
            Set-Item -Path "Env:$key" -Value $value
            Write-Host "Set $key (User + current session)"
        }
    } else {
        Set-Item -Path "Env:$key" -Value $value
        Write-Host "Set $key"
    }
}

Write-Host "Environment variables loaded from $envPath"
if ($PersistUser) {
    Write-Host "Persisted to User environment. New terminals will inherit these values."
} else {
    Write-Host "Applied to current session only. Run with -PersistUser to make it permanent for your user."
}
