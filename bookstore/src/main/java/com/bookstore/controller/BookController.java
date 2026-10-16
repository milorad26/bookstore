package com.bookstore.controller;

import com.bookstore.dto.BookDTO;
import com.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Book Management", description = "APIs for managing books in the virtual bookstore")
public class BookController {

    private final BookService bookService;
    private final MessageSource messageSource;

    @Value("${inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Operation(summary = "Get all books", description = "Retrieve a list of all books in the bookstore")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of books")
    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @Operation(summary = "Get low-stock books", description = "Retrieve books at or below the configured inventory alert threshold")
    @GetMapping("/low-stock")
    public ResponseEntity<List<BookDTO>> getLowStockBooks() {
        return ResponseEntity.ok(bookService.getLowStockBooks(lowStockThreshold));
    }

    @Operation(summary = "Get book by ID", description = "Retrieve a specific book by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book found"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @Operation(summary = "Search books by ISBN", description = "Find all books whose ISBN contains the search text")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Matching books retrieved"),
        @ApiResponse(responseCode = "404", description = "No books found with the ISBN")
    })
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<List<BookDTO>> getBookByIsbn(@PathVariable String isbn) {
        Locale locale = LocaleContextHolder.getLocale();
        List<BookDTO> books = bookService.getBookByIsbn(isbn);
        if (books.isEmpty()) {
            String message = messageSource.getMessage("book.notfound.isbn",
                new Object[]{isbn}, locale);
            throw new com.bookstore.exception.ResourceNotFoundException(message);
        }
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Search books by author", description = "Find all books by a specific author name")
    @ApiResponse(responseCode = "200", description = "Books found")
    @GetMapping("/search/author")
    public ResponseEntity<List<BookDTO>> searchByAuthor(@RequestParam String author) {
        Locale locale = LocaleContextHolder.getLocale();
        List<BookDTO> books = bookService.searchByAuthor(author);
        if (books.isEmpty()) {
            String message = messageSource.getMessage("book.notfound.author", 
                new Object[]{author}, locale);
            throw new com.bookstore.exception.ResourceNotFoundException(message);
        }
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Search books by title", description = "Find all books matching the title keyword")
    @ApiResponse(responseCode = "200", description = "Books found")
    @GetMapping("/search/title")
    public ResponseEntity<List<BookDTO>> searchByTitle(@RequestParam String title) {
        Locale locale = LocaleContextHolder.getLocale();
        List<BookDTO> books = bookService.searchByTitle(title);
        if (books.isEmpty()) {
            String message = messageSource.getMessage("book.notfound.title", 
                new Object[]{title}, locale);
            throw new com.bookstore.exception.ResourceNotFoundException(message);
        }
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Create a new book", description = "Add a new book to the bookstore inventory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Book created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Only administrators can create books")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody BookDTO bookDTO) {
        BookDTO createdBook = bookService.createBook(bookDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }

    @Operation(summary = "Update a book", description = "Update an existing book's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book updated successfully"),
        @ApiResponse(responseCode = "404", description = "Book not found"),
        @ApiResponse(responseCode = "403", description = "Only administrators can update books")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, 
                                               @Valid @RequestBody BookDTO bookDTO) {
        BookDTO updatedBook = bookService.updateBook(id, bookDTO);
        return ResponseEntity.ok(updatedBook);
    }

    @Operation(summary = "Delete a book", description = "Remove a book from the bookstore inventory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Book not found"),
        @ApiResponse(responseCode = "403", description = "Only administrators can delete books")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
