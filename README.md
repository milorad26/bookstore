# Virtual Bookstore API

A Spring Boot REST API for managing a virtual bookstore.

## Features

- Full CRUD operations for books
- Search books by title or author
- H2 in-memory database
- RESTful API design
- Input validation

## Technologies

- Java 21
- Spring Boot 3.2.2
- Spring Data JPA
- H2 Database
- Lombok
- Maven

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven

### Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### H2 Database Console

Access the H2 console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:bookstore`
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

You can test the API using:
- cURL
- Postman
- Browser (for GET requests)
- Any HTTP client

Example cURL command:
```bash
curl http://localhost:8080/api/books
```

## Next Steps

- Add frontend (React, Angular, or Vue.js)
- Add pagination
- Add authentication and authorization
- Add order management
- Add shopping cart functionality
- Deploy to cloud platform
