@echo off

REM ═══════════════════════════════════════════
REM XXL-Job 调度中心启动脚本
REM 请修改下方环境变量为实际值
REM ═══════════════════════════════════════════

set XXL_MYSQL_PASSWORD=your_db_password
set XXL_JOB_ACCESS_TOKEN=your_xxl_job_access_token

cd /d "%~dp0"
java -jar "%CD%\xxl-job-admin-2.4.1.jar" --spring.config.additional-location=file:"%CD%\application-prod.yml"
pause
