# Virtual Bookstore - Quick Setup Guide

## 🚀 Quick Start

### Windows Users

Simply double-click `start.bat` or run:
```bash
start.bat
```

### Linux/Mac Users

```bash
chmod +x start.sh
./start.sh
```

This will start the Spring Boot backend on port 8080 and automatically open your browser.

## 📋 What You'll See

Once the server is running:

- **Backend API**: http://localhost:8080/api/books
- **Swagger Docs**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console

## 🎯 First Steps

1. Use Swagger UI at http://localhost:8080/swagger-ui.html to interact with the API
2. Use the Postman collection to test endpoints
3. Access the H2 console to view the database directly

## 🛠️ Troubleshooting

### Backend won't start
- Check if port 8080 is already in use
- Verify Java 21 is installed: `java -version`
- Try: `mvn clean install` then `mvn spring-boot:run`

### "Cannot connect to backend"
- Verify the backend is running on http://localhost:8080
- Check the console for error messages
- Try accessing http://localhost:8080/api/books in your browser

## 📦 Sample Data

The backend comes with pre-loaded sample books. To add more, you can:
- Use Swagger UI
- Import the Postman collection
- Use the H2 console to insert SQL data

## 🧪 Testing

### Backend Tests
```bash
mvn test
```

### Test Specific Class
```bash
mvn test -Dtest=BookRepositoryTest
```

## 💡 Tips

- The database is file-based (stored in `data/bookstore.mv.db`)
- Data persists between restarts
- ISBN must be unique
- All prices are in USD
- Stock quantity defaults to 0 if not specified

Enjoy your Virtual Bookstore! 📚
