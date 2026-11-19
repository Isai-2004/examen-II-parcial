@echo off
title API de Contactos
cd /d "%~dp0api\src\Contactos.Api"
echo Iniciando la API en http://localhost:5080/swagger ...
dotnet run
pause
