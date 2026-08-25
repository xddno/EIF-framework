@echo off
REM MIT License. Copyright (c) EIF-framework author. See NOTICE.md.
REM SPDX-License-Identifier: MIT

set "SCRIPT_DIR=%~dp0"

set "JAVA_HOME=%SCRIPT_DIR%tools\jdk8u442-b06"
set "PATH=%JAVA_HOME%\bin;%SCRIPT_DIR%tools\apache-maven-3.9.9\bin;%SCRIPT_DIR%tools\cmake-3.31.5-windows-x86_64\bin;%PATH%"

set "MAVEN_THREADS="
set "CMAKE_PARALLEL="

:parse_args
if "%~1"=="" goto :parse_done
set "arg=%~1"
if /i "%arg:~0,2%"=="-j" (
    set "threads_val=%arg:~2%"
    if "%threads_val%"=="" (
        shift
        set "threads_val=%~1"
    )
    if not "%threads_val%"=="" (
        set "MAVEN_THREADS=-T%threads_val%"
        set "CMAKE_PARALLEL=--parallel %threads_val%"
    )
)
shift
goto :parse_args
:parse_done

if exist "%SCRIPT_DIR%tools\BuildTools\devcmd.bat" call "%SCRIPT_DIR%tools\BuildTools\devcmd.bat"

if not exist "%SCRIPT_DIR%File2Hex\File2Hex.exe" (
    echo File2Hex.exe not found, building it from source...
    pushd "%SCRIPT_DIR%File2Hex"
    cl /nologo /std:c++20 /EHsc /O2 /DNDEBUG /FeFile2Hex.exe main.cpp
    if not exist "File2Hex.exe" (
        popd
        echo ERROR: Failed to build File2Hex.exe
        pause
        exit /b 1
    )
    popd
)

echo Cleaning
del /q "%SCRIPT_DIR%src\payload.jar.hpp" 2>nul
del /q "%SCRIPT_DIR%EIF\remapped\EIF-framework.jar" 2>nul
del /q "%SCRIPT_DIR%EIF\remapped\EIF-framework.jar.hpp" 2>nul
del /q "%SCRIPT_DIR%EIF\remapped\payload.jar" 2>nul
del /q "%SCRIPT_DIR%EIF\remapped\payload.jar.hpp" 2>nul
cd /d "%SCRIPT_DIR%"
CALL mvn %MAVEN_THREADS% clean -Dmaven.repo.local=EIF/local_maven_repo

echo.
echo.
echo Building jar
CALL mvn %MAVEN_THREADS% package -Dremapper.destinationNamespace=named -Dmaven.repo.local=EIF/local_maven_repo

echo.
echo.
cd /d "%SCRIPT_DIR%EIF"
if not exist remapped mkdir remapped
copy /y target\EIF-1.0-SNAPSHOT-shaded.jar remapped\EIF-framework.jar >nul
cd remapped

echo.
echo.
echo Writing jar bytes to header file
copy /y EIF-framework.jar payload.jar >nul
"%SCRIPT_DIR%File2Hex\File2Hex.exe" payload.jar
copy /y payload.jar.hpp "%SCRIPT_DIR%src\payload.jar.hpp" >nul

echo.
echo.
echo Building dll
cd /d "%SCRIPT_DIR%"
if exist build rmdir /s /q build
cmake -B build -G "NMake Makefiles" -DCMAKE_BUILD_TYPE=RelWithDebInfo -DMINECRAFT_CLASS="net/minecraft/client/Minecraft" --fresh
cmake --build build %CMAKE_PARALLEL%

echo.
echo.
echo Build complete: %SCRIPT_DIR%build\JarLoader.dll
