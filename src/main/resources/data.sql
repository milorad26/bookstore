-- Sample book data
MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0743273565', 'The Great Gatsby', 'F. Scott Fitzgerald', 15.99, 'A classic American novel set in the Jazz Age, exploring themes of wealth, love, and the American Dream.', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0061120084', 'To Kill a Mockingbird', 'Harper Lee', 18.99, 'A powerful story of racial injustice and childhood innocence in the American South.', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0451524935', '1984', 'George Orwell', 16.99, 'A dystopian social science fiction novel and cautionary tale about totalitarianism.', 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0141439518', 'Pride and Prejudice', 'Jane Austen', 14.99, 'A romantic novel of manners that critiques the British landed gentry at the end of the 18th century.', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0316769174', 'The Catcher in the Rye', 'J.D. Salinger', 17.50, 'A story about teenage rebellion and alienation narrated by Holden Caulfield.', 65, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0747532699', 'Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', 22.99, 'The first novel in the Harry Potter series, introducing the magical world of Hogwarts.', 150, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0547928227', 'The Hobbit', 'J.R.R. Tolkien', 19.99, 'A fantasy novel about the quest of home-loving Bilbo Baggins to win treasure guarded by a dragon.', 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO books (isbn, title, author, price, description, stock_quantity, created_at, updated_at) 
KEY (isbn) VALUES ('978-0060850524', 'Brave New World', 'Aldous Huxley', 15.50, 'A dystopian novel set in a futuristic World State of genetically modified citizens.', 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample user data
-- Note: ALL TEST USERS NOW USE THE SAME PASSWORD: password123
-- johndoe: password123
-- janesmith: password123  
-- admin: password123
MERGE INTO users (username, password, first_name, last_name, email, phone_number, address, enabled, user_type, created_at, updated_at) 
KEY (username) VALUES ('johndoe', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi6', 'John', 'Doe', 'john.doe@example.com', '+1-555-0101', '123 Main St, New York, NY 10001', true, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO users (username, password, first_name, last_name, email, phone_number, address, enabled, user_type, created_at, updated_at) 
KEY (username) VALUES ('janesmith', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi6', 'Jane', 'Smith', 'jane.smith@example.com', '+1-555-0102', '456 Oak Ave, Los Angeles, CA 90001', true, 'SUPER_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO users (username, password, first_name, last_name, email, phone_number, address, enabled, user_type, created_at, updated_at) 
KEY (username) VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi6', 'Admin', 'User', 'admin@bookstore.com', '+1-555-0100', '1 Bookstore Way, Chicago, IL 60601', true, 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample order data
-- Order 1: John Doe's completed order
MERGE INTO orders (id, user_id, order_date, total_amount, status, shipping_address, billing_address, order_notes, created_at, updated_at)
KEY (id) VALUES (1, 1, DATEADD('DAY', -7, CURRENT_TIMESTAMP), 58.97, 'DELIVERED', '123 Main St, New York, NY 10001', '123 Main St, New York, NY 10001', 'Please leave at front door', DATEADD('DAY', -7, CURRENT_TIMESTAMP), DATEADD('DAY', -7, CURRENT_TIMESTAMP));

-- Order items for Order 1
MERGE INTO order_items (id, order_id, book_id, quantity, price, created_at, updated_at)
KEY (id) VALUES (1, 1, 1, 2, 15.99, DATEADD('DAY', -7, CURRENT_TIMESTAMP), DATEADD('DAY', -7, CURRENT_TIMESTAMP)); -- 2x The Great Gatsby

MERGE INTO order_items (id, order_id, book_id, quantity, price, created_at, updated_at)
KEY (id) VALUES (2, 1, 3, 1, 16.99, DATEADD('DAY', -7, CURRENT_TIMESTAMP), DATEADD('DAY', -7, CURRENT_TIMESTAMP)); -- 1x 1984

MERGE INTO order_items (id, order_id, book_id, quantity, price, created_at, updated_at)
KEY (id) VALUES (3, 1, 5, 1, 17.50, DATEADD('DAY', -7, CURRENT_TIMESTAMP), DATEADD('DAY', -7, CURRENT_TIMESTAMP)); -- 1x The Catcher in the Rye

-- Order 2: Jane Smith's processing order  
MERGE INTO orders (id, user_id, order_date, total_amount, status, shipping_address, billing_address, order_notes, created_at, updated_at)
KEY (id) VALUES (2, 2, DATEADD('DAY', -3, CURRENT_TIMESTAMP), 37.98, 'PROCESSING', '456 Oak Ave, Los Angeles, CA 90001', '456 Oak Ave, Los Angeles, CA 90001', 'Express delivery requested', DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -3, CURRENT_TIMESTAMP));

-- Order items for Order 2
MERGE INTO order_items (id, order_id, book_id, quantity, price, created_at, updated_at)
KEY (id) VALUES (4, 2, 2, 2, 18.99, DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -3, CURRENT_TIMESTAMP)); -- 2x To Kill a Mockingbird

-- Order 3: Admin's pending order (to demonstrate pending order functionality)
MERGE INTO orders (id, user_id, order_date, total_amount, status, shipping_address, billing_address, created_at, updated_at)
KEY (id) VALUES (3, 3, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 42.98, 'PENDING', '1 Bookstore Way, Chicago, IL 60601', '1 Bookstore Way, Chicago, IL 60601', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP));

-- Order items for Order 3
MERGE INTO order_items (id, order_id, book_id, quantity, price, created_at, updated_at)
KEY (id) VALUES (5, 3, 6, 1, 22.99, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP)); -- 1x Harry Potter

MERGE INTO order_items (id, order_id, book_id, quantity, price, created_at, updated_at)
KEY (id) VALUES (6, 3, 7, 1, 19.99, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP)); -- 1x The Hobbit

