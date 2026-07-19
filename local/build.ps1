# Valkyrie 一键编译打包脚本
# 用法: .\local\build.ps1 [-SkipTests] [-Run]

param(
    [switch]$SkipTests = $true,
    [switch]$Run = $false
)

$ErrorActionPreference = "Stop"

# JDK 21 路径
$env:JAVA_HOME = "D:\dev\Java\jdk-21.0.10_windows-x64_bin\jdk-21.0.10"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 项目根目录
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Valkyrie Build Script" -ForegroundColor Cyan
Write-Host "  JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 清理旧日志
Get-ChildItem -Path . -Filter "build-*.log" -ErrorAction SilentlyContinue | Remove-Item -Force

# Maven 命令参数
$mavenArgs = @("package")
if ($SkipTests) {
    $mavenArgs += "-DskipTests"
}

Write-Host "[1/3] 编译打包..." -ForegroundColor Yellow
$logFile = "build-package.log"
& mvn @mavenArgs *> $logFile
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) {
    Write-Host "编译失败! 查看日志: $logFile" -ForegroundColor Red
    Get-Content $logFile -Tail 30 -Encoding UTF8
    exit $exitCode
}

Write-Host "编译成功!" -ForegroundColor Green
Write-Host ""

# 显示产物
$jarPath = "launcher/target/launcher-v1.0.0-arch.1.jar"
if (Test-Path $jarPath) {
    $jarSize = (Get-Item $jarPath).Length / 1MB
    Write-Host "[2/3] 产物: $jarPath ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[3/3] 完成!" -ForegroundColor Green
Write-Host ""

# 启动应用
if ($Run) {
    Write-Host "启动应用..." -ForegroundColor Yellow
    & java -jar $jarPath
} else {
    Write-Host "运行命令: java -jar $jarPath" -ForegroundColor Gray
    Write-Host "或使用: .\local\build.ps1 -Run" -ForegroundColor Gray
}
