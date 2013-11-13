@echo off
@SET CUR_DIR=%~dp0
@SET CUR_DIR=%CUR_DIR:~0,-1%
if ""%JITS_HOME%""=="""" goto setup_sdk_home
if ""%JITS_HOME%""==""%CUR_DIR%"" goto sdk_home_success
:setup_sdk_home
@SET JITS_HOME=%~dp0
@SET JITS_HOME=%JITS_HOME:~0,-1%
@SET PATH=%JITS_HOME%\tools;%PATH%
:sdk_home_success
@echo.
@echo     .-. .-. .-----.  .--. 
@echo     : : : : `-. .-' : .--'
@echo   _ : : : :   : :   `. `. 
@echo  : :; : : :   : :    _`, :
@echo  `.__.' :_;   :_;   `.__.'  0.1-BETA by neeker @2012
@echo.
@echo  JITS SDK home path is "%JITS_HOME%"
@echo  The name has been set for JITS_HOME environment variable
@echo.
@echo.
