$ErrorActionPreference = "Stop"
$javaPath = "C:\Users\USER\.jdks\corretto-22.0.2\bin\java.exe"
$argsFile = "D:\Software_New_Project\demo_cp.args"
$mainClass = "io.flagship.demo.DemoApplication"

Write-Host "Starting FlagShip Demo App on http://localhost:8081..." -ForegroundColor Cyan
& $javaPath "@$argsFile" $mainClass --server.port=8081 --flagship.server-url=http://localhost:8080 --flagship.api-key=dev-sdk-key-secret-12345
