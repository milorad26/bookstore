# Virtual Bookstore

A Spring Boot REST API backend for managing a virtual bookstore.

## Features

- 📚 Full CRUD operations for books
- 🔍 Search books by title or author
- 💾 H2 file-based database
- ✅ Input validation
- 🔄 RESTful API design
- 📝 API documentation with Swagger
- 🧪 Comprehensive test suite

## Technologies

- Java 21
- Spring Boot 3.2.2
- Spring Data JPA
- H2 Database
- Lombok
- Maven
- JUnit 5

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven

### Running the Application

```bash
# From the project root
mvn spring-boot:run
```

The backend API will start on `http://localhost:8080`

**Quick Start Script**

Windows:
```bash
start.bat
```

Linux/Mac:
```bash
chmod +x start.sh
./start.sh
```

### API Documentation

Access the Swagger UI at: `http://localhost:8080/swagger-ui.html`

Interactive API documentation with:
- All endpoints documented
- Try out requests directly in the browser
- Request/response examples

### H2 Database Console

Access the H2 console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:file:./data/bookstore`
- Username: `sa`
- Password: (leave empty)

## API Endpoints

### Get All Books
```
GET /api/books
```

### Get Book by ID
```
GET /api/books/{id}
```

### Get Book by ISBN
```
GET /api/books/isbn/{isbn}
```

### Search Books by Author
```
GET /api/books/search/author?author={author}
```

### Search Books by Title
```
GET /api/books/search/title?title={title}
```

### Create Book
```
POST /api/books
Content-Type: application/json

{
  "title": "Book Title",
  "author": "Author Name",
  "isbn": "978-0-123456-78-9",
  "price": 19.99,
  "description": "Book description",
  "stockQuantity": 10
}
```

### Update Book
```
PUT /api/books/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "author": "Author Name",
  "isbn": "978-0-123456-78-9",
  "price": 24.99,
  "description": "Updated description",
  "stockQuantity": 15
}
```

### Delete Book
```
DELETE /api/books/{id}
```

## Sample Data

The application comes pre-loaded with 5 sample books:
- The Great Gatsby
- To Kill a Mockingbird
- 1984
- Pride and Prejudice
- The Catcher in the Rye

## Testing the API

### Using Postman Collection

A complete Postman collection is included in the project:
- **Collection**: `Virtual-Bookstore.postman_collection.json`
- **Environment**: `Virtual-Bookstore-Local.postman_environment.json`

**To import into Postman:**
1. Open Postman
2. Click **Import** button
3. Select both JSON files
4. Select the "Virtual Bookstore - Local" environment
5. Start making requests!

The collection includes all 8 API endpoints with sample data and pre-configured requests for testing.

### Using cURL

```bash
# Get all books
curl http://localhost:8080/api/books

# Create a new book
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "978-0-13-235088-4",
    "price": 42.99,
    "description": "A Handbook of Agile Software Craftsmanship",
    "stockQuantity": 10
  }'
```

### Other Options
- Browser (for GET requests)
- Swagger UI at http://localhost:8080/swagger-ui.html
- Any HTTP client

## Project Structure

```
virtual-bookstore/
├── src/
│   ├── main/
│   │   ├── java/com/bookstore/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── service/        # Business logic
│   │   │   ├── repository/     # Data access layer
│   │   │   ├── model/          # JPA entities
│   │   │   ├── dto/            # Data transfer objects
│   │   │   ├── config/         # Configuration classes
│   │   │   └── exception/      # Exception handlers
│   │   └── resources/
│   │       ├── application.yml
│   │       └── data.sql
│   └── test/                    # Unit and integration tests
├── data/                        # H2 database files
├── pom.xml
├── Virtual-Bookstore.postman_collection.json
├── Virtual-Bookstore-Local.postman_environment.json
├── start.bat
├── start.sh
└── README.md
```

## Testing

### Backend Tests

```bash
mvn test
```

Run specific test:
```bash
mvn test -Dtest=BookRepositoryTest
```

## Next Steps

- Add pagination to book list
- Add authentication and authorization
- Add order management
- Add shopping cart functionality
- Deploy to cloud platform (AWS, Azure, etc.)
