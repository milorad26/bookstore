-- Sample book data
INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('The Great Gatsby', 'F. Scott Fitzgerald', '978-0743273565', 15.99, 'A classic American novel set in the Jazz Age, exploring themes of wealth, love, and the American Dream.', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('To Kill a Mockingbird', 'Harper Lee', '978-0061120084', 18.99, 'A powerful story of racial injustice and childhood innocence in the American South.', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('1984', 'George Orwell', '978-0451524935', 16.99, 'A dystopian social science fiction novel and cautionary tale about totalitarianism.', 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('Pride and Prejudice', 'Jane Austen', '978-0141439518', 14.99, 'A romantic novel of manners that critiques the British landed gentry at the end of the 18th century.', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('The Catcher in the Rye', 'J.D. Salinger', '978-0316769174', 17.50, 'A story about teenage rebellion and alienation narrated by Holden Caulfield.', 65, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', '978-0747532699', 22.99, 'The first novel in the Harry Potter series, introducing the magical world of Hogwarts.', 150, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('The Hobbit', 'J.R.R. Tolkien', '978-0547928227', 19.99, 'A fantasy novel about the quest of home-loving Bilbo Baggins to win treasure guarded by a dragon.', 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (title, author, isbn, price, description, stock_quantity, created_at, updated_at) 
VALUES ('Brave New World', 'Aldous Huxley', '978-0060850524', 15.50, 'A dystopian novel set in a futuristic World State of genetically modified citizens.', 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample user data
-- Note: Passwords are BCrypt encrypted. Plain text passwords for reference:
-- johndoe: password123
-- janesmith: password456
-- admin: admin123
INSERT INTO users (username, password, first_name, last_name, email, phone_number, address, enabled, user_type, created_at, updated_at) 
VALUES ('johndoe', '$2a$10$N9qo8uLOickgx2ZMRZoMye6J954rKdgE4T7.5izzKhA7jU8qVrGFy', 'John', 'Doe', 'john.doe@example.com', '+1-555-0101', '123 Main St, New York, NY 10001', true, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, first_name, last_name, email, phone_number, address, enabled, user_type, created_at, updated_at) 
VALUES ('janesmith', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Jane', 'Smith', 'jane.smith@example.com', '+1-555-0102', '456 Oak Ave, Los Angeles, CA 90001', true, 'SUPER_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, first_name, last_name, email, phone_number, address, enabled, user_type, created_at, updated_at) 
VALUES ('admin', '$2a$10$fGHQf8AYDFz7xhOE6YJiXOZhldLQ2sMDXMEkD/PCJIgQQqXBk5MYG', 'Admin', 'User', 'admin@bookstore.com', '+1-555-0100', '1 Bookstore Way, Chicago, IL 60601', true, 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample order data
-- Order 1: John Doe's completed order
INSERT INTO orders (user_id, order_date, total_amount, status, shipping_address, billing_address, order_notes, created_at, updated_at)
VALUES (1, CURRENT_TIMESTAMP - INTERVAL '7' DAY, 58.97, 'DELIVERED', '123 Main St, New York, NY 10001', '123 Main St, New York, NY 10001', 'Please leave at front door', CURRENT_TIMESTAMP - INTERVAL '7' DAY, CURRENT_TIMESTAMP - INTERVAL '7' DAY);

-- Order items for Order 1
INSERT INTO order_items (order_id, book_id, quantity, price, created_at, updated_at)
VALUES (1, 1, 2, 15.99, CURRENT_TIMESTAMP - INTERVAL '7' DAY, CURRENT_TIMESTAMP - INTERVAL '7' DAY); -- 2x The Great Gatsby

INSERT INTO order_items (order_id, book_id, quantity, price, created_at, updated_at)
VALUES (1, 3, 1, 16.99, CURRENT_TIMESTAMP - INTERVAL '7' DAY, CURRENT_TIMESTAMP - INTERVAL '7' DAY); -- 1x 1984

INSERT INTO order_items (order_id, book_id, quantity, price, created_at, updated_at)
VALUES (1, 5, 1, 17.50, CURRENT_TIMESTAMP - INTERVAL '7' DAY, CURRENT_TIMESTAMP - INTERVAL '7' DAY); -- 1x The Catcher in the Rye

-- Order 2: Jane Smith's processing order  
INSERT INTO orders (user_id, order_date, total_amount, status, shipping_address, billing_address, order_notes, created_at, updated_at)
VALUES (2, CURRENT_TIMESTAMP - INTERVAL '3' DAY, 37.98, 'PROCESSING', '456 Oak Ave, Los Angeles, CA 90001', '456 Oak Ave, Los Angeles, CA 90001', 'Express delivery requested', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '3' DAY);

-- Order items for Order 2
INSERT INTO order_items (order_id, book_id, quantity, price, created_at, updated_at)
VALUES (2, 2, 2, 18.99, CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '3' DAY); -- 2x To Kill a Mockingbird

-- Order 3: Admin's pending order (to demonstrate pending order functionality)
INSERT INTO orders (user_id, order_date, total_amount, status, shipping_address, billing_address, created_at, updated_at)
VALUES (3, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, 42.98, 'PENDING', '1 Bookstore Way, Chicago, IL 60601', '1 Bookstore Way, Chicago, IL 60601', CURRENT_TIMESTAMP - INTERVAL '1' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR);

-- Order items for Order 3
INSERT INTO order_items (order_id, book_id, quantity, price, created_at, updated_at)
VALUES (3, 6, 1, 22.99, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR); -- 1x Harry Potter

INSERT INTO order_items (order_id, book_id, quantity, price, created_at, updated_at)
VALUES (3, 7, 1, 19.99, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR); -- 1x The Hobbit

