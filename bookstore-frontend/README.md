# Virtual Bookstore Frontend

A Vue.js 3 frontend for the Virtual Bookstore backend application.

## Features

- User authentication (Login/Register)
- Book browsing and searching
- Responsive design with Bootstrap 5
- Form validation matching backend rules
- JWT token management
- Error handling with backend error messages

## Setup & Installation

### Prerequisites
- Node.js 16+ 
- npm or yarn

### Installation

```bash
cd bookstore-frontend
npm install
```

### Development Server

```bash
npm run dev
```

The application will be available at `http://localhost:5173`

Make sure your backend is running on `http://localhost:8080`

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## Project Structure

```
src/
├── assets/              # Static assets
├── components/          # Reusable Vue components
│   ├── Alert.vue       # Alert/notification component
│   └── FormField.vue   # Form input field component
├── pages/              # Page components (routed)
│   ├── Login.vue       # Login page
│   ├── Register.vue    # Registration page
│   └── Shop.vue        # Book shop/catalog page
├── router/             # Vue Router configuration
├── services/           # API service layer
│   ├── api.js         # Axios instance with interceptors
│   ├── authService.js # Authentication API calls
│   └── bookService.js # Books API calls
├── stores/            # Pinia state management
│   └── authStore.js   # Authentication state
├── utils/             # Utility functions
│   └── validators.js  # Form validation rules (matching backend)
├── App.vue           # Root component
└── main.js           # Application entry point
```

## Validation Rules

The frontend validation rules match the backend constraints:

### Login
- **Username**: Required
- **Password**: Required

### Registration
- **Username**: Required, 3-50 characters
- **Password**: Required, minimum 6 characters
- **Email**: Required, valid email format
- **First Name**: Required
- **Last Name**: Required
- **Phone Number**: Optional
- **Address**: Optional

## Error Handling

Errors from the backend are parsed and displayed:
- Validation errors show field-specific messages
- Authentication errors (401) redirect to login
- Network errors are displayed as alerts
- API errors include detailed error messages

## API Endpoints Used

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Books
- `GET /api/books` - Get all books
- `GET /api/books/{id}` - Get book by ID
- `GET /api/books/search/title?title=` - Search by title
- `GET /api/books/search/author?author=` - Search by author

## Authentication

JWT tokens are stored in localStorage and automatically included in all API requests via the Authorization header:
```
Authorization: Bearer <token>
```

## Configuration

Create a `.env.local` file to override environment variables:

```env
VITE_API_URL=http://localhost:8080/api
```

## Notes

- The frontend respects backend validation rules
- All form fields are validated before submission
- Error messages from the backend are displayed to the user
- JWT tokens are persisted in localStorage
- The app automatically redirects to login if token expires (401 response)
