@echo off
echo ========================================
echo AI API Platform - Windows 快速启动脚本
echo ========================================
echo.

REM 检查 Docker 是否已安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Docker，请先安装 Docker Desktop
    pause
    exit /b 1
)

echo [1/4] 检查环境配置...
if not exist .env (
    echo [警告] 未找到 .env 文件，正在从示例文件复制...
    copy .env.example .env
    echo.
    echo [提示] 已创建 .env 配置文件，如需自定义请编辑该文件
    echo.
)

echo [2/4] 停止旧容器...
docker-compose down

echo [3/4] 构建并启动所有服务...
docker-compose up -d --build

echo [4/4] 等待服务启动...
timeout /t 10 /nobreak

echo.
echo ========================================
echo 服务启动成功！
echo ========================================
echo.
echo 访问地址：
echo - 前端: http://localhost
echo - 后端 API: http://localhost:8080
echo.
echo 默认管理员账号：
echo - 用户名: admin
echo - 密码: admin123
echo.
echo 提示：
echo - 如需使用 Copilot API，请单独启动并在后台配置
echo - 查看日志: docker-compose logs -f
echo - 停止服务: docker-compose down
echo.
echo ========================================
pause
