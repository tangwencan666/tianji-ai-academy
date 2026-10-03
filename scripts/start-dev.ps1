param([int]$Port=8080,[switch]$Seed)
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!$env:DB_PASSWORD) { throw '请自行设置 DB_PASSWORD 环境变量后重试；不要把密码发到聊天或写入源码。' }
if (!$env:DB_USERNAME) { $env:DB_USERNAME='root' }
if (!$env:DB_HOST) { $env:DB_HOST='127.0.0.1' }
if (!$env:DB_PORT) { $env:DB_PORT='3306' }
if ($env:DB_NAME -and $env:DB_NAME -ne 'tianji_ai_academy') { throw '开发脚本只允许独立数据库 tianji_ai_academy' }
$env:DB_NAME='tianji_ai_academy'
if (!$env:JWT_SECRET) { $env:JWT_SECRET=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48)) }
if ($Seed -and !$env:DEMO_PASSWORD) { throw '启用Seed前请设置DEMO_PASSWORD（12至72字节）。' }
$env:SPRING_PROFILES_ACTIVE='dev'
$env:DEV_SEED=if($Seed){'true'}else{'false'}
$env:DEMO_DATA='false'
$env:SERVER_PORT="$Port"
java -jar tj-server/target/tj-server-1.0.0.jar
