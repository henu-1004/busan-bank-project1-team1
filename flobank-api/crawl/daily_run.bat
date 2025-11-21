@echo off
cd /d "%~dp0"

echo ----------------------------------------
echo [INFO] 파이썬 실행을 시작합니다...
echo ----------------------------------------

:: 파이썬 버전이 나오는지 확인 (안 나오면 파이썬 경로 문제)
python --version

:: 파이썬 스크립트 실행
python depositRate_crawl.py

echo.
echo [INFO] 실행이 끝났습니다. 엔터를 누르면 닫힙니다.
pause