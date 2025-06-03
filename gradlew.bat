@echo off
rem --------------------------------------------------------------------------
rem Gradle startup script for Windows
rem --------------------------------------------------------------------------
set GRADLE_HOME=%~dp0
"%GRADLE_HOME%\gradle\wrapper\gradle-wrapper.jar" %*
