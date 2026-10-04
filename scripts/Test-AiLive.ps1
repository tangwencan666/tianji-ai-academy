param([string]$LegacyEnvPath,[string]$BackendUrl,[string]$Token)
$ErrorActionPreference='Stop'
if ($BackendUrl) {
  if (!$Token) { throw 'An authenticated Tianji token is required for the backend probe.' }
  try {
    $result=Invoke-RestMethod "$BackendUrl/api/ai/probe" -Method Post -Headers @{Authorization="Bearer $Token"} -TimeoutSec 45
    if (!$result.data.verified) { throw 'Not verified' }
    Write-Output "LIVE_PROBE=PASS model=$($result.data.model)"
  } catch { throw 'LIVE_PROBE=FAIL; inspect the safe provider status. No automatic retry was made.' }
} else {
  . "$PSScriptRoot/Ai-Environment.ps1"
  Import-TianjiAiEnvironment -LegacyEnvPath $LegacyEnvPath
  $headers=@{Authorization="Bearer $env:AI_API_KEY"}
  try {
    $models=Invoke-RestMethod "$($env:AI_BASE_URL.TrimEnd('/'))/models" -Headers $headers -TimeoutSec 10
    if ($env:AI_MODEL -notin @($models.data.id)) { throw 'Configured model is unavailable' }
    $body=@{model=$env:AI_MODEL;messages=@(@{role='user';content='你好'});max_tokens=64;stream=$false;thinking=@{type='disabled'}} | ConvertTo-Json -Depth 8 -Compress
    $result=Invoke-RestMethod "$($env:AI_BASE_URL.TrimEnd('/'))/chat/completions" -Method Post -Headers $headers -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 35
    if (!$result.choices[0].message.content) { throw 'Empty model response' }
    Write-Output "LIVE_PROBE=PASS model=$($result.model) reply=$($result.choices[0].message.content)"
  } catch { throw 'LIVE_PROBE=FAIL; check network, model, authorization or account balance. No key or raw provider error is printed; no retry was made.' }
}
