# Sube este proyecto al repositorio público examen-II-parcial.
# Uso: clic derecho -> "Ejecutar con PowerShell", o en una terminal:  powershell -ExecutionPolicy Bypass -File .\subir.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".git")) { git init | Out-Null }
git add .
git commit -m "Examen del II parcial: API REST de contactos con firma digital y app Android con Volley"
git branch -M main
if (-not (git remote)) { git remote add origin https://github.com/Isai-2004/examen-II-parcial.git }
git push -u origin main

Write-Host ""
Write-Host "Listo: https://github.com/Isai-2004/examen-II-parcial" -ForegroundColor Green
Read-Host "Presiona Enter para cerrar"
