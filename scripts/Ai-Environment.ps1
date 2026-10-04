function Import-TianjiAiEnvironment {
  param([string]$LegacyEnvPath)
  if ($LegacyEnvPath) {
    if (!(Test-Path -LiteralPath $LegacyEnvPath -PathType Leaf)) { throw 'Authorized env file does not exist.' }
    foreach ($line in [IO.File]::ReadAllLines($LegacyEnvPath)) {
      if ($line -match '^\s*(?:export\s+)?(LLM_API_KEY|LLM_BASE_URL|LLM_MODEL)\s*=\s*(.*?)\s*$') {
        $name = $Matches[1]; $value = $Matches[2].Trim().Trim('"').Trim("'")
        $target = @{LLM_API_KEY='AI_API_KEY';LLM_BASE_URL='AI_BASE_URL';LLM_MODEL='AI_MODEL'}[$name]
        if (![Environment]::GetEnvironmentVariable($target,'Process')) { [Environment]::SetEnvironmentVariable($target,$value,'Process') }
      }
    }
  }
  if (!$env:AI_API_KEY -or $env:AI_API_KEY -eq 'disabled') { throw 'AI_API_KEY missing; set it in this shell or pass -LegacyEnvPath. Never print the key.' }
  if (!$env:AI_BASE_URL) { $env:AI_BASE_URL='https://api.deepseek.com' }
  $uri=[Uri]$env:AI_BASE_URL
  if ($uri.Scheme -ne 'https' -or $uri.Host -ne 'api.deepseek.com' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath.Trim('/') -notin @('','v1')) { throw 'Only the official DeepSeek HTTPS base URL is allowed.' }
  # Current official model name. Older aliases are deliberately not silently used.
  if (!$env:AI_MODEL -or $env:AI_MODEL -in @('deepseek-chat','deepseek-reasoner')) { $env:AI_MODEL='deepseek-flash' }
  $env:AI_MODE='live'; $env:AI_ENABLED='true'
}
