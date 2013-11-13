@echo off
if ""%JITS_HOME%""=="""" goto errorHome
@SET JITS_PROJECT_NAME=
@SET JITS_PROJECT_PATH=
@SET JITS_PROJECT_TYPE=
@SET JITS_PROJECT_ORG=
@SET JITS_TEMP_ARG_NAME=
@SET JITS_TEMP_ARG_VALUE=

:setupArgs
if ""%1""=="""" goto doneStart
@SET JITS_TEMP_ARG_NAME=%1
shift
@SET JITS_TEMP_ARG_VALUE=%1
shift
if ""%JITS_TEMP_ARG_NAME%""==""name"" goto setupProjectName
if ""%JITS_TEMP_ARG_NAME%""==""org"" goto setupProjectOrg
if ""%JITS_TEMP_ARG_NAME%""==""path"" goto setupProjectPath
if ""%JITS_TEMP_ARG_NAME%""==""type"" goto setupProjectType
@echo %JITS_TEMP_ARG_NAME% is error parameter name
goto end
 
:setupProjectName
@SET JITS_PROJECT_NAME=%JITS_TEMP_ARG_VALUE%
goto setupArgs

:setupProjectOrg
@SET JITS_PROJECT_ORG=%JITS_TEMP_ARG_VALUE%
goto setupArgs

:setupProjectPath
@SET JITS_PROJECT_PATH=%JITS_TEMP_ARG_VALUE%
goto setupArgs

:setupProjectType
@SET JITS_PROJECT_TYPE=%JITS_TEMP_ARG_VALUE%
goto setupArgs

:errorHome
@echo JITS_HOME environment variable is not set
@echo Please run jitsvars.bat First
goto end

:doneStart   
@if ""%JITS_PROJECT_NAME%""=="""" goto errorProjectName
@if ""%JITS_PROJECT_ORG%""=="""" goto errorProjectOrg
@if ""%JITS_PROJECT_TYPE%""=="""" goto defaultProjectType
@if ""%JITS_PROJECT_PATH%""=="""" goto defaultProjectPath
goto createProject

:defaultProjectType
@SET JITS_PROJECT_TYPE=jar
goto doneStart

:defaultProjectPath
@SET JITS_PROJECT_PATH=%CD%\%JITS_PROJECT_NAME%
goto doneStart

:errorProjectName
@echo Must be set project name
goto end

:errorProjectOrg
@echo Must be set project org
goto end

:createProject
@call ant -f "%JITS_HOME%\\settings\\ant\\tools.xml" create-project -Dtarget.project.name="%JITS_PROJECT_NAME%" -Dtarget.project.path="%JITS_PROJECT_PATH%" -Dtarget.project.type="%JITS_PROJECT_TYPE%" -Dtarget.project.org="%JITS_PROJECT_ORG%"

:end
@echo.
