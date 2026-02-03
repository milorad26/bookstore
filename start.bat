@echo off
echo Starting Virtual Bookstore...
echo.
echo Starting Backend (Spring Boot)...
start "Virtual Bookstore Backend" cmd /k "mvn spring-boot:run"

echo.
echo Waiting for backend to start...
timeout /t 15 /nobreak

echo.
echo ========================================
echo Virtual Bookstore is running!
echo ========================================
echo Backend API: http://localhost:8080/api/books
echo Swagger UI:  http://localhost:8080/swagger-ui.html
echo H2 Console:  http://localhost:8080/h2-console
echo ========================================
echo.
echo Opening Swagger UI in browser...
start http://localhost:8080/swagger-ui.html
