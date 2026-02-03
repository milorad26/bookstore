package com.bookstore.controller;

import com.bookstore.dto.BookDTO;
import com.bookstore.dto.ErrorResponse;
import com.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Book Management", description = "APIs for managing books in the virtual bookstore")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "Get all books", description = "Retrieve a list of all books in the bookstore")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of books")
    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @Operation(summary = "Get book by ID", description = "Retrieve a specific book by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book found"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(bookService.getBookById(id));
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                "Book Not Found"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(summary = "Get book by ISBN", description = "Retrieve a specific book by its ISBN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book found"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<?> getBookByIsbn(@PathVariable String isbn) {
        try {
            return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                "Book Not Found"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(summary = "Search books by author", description = "Find all books by a specific author name")
    @ApiResponse(responseCode = "200", description = "Books found")
    @GetMapping("/search/author")
    public ResponseEntity<?> searchByAuthor(@RequestParam String author) {
        List<BookDTO> books = bookService.searchByAuthor(author);
        if (books.isEmpty()) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "No books found for author: " + author,
                "No Books Found"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Search books by title", description = "Find all books matching the title keyword")
    @ApiResponse(responseCode = "200", description = "Books found")
    @GetMapping("/search/title")
    public ResponseEntity<?> searchByTitle(@RequestParam String title) {
        List<BookDTO> books = bookService.searchByTitle(title);
        if (books.isEmpty()) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "No books found with title containing: " + title,
                "No Books Found"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Create a new book", description = "Add a new book to the bookstore inventory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Book created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<?> createBook(@Valid @RequestBody BookDTO bookDTO) {
        try {
            BookDTO createdBook = bookService.createBook(bookDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                "Book Creation Failed"
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Update a book", description = "Update an existing book's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book updated successfully"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, 
                                               @Valid @RequestBody BookDTO bookDTO) {
        try {
            BookDTO updatedBook = bookService.updateBook(id, bookDTO);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            ErrorResponse error = new ErrorResponse(
                status.value(),
                e.getMessage(),
                status == HttpStatus.NOT_FOUND ? "Book Not Found" : "Book Update Failed"
            );
            return ResponseEntity.status(status).body(error);
        }
    }

    @Operation(summary = "Delete a book", description = "Remove a book from the bookstore inventory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                "Book Not Found"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
