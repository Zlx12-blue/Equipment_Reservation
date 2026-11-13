# 一键启动开发环境：MySQL(3306) + 后端(8080) + 前端(5173)
# 用法：在项目根目录执行  .\start-dev.ps1
$ErrorActionPreference = 'Stop'

$root      = Split-Path -Parent $MyInvocation.MyCommand.Path
$mysqld    = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe'
$mysqlData = Join-Path $root 'mysql-data'

# Maven 不在 PATH 时自动查找：优先 PATH，其次 E:\Maven，再次 ~/.m2 下的 wrapper 发行版
function Resolve-Maven {
    $cmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @()
    $candidates += Get-ChildItem 'E:\Maven' -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin\mvn.cmd' }
    $candidates += Get-ChildItem (Join-Path $env:USERPROFILE '.m2\wrapper\dists') -Recurse -Filter 'mvn.cmd' -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName
    $candidates += Join-Path $root 'backend\mvnw.cmd'
    $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}

$mvn = Resolve-Maven

function Test-Port([int]$port) {
    # 用系统连接表判断，兼容 IPv4/IPv6（Vite 只监听 ::1）
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return [bool]$conn
}

function Wait-Port([int]$port, [int]$seconds = 60) {
    for ($i = 0; $i -lt $seconds; $i++) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

# 1. MySQL
if (Test-Port 3306) {
    Write-Host '[跳过] MySQL 已在 3306 运行' -ForegroundColor Yellow
} else {
    if (-not (Test-Path $mysqlData)) {
        Write-Host "[错误] 数据目录不存在：$mysqlData" -ForegroundColor Red
        Write-Host '首次使用请先初始化：mysqld --initialize-insecure --datadir="{0}"' -f $mysqlData -ForegroundColor Red
        exit 1
    }
    Start-Process -FilePath $mysqld -ArgumentList "--datadir=$mysqlData", '--port=3306', '--console' -WorkingDirectory $root
    if (Wait-Port 3306 30) { Write-Host '[启动] MySQL 3306 就绪' -ForegroundColor Green }
    else { Write-Host '[错误] MySQL 启动失败，请检查 mysql-data 目录或 3306 端口占用' -ForegroundColor Red; exit 1 }
}

# 2. 后端
if (Test-Port 8080) {
    Write-Host '[跳过] 后端已在 8080 运行' -ForegroundColor Yellow
} elseif (-not $mvn) {
    Write-Host '[错误] 未找到 Maven（mvn.cmd），请安装 Maven 并加入 PATH，或在脚本中指定路径' -ForegroundColor Red
    exit 1
} else {
    Start-Process -FilePath $mvn -ArgumentList 'spring-boot:run' -WorkingDirectory (Join-Path $root 'backend')
    Write-Host "[启动] 后端 spring-boot:run（$mvn，首次编译约需 1 分钟）" -ForegroundColor Green
}

# 3. 前端
if (Test-Port 5173) {
    Write-Host '[跳过] 前端已在 5173 运行' -ForegroundColor Yellow
} else {
    Start-Process -FilePath 'npm.cmd' -ArgumentList 'run', 'dev' -WorkingDirectory (Join-Path $root 'frontend')
    Write-Host '[启动] 前端 npm run dev' -ForegroundColor Green
}

Write-Host ''
Write-Host '前端地址: http://localhost:5173' -ForegroundColor Cyan
Write-Host '测试账号: student1 / teacher1 / admin，密码均为 123456' -ForegroundColor Cyan
