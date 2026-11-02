$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=UTC"
$rootDir = "c:\Users\archi\OneDrive\Desktop\digital wallet advance java"

function Start-ServiceProcess($name, $dir) {
    Write-Host "Starting $name..."
    Start-Process -FilePath "$dir\mvnw.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory $dir -WindowStyle Hidden
}

Start-ServiceProcess "eureka-server" "$rootDir\eureka-server"
Start-Sleep -Seconds 10

Start-ServiceProcess "auth-service" "$rootDir\auth-service"
Start-ServiceProcess "customer-service" "$rootDir\customer-service"
Start-ServiceProcess "account-service" "$rootDir\account-service"
Start-ServiceProcess "transaction-service" "$rootDir\transaction-service"
Start-Sleep -Seconds 12

Start-ServiceProcess "api-gateway" "$rootDir\api-gateway"
Write-Host "All services triggered!"
