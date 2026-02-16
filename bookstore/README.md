# Virtual Bookstore

A Spring Boot REST API backend for managing a virtual bookstore.

## Features

- 📚 Full CRUD operations for books
- � User management with role-based permissions  
- 🛒 Complete order management system
- 📦 Order items with multiple books per order
- 📊 Stock management with automatic deduction
- 🔍 Search books by title or author
- 💾 H2 file-based database
- ✅ Input validation & exception handling
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

## User Management

### Get All Users
```
GET /api/users
```

### Get User by ID
```
GET /api/users/{id}
```

### Get User by Username
```
GET /api/users/username/{username}
```

### Create User
```
POST /api/users
Content-Type: application/json
X-User-Id: {current-user-id}

{
  "username": "newuser",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe", 
  "email": "john.doe@example.com",
  "phoneNumber": "+1-555-0123",
  "address": "123 Main St, City, State 12345",
  "userType": "USER"
}
```

**Permission Rules:**
- Regular users: Cannot create other users
- Super users: Can only create regular users  
- Admins: Can create users of any type

### Update User
```
PUT /api/users/{id}
Content-Type: application/json
X-User-Id: {current-user-id}

{
  "firstName": "Updated Name",
  "lastName": "Updated Last",
  "email": "updated.email@example.com",
  "phoneNumber": "+1-555-9999",
  "address": "456 New St, City, State 54321",
  "enabled": true,
  "userType": "USER"
}
```

**Permission Rules:**
- Regular users: Can only edit their own profile (cannot change userType)
- Super users: Can only edit regular users (cannot change userType)
- Admins: Can edit any user and change userType

### Delete User
```
DELETE /api/users/{id}
```

## Order Management

### Get All Orders
```
GET /api/orders
```

### Get Order by ID
```
GET /api/orders/{id}
```

### Get Orders by User ID
```
GET /api/orders/user/{userId}
```
**Returns:**
- `200 OK` - List of orders (may be empty if user has no orders)
- `404 Not Found` - User doesn't exist

### Get Pending Order for User
```
GET /api/orders/user/{userId}/pending
```
**Returns:**
- `200 OK` - Pending order found
- `204 No Content` - User exists but has no pending order
- `404 Not Found` - User doesn't exist

### Get Orders by Status
```
GET /api/orders/status/{status}
```
**Valid statuses:** `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`

**Returns:**
- `200 OK` + `[orders...]` - Orders found with this status
- `404 Not Found` - No orders found with this status
- `400 Bad Request` - Invalid status provided

### Create Order
```
POST /api/orders
Content-Type: application/json

{
  "userId": 1,
  "orderItems": [
    {
      "title": "The Great Gatsby",
      "author": "F. Scott Fitzgerald",
      "quantity": 2
    },
    {
      "title": "1984",
      "author": "George Orwell", 
      "quantity": 1
    }
  ],
  "shippingAddress": "123 Main St, New York, NY 10001",
  "billingAddress": "123 Main St, New York, NY 10001", 
  "orderNotes": "Please leave at front door"
}
```

**📚 Book Identification:**
Users simply provide the book **title** and **author** - the most natural way to identify books!

**💡 Tip:** Use the book search endpoints to find exact titles and authors:
- Search by title: `GET /api/books/search/title?title=gatsby`
- Search by author: `GET /api/books/search/author?author=fitzgerald`
- Get all books: `GET /api/books`

**Business Rules:**
- One user can only have one pending order at a time
- Stock availability is checked before order creation
- Historical pricing is preserved in order items

### Confirm Order (Deducts Stock)
```
PUT /api/orders/{id}/confirm
```

### Update Order Status  
```
PUT /api/orders/{id}/status?status={newStatus}
```

### Cancel Order
```
PUT /api/orders/{id}/cancel
```

### Delete Order
```
DELETE /api/orders/{id}
```
Only pending or cancelled orders can be deleted.

## Sample Data

The application comes pre-loaded with sample data:

**Books (8 total):**
- The Great Gatsby
- To Kill a Mockingbird  
- 1984
- Pride and Prejudice
- The Catcher in the Rye
- Harry Potter and the Philosopher's Stone
- The Hobbit
- Brave New World

**Users (3 total):**
- `johndoe` (USER) - password: `password123`
- `janesmith` (SUPER_USER) - password: `password456`
- `admin` (ADMIN) - password: `admin123`

**Orders (3 sample orders):**
- Delivered order for John Doe
- Processing order for Jane Smith  
- Pending order for Admin

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
```User Types & Permissions

The system supports three user types with different permission levels:

### USER (Regular User)
- Can view books and search
- Can create and manage their own orders
- Cannot create other users
- Can only edit their own profile

### SUPER_USER  
- All USER permissions
- Can create regular users (USER type only)
- Can edit regular users
- Cannot edit admins or other super users

### ADMIN
- Full system access
- Can create users of any type
- Can edit any user
- Can manage all orders
- Full CRUD operations on all entities

## Next Steps

- Add JWT authentication and authorization
- Add pagination to book and order lists  
- Add shopping cart functionality
- Add payment processing integration
- Add order tracking and notifications
- Add pagination to book list
- Add authentication and authorization
- Add order management
- Add shopping cart functionality
- Deploy to cloud platform (AWS, Azure, etc.)
