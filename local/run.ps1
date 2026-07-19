# Valkyrie 一键启动脚本
# 用法: .\local\run.ps1

$ErrorActionPreference = "Stop"

# JDK 21 路径
$env:JAVA_HOME = "D:\dev\Java\jdk-21.0.10_windows-x64_bin\jdk-21.0.10"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 项目根目录
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$jarPath = "launcher/target/launcher-v1.0.0-arch.1.jar"

# 如果 jar 不存在，先编译
if (-not (Test-Path $jarPath)) {
    Write-Host "未找到 jar 包，先执行编译..." -ForegroundColor Yellow
    & "$PSScriptRoot\build.ps1"
}

Write-Host "启动 Valkyrie..." -ForegroundColor Cyan
& java -jar $jarPath
