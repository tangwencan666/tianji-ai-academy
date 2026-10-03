param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$envFile = Join-Path $projectRoot '.env'
if (!(Test-Path -LiteralPath $envFile)) { throw 'Run scripts/init-env.ps1 first.' }
Get-Content -LiteralPath $envFile | ForEach-Object {
 if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process') }
}
$jarPath = Join-Path $projectRoot 'tj-server/target/tj-server-1.0.0.jar'
if (!(Test-Path -LiteralPath $jarPath)) { throw 'Build first: mvn clean package -Plocal' }
if ($env:JAVA_HOME) { $javaPath = Join-Path $env:JAVA_HOME 'bin/java.exe' } else { $javaPath = (Get-Command java).Source }
& $javaPath -jar $jarPath --spring.profiles.active=local --server.address=127.0.0.1 "--server.port=$Port"
