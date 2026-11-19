$ErrorActionPreference = "Stop"
$javaPath = "C:\Users\USER\.jdks\corretto-22.0.2\bin\java.exe"
$argsFile = "D:\Software_New_Project\server_cp.args"
$mainClass = "io.flagship.server.FlagShipServerApplication"

Write-Host "Starting FlagShip Control-Plane Server on http://localhost:8080..." -ForegroundColor Cyan
& $javaPath "@$argsFile" $mainClass --server.port=8080
