$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envFile) { throw '.env already exists. Edit it manually to preserve your configuration.' }
function New-Secret { $bytes = New-Object byte[] 32; $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create(); $rng.GetBytes($bytes); $rng.Dispose(); return [BitConverter]::ToString($bytes).Replace('-', '') }
$content = Get-Content -LiteralPath (Join-Path $projectRoot '.env.example') -Raw
$content = $content.Replace('CHANGE_ME_RANDOM_DATABASE_PASSWORD', (New-Secret)).Replace('CHANGE_ME_RANDOM_ROOT_PASSWORD', (New-Secret)).Replace('CHANGE_ME_RANDOM_SECRET_AT_LEAST_32_CHARACTERS', (New-Secret)).Replace('CHANGE_ME_ADMIN_PASSWORD_MIN_12_CHARACTERS', (New-Secret))
[System.IO.File]::WriteAllText($envFile, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host 'Created .env with random credentials. Read BOOTSTRAP_ADMIN_PASSWORD locally to log in as admin.'
