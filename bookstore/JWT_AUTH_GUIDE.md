# JWT Authentication Guide

## Overview

The Virtual Bookstore API now implements JWT (JSON Web Token) authentication to secure user and order endpoints. This guide explains how to authenticate and use protected endpoints.

## Public Endpoints

- **`/api/auth/register`** - Register a new user account
- **`/api/auth/login`** - Login and receive JWT token
- **`GET /api/books`** - Get all books (no authentication required)
- **`GET /api/books/isbn/{isbn}`** - Get book by ISBN (no authentication required)
- **`GET /api/books/search/title`** - Search books by title (no authentication required)
- **`GET /api/books/search/author`** - Search books by author (no authentication required)

## Protected Endpoints

- **`/api/users/**`** - All user management endpoints require authentication
- **`/api/orders/**`** - All order management endpoints require authentication
- **`/api/order-items/**`** - View order items (SUPER_USER and ADMIN only)
- **`GET /api/books/{id}`** - Get book by ID (SUPER_USER and ADMIN only)
- **`POST/PUT/DELETE /api/books/**`** - Create, update, delete books (ADMIN only)

## Authentication Flow

### 1. Register a New Account

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "username": "newuser",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1-555-0123",
  "address": "123 Main Street"
}
```

**Response:** (201 Created)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": 4,
  "username": "newuser",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### 2. Login with Existing Account

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "username": "johndoe",
  "password": "password123"
}
```

**Response:** (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### 3. Use JWT Token for Protected Endpoints

After receiving the JWT token from login or registration, include it in the `Authorization` header for all protected endpoints:

**Header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Example Request:**
```bash
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Test Users

The system comes with pre-configured test users (all passwords are `password123` for simplicity):

| Username  | Password    | User Type  | Description           |
|-----------|-------------|------------|-----------------------|
| johndoe   | password123 | USER       | Regular user          |
| janesmith | password123 | SUPER_USER | Super user with elevated privileges |
| admin     | password123 | ADMIN      | Administrator with full access |

## JWT Configuration

The JWT tokens are configured with the following settings:

- **Algorithm:** HS256 (HMAC with SHA-256)
- **Expiration:** 15 minutes (900,000 milliseconds)
- **Issuer:** virtual-bookstore
- **Secret:** Configurable via `JWT_SECRET` environment variable

You can customize these settings in `application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key-here}
  expiration: 900000  # 15 minutes
  refresh-expiration: 604800000  # 7 days
  issuer: virtual-bookstore
```

## Common Use Cases

### Creating an Order (Protected)

```bash
# 1. Login first
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","password":"password123"}'

# 2. Use the token to create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "orderItems": [
      {"bookId": 1, "quantity": 2}
    ],
    "shippingAddress": "123 Main St",
    "billingAddress": "123 Main St"
  }'
```

### Viewing User Profile (Protected)

```bash
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Updating User Information (Protected)

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com",
    "phoneNumber": "+1-555-9999"
  }'
```

## Error Responses

### 401 Unauthorized

Returned when:
- No JWT token is provided
- JWT token is invalid or expired
- JWT token signature verification fails

```json
{
  "error": "Unauthorized",
  "message": "Access Denied"
}
```

### 403 Forbidden

Returned when the user doesn't have permission to access the resource:

```json
{
  "error": "Forbidden",
  "message": "Access denied: You do not have permission to perform this action"
}
```

## Token Expiration

JWT tokens expire after 15 minutes. When a token expires, you'll receive a 401 Unauthorized response. To continue, simply login again to get a new token.

## Security Best Practices

1. **Never share your JWT token** - Treat it like a password
2. **Use HTTPS in production** - Always transmit tokens over secure connections
3. **Store tokens securely** - Use secure storage mechanisms in your client application
4. **Implement token refresh** - Consider implementing refresh token functionality for better UX
5. **Logout properly** - Clear tokens from client storage when logging out

## Troubleshooting

### "Authentication required" Error

Make sure you're including the `Authorization` header with the `Bearer` prefix:
```
Authorization: Bearer YOUR_TOKEN_HERE
```

### "Token has expired" Error

Login again to get a new token. Tokens expire after 15 minutes by default.

### "Invalid token" Error

- Check that you're including the full token (it's quite long)
- Verify there are no extra spaces or characters
- Make sure the token hasn't been modified

## Migration from X-User-Id Header

Previously, the API used an `X-User-Id` header for user identification. This has been replaced with JWT authentication:

**Before:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser",...}'
```

**After:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser",...}'
```

The user ID is now automatically extracted from the JWT token, providing better security and preventing user impersonation.
