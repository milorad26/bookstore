# Verify MySQL Installation Script
Write-Host "Checking MySQL Service..." -ForegroundColor Cyan
$mysqlService = Get-Service | Where-Object {$_.Name -match '^mysql'}
if ($mysqlService) {
    Write-Host "✓ MySQL Service Found: $($mysqlService.DisplayName)" -ForegroundColor Green
    Write-Host "  Status: $($mysqlService.Status)" -ForegroundColor $(if ($mysqlService.Status -eq 'Running') {'Green'} else {'Yellow'})
    
    if ($mysqlService.Status -ne 'Running') {
        Write-Host "`nStarting MySQL service..." -ForegroundColor Yellow
        Start-Service $mysqlService.Name
        Write-Host "✓ MySQL service started" -ForegroundColor Green
    }
} else {
    Write-Host "✗ MySQL Server is not installed" -ForegroundColor Red
    Write-Host "`nPlease install MySQL Server from: https://dev.mysql.com/downloads/installer/" -ForegroundColor Yellow
}

Write-Host "`nChecking port 3306..." -ForegroundColor Cyan
$port3306 = netstat -ano | findstr :3306
if ($port3306) {
    Write-Host "✓ Port 3306 is in use (MySQL is listening)" -ForegroundColor Green
} else {
    Write-Host "✗ Port 3306 is not in use" -ForegroundColor Red
}
