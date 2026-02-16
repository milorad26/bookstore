#!/bin/bash

echo "Starting Virtual Bookstore..."
echo ""
echo "Starting Backend (Spring Boot)..."

# Start backend in background
mvn spring-boot:run &
BACKEND_PID=$!

# Wait for backend to start
echo "Waiting for backend to start..."
sleep 15

echo ""
echo "========================================"
echo "Virtual Bookstore is running!"
echo "========================================"
echo "Backend API: http://localhost:8080/api/books"
echo "Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "H2 Console:  http://localhost:8080/h2-console"
echo "========================================"
echo ""
echo "Opening Swagger UI in browser..."

# Open browser based on OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    open http://localhost:8080/swagger-ui.html
else
    xdg-open http://localhost:8080/swagger-ui.html 2>/dev/null || echo "Please open http://localhost:8080/swagger-ui.html in your browser"
fi

echo ""
echo "Press Ctrl+C to stop the server"

# Wait for user interrupt
trap "kill $BACKEND_PID; exit" INT
wait
