param([string]$LegacyEnvPath,[string]$JavaHome=$env:JAVA_HOME,[string]$MavenSettings,
  [int]$BackendPort=18273,[int]$FrontendPort=15175,[switch]$SkipBuild,[switch]$Restart)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/Ai-Environment.ps1"
Import-TianjiAiEnvironment -LegacyEnvPath $LegacyEnvPath
if(!$env:DB_PASSWORD){throw 'DB_PASSWORD missing. Set it securely in this PowerShell; no prompt or password output is used.'}
if($env:DB_NAME -and $env:DB_NAME -ne 'tianji_ai_academy'){throw 'Only tianji_ai_academy is allowed. Other databases are never initialized.'}
$env:DB_NAME='tianji_ai_academy'
if(!$env:DB_HOST){$env:DB_HOST='127.0.0.1'};if(!$env:DB_PORT){$env:DB_PORT='3306'};if(!$env:DB_USERNAME){$env:DB_USERNAME='root'}
if(!$env:REDIS_HOST){$env:REDIS_HOST='127.0.0.1'};if(!$env:REDIS_PORT){$env:REDIS_PORT='6379'}
if($JavaHome){$java=Join-Path $JavaHome 'bin/java.exe';$env:JAVA_HOME=$JavaHome}else{$java=(Get-Command java -ErrorAction Stop).Source}
$node=(Get-Command node -ErrorAction Stop).Source
$jar=Join-Path $project 'tj-server/target/tj-server-1.0.0.jar'
$vite=Join-Path $project 'tj-web/node_modules/vite/bin/vite.js'
if(!(Test-Path -LiteralPath $vite)){throw 'Install the frontend dependencies with npm ci first.'}
$runtime=Join-Path $project 'work/live-preview'
New-Item -ItemType Directory -Path $runtime -Force|Out-Null
$manifestPath=Join-Path $runtime 'processes.json'
if($Restart -and (Test-Path -LiteralPath $manifestPath)){
  $old=Get-Content -LiteralPath $manifestPath -Raw|ConvertFrom-Json
  if($old.projectPath -ne $project){throw 'Runtime manifest belongs to another project.'}
  foreach($kind in @('backend','frontend')){
    $processId=$old."${kind}Pid";$start=$old."${kind}Start";$exe=$old."${kind}Exe"
    $owned=Get-Process -Id $processId -ErrorAction SilentlyContinue
    if($owned){
      if($owned.Path -ne $exe -or $owned.StartTime.ToUniversalTime() -ne ([datetime]$start).ToUniversalTime()){throw 'Process identity changed; no process was stopped.'}
      Stop-Process -InputObject $owned
    }
  }
}
foreach($port in @($BackendPort,$FrontendPort)){
  $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$port)
  try{$listener.Start()}catch{throw "Port $port is occupied. No unknown process was stopped."}finally{$listener.Stop()}
}
if(!$SkipBuild){
  Push-Location $project
  try{
    $argsList=@('-B','-DskipTests','package');if($MavenSettings){$argsList=@('-gs',$MavenSettings,'-s',$MavenSettings)+$argsList}
    & mvn @argsList *> (Join-Path $runtime 'build.log')
    if($LASTEXITCODE -ne 0){throw 'Build failed. See work/live-preview/build.log (never contains environment values).'}
  }finally{Pop-Location}
}
if(!(Test-Path -LiteralPath $jar)){throw 'Backend jar missing. Run mvn package first.'}
if(!$env:JWT_SECRET){$env:JWT_SECRET=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))}
$env:SPRING_PROFILES_ACTIVE='dev';$env:SERVER_PORT="$BackendPort";$env:SERVER_ADDRESS='127.0.0.1'
$env:SPRING_FLYWAY_ENABLED='false';$env:SPRING_SQL_INIT_MODE='never';$env:DEV_SEED='false';$env:DEMO_DATA='false'
$env:EMBEDDING_MODE='lexical';$env:EMBEDDING_MODEL='local-lexical-hash-v1'
$runId=Get-Date -Format 'yyyyMMdd-HHmmss';$backend=$null;$frontend=$null
$saved=@{}
try{
  $backend=Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$jar+'"')) -WorkingDirectory $project -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime "$runId-backend.log") -RedirectStandardError (Join-Path $runtime "$runId-backend-error.log")
  $base="http://127.0.0.1:$BackendPort";$deadline=[DateTime]::UtcNow.AddSeconds(60);$ready=$false
  do{
    $backend.Refresh();if($backend.HasExited){throw 'Backend exited. Inspect the safe runtime log.'}
    try{if((Invoke-RestMethod "$base/actuator/health" -TimeoutSec 2).status -eq 'UP'){$ready=$true;break}}catch{}
    Start-Sleep -Milliseconds 500
  }while([DateTime]::UtcNow -lt $deadline)
  if(!$ready){throw 'MySQL/Redis readiness did not pass within 60 seconds.'}
  $provider=(Invoke-RestMethod "$base/api/ai/provider" -TimeoutSec 5).data
  if(!$provider.enabled -or $provider.mode -ne 'live'){throw 'Provider is not configured for live mode.'}
  $env:API_PROXY_TARGET=$base
  # Frontend must never inherit infrastructure or model credentials. Restore the caller env afterward.
  foreach($name in @('DB_PASSWORD','JWT_SECRET','AI_API_KEY','LLM_API_KEY','EMBEDDING_API_KEY','REDIS_PASSWORD','DEMO_PASSWORD')){
    $saved[$name]=[Environment]::GetEnvironmentVariable($name,'Process');[Environment]::SetEnvironmentVariable($name,$null,'Process')
  }
  $frontend=Start-Process -FilePath $node -ArgumentList @(('"'+$vite+'"'),'--host','127.0.0.1','--port',"$FrontendPort",'--strictPort') -WorkingDirectory (Join-Path $project 'tj-web') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime "$runId-frontend.log") -RedirectStandardError (Join-Path $runtime "$runId-frontend-error.log")
  $deadline=[DateTime]::UtcNow.AddSeconds(20);$ready=$false
  do{
    $frontend.Refresh();if($frontend.HasExited){throw 'Frontend exited.'}
    try{if((Invoke-WebRequest "http://127.0.0.1:$FrontendPort" -TimeoutSec 2).StatusCode -eq 200){$ready=$true;break}}catch{}
    Start-Sleep -Milliseconds 300
  }while([DateTime]::UtcNow -lt $deadline)
  if(!$ready){throw 'Frontend readiness exceeded 20 seconds.'}
  $manifest=[pscustomobject]@{projectPath=$project;backendPid=$backend.Id;frontendPid=$frontend.Id;backendStart=$backend.StartTime.ToUniversalTime().ToString('o');frontendStart=$frontend.StartTime.ToUniversalTime().ToString('o');backendExe=$java;frontendExe=$node;backendUrl=$base;frontendUrl="http://127.0.0.1:$FrontendPort"}
  $manifest|ConvertTo-Json|Set-Content -LiteralPath $manifestPath -Encoding UTF8
  Write-Output "PREVIEW=READY url=http://127.0.0.1:$FrontendPort AI_MODE=live model=$($provider.model) verified=$($provider.verified) DB=tianji_ai_academy migrations=false seed=false"
}catch{
  foreach($owned in @($frontend,$backend)){if($owned){$owned.Refresh();if(!$owned.HasExited){$owned.Kill()}}}
  throw
}finally{foreach($name in $saved.Keys){[Environment]::SetEnvironmentVariable($name,$saved[$name],'Process')}}
