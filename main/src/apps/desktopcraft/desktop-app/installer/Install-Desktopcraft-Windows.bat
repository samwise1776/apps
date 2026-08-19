@echo off
setlocal
set "SOURCE_DIR=%~dp0"
set "DESKTOP_DIR=%USERPROFILE%\Desktop"
if exist "%USERPROFILE%\OneDrive\Desktop" set "DESKTOP_DIR=%USERPROFILE%\OneDrive\Desktop"
set "APP_DIR=%DESKTOP_DIR%\Desktopcraft"

where javaw >nul 2>nul
if errorlevel 1 (
  echo Desktopcraft needs Java 17 or newer.
  echo Install Java, then run this installer again.
  pause
  exit /b 1
)

if not exist "%APP_DIR%" mkdir "%APP_DIR%"
copy /Y "%SOURCE_DIR%Desktopcraft.jar" "%APP_DIR%\Desktopcraft.jar" >nul

(
  echo @echo off
  echo start "" javaw -jar "%%USERPROFILE%%\Desktop\Desktopcraft\Desktopcraft.jar"
) > "%DESKTOP_DIR%\Launch Desktopcraft.bat"

if exist "%USERPROFILE%\OneDrive\Desktop" (
  (
    echo @echo off
    echo start "" javaw -jar "%%USERPROFILE%%\OneDrive\Desktop\Desktopcraft\Desktopcraft.jar"
  ) > "%DESKTOP_DIR%\Launch Desktopcraft.bat"
)

echo Desktopcraft is installed on your Desktop.
start "" javaw -jar "%APP_DIR%\Desktopcraft.jar"
endlocal
