package com.bookstore.controller;

import com.bookstore.dto.BookDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private BookDTO testBookDTO1;
    private BookDTO testBookDTO2;

    @BeforeEach
    void setUp() {
        testBookDTO1 = new BookDTO();
        testBookDTO1.setId(1L);
        testBookDTO1.setTitle("Clean Code");
        testBookDTO1.setAuthor("Robert C. Martin");
        testBookDTO1.setIsbn("978-0-13-235088-4");
        testBookDTO1.setPrice(new BigDecimal("42.99"));
        testBookDTO1.setDescription("A Handbook of Agile Software Craftsmanship");
        testBookDTO1.setStockQuantity(10);

        testBookDTO2 = new BookDTO();
        testBookDTO2.setId(2L);
        testBookDTO2.setTitle("The Pragmatic Programmer");
        testBookDTO2.setAuthor("Andrew Hunt");
        testBookDTO2.setIsbn("978-0-13-595705-9");
        testBookDTO2.setPrice(new BigDecimal("39.99"));
        testBookDTO2.setDescription("Your Journey to Mastery");
        testBookDTO2.setStockQuantity(5);
    }

    @Test
    void testGetAllBooks_ShouldReturnBookList() throws Exception {
        // Given
        List<BookDTO> books = Arrays.asList(testBookDTO1, testBookDTO2);
        when(bookService.getAllBooks()).thenReturn(books);

        // When & Then
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Clean Code")))
                .andExpect(jsonPath("$[0].author", is("Robert C. Martin")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].title", is("The Pragmatic Programmer")));

        verify(bookService).getAllBooks();
    }

    @Test
    void testGetBookById_ShouldReturnBook() throws Exception {
        // Given
        when(bookService.getBookById(1L)).thenReturn(testBookDTO1);

        // When & Then
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Clean Code")))
                .andExpect(jsonPath("$.author", is("Robert C. Martin")))
                .andExpect(jsonPath("$.isbn", is("978-0-13-235088-4")))
                .andExpect(jsonPath("$.price", is(42.99)))
                .andExpect(jsonPath("$.stockQuantity", is(10)));

        verify(bookService).getBookById(1L);
    }

    @Test
    void testGetBookById_WithNonExistentId_ShouldReturn404() throws Exception {
        // Given
        when(bookService.getBookById(999L))
                .thenThrow(new ResourceNotFoundException("Book not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Book not found")));

        verify(bookService).getBookById(999L);
    }

    @Test
    void testGetBookByIsbn_ShouldReturnBook() throws Exception {
        // Given
        when(bookService.getBookByIsbn("978-0-13-235088-4")).thenReturn(testBookDTO1);

        // When & Then
        mockMvc.perform(get("/api/books/isbn/978-0-13-235088-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Clean Code")))
                .andExpect(jsonPath("$.isbn", is("978-0-13-235088-4")));

        verify(bookService).getBookByIsbn("978-0-13-235088-4");
    }

    @Test
    void testGetBookByIsbn_WithNonExistentIsbn_ShouldReturn404() throws Exception {
        // Given
        when(bookService.getBookByIsbn("999-9-99-999999-9"))
                .thenThrow(new ResourceNotFoundException("Book not found with ISBN: 999-9-99-999999-9"));

        // When & Then
        mockMvc.perform(get("/api/books/isbn/999-9-99-999999-9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Book not found")));

        verify(bookService).getBookByIsbn("999-9-99-999999-9");
    }

    @Test
    void testSearchByAuthor_ShouldReturnMatchingBooks() throws Exception {
        // Given
        when(bookService.searchByAuthor("Martin")).thenReturn(Arrays.asList(testBookDTO1));

        // When & Then
        mockMvc.perform(get("/api/books/search/author")
                        .param("author", "Martin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].author", containsString("Martin")));

        verify(bookService).searchByAuthor("Martin");
    }

    @Test
    void testSearchByAuthor_WithNoResults_ShouldReturn404() throws Exception {
        // Given
        when(bookService.searchByAuthor("NonExistent")).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/books/search/author")
                        .param("author", "NonExistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("No books found for author")));

        verify(bookService).searchByAuthor("NonExistent");
    }

    @Test
    void testSearchByTitle_ShouldReturnMatchingBooks() throws Exception {
        // Given
        when(bookService.searchByTitle("Code")).thenReturn(Arrays.asList(testBookDTO1));

        // When & Then
        mockMvc.perform(get("/api/books/search/title")
                        .param("title", "Code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Code")));

        verify(bookService).searchByTitle("Code");
    }

    @Test
    void testSearchByTitle_WithNoResults_ShouldReturn404() throws Exception {
        // Given
        when(bookService.searchByTitle("NonExistent")).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/books/search/title")
                        .param("title", "NonExistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("No books found with title containing")));

        verify(bookService).searchByTitle("NonExistent");
    }

    @Test
    void testCreateBook_ShouldReturnCreatedBook() throws Exception {
        // Given
        BookDTO newBookDTO = new BookDTO();
        newBookDTO.setTitle("Effective Java");
        newBookDTO.setAuthor("Joshua Bloch");
        newBookDTO.setIsbn("978-0-13-468599-1");
        newBookDTO.setPrice(new BigDecimal("49.99"));
        newBookDTO.setStockQuantity(15);

        BookDTO createdBookDTO = new BookDTO();
        createdBookDTO.setId(3L);
        createdBookDTO.setTitle("Effective Java");
        createdBookDTO.setAuthor("Joshua Bloch");
        createdBookDTO.setIsbn("978-0-13-468599-1");
        createdBookDTO.setPrice(new BigDecimal("49.99"));
        createdBookDTO.setStockQuantity(15);

        when(bookService.createBook(any(BookDTO.class))).thenReturn(createdBookDTO);

        // When & Then
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBookDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.title", is("Effective Java")))
                .andExpect(jsonPath("$.author", is("Joshua Bloch")))
                .andExpect(jsonPath("$.isbn", is("978-0-13-468599-1")))
                .andExpect(jsonPath("$.price", is(49.99)))
                .andExpect(jsonPath("$.stockQuantity", is(15)));

        verify(bookService).createBook(any(BookDTO.class));
    }

    @Test
    void testCreateBook_WithExistingIsbn_ShouldReturn400() throws Exception {
        // Given
        BookDTO duplicateBookDTO = new BookDTO();
        duplicateBookDTO.setTitle("Clean Code");
        duplicateBookDTO.setAuthor("Robert C. Martin");
        duplicateBookDTO.setIsbn("978-0-13-235088-4");
        duplicateBookDTO.setPrice(new BigDecimal("42.99"));
        duplicateBookDTO.setStockQuantity(10);

        when(bookService.createBook(any(BookDTO.class)))
                .thenThrow(new IllegalArgumentException("Book with ISBN 978-0-13-235088-4 already exists"));

        // When & Then
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateBookDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("already exists")));

        verify(bookService).createBook(any(BookDTO.class));
    }

    @Test
    void testCreateBook_WithInvalidData_ShouldReturn400() throws Exception {
        // Given - Book without required fields
        BookDTO invalidBookDTO = new BookDTO();
        invalidBookDTO.setTitle(""); // Empty title
        invalidBookDTO.setAuthor(""); // Empty author

        // When & Then
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBookDTO)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).createBook(any());
    }

    @Test
    void testUpdateBook_ShouldReturnUpdatedBook() throws Exception {
        // Given
        BookDTO updatedBookDTO = new BookDTO();
        updatedBookDTO.setId(1L);
        updatedBookDTO.setTitle("Clean Code - Updated");
        updatedBookDTO.setAuthor("Robert C. Martin");
        updatedBookDTO.setIsbn("978-0-13-235088-4");
        updatedBookDTO.setPrice(new BigDecimal("45.99"));
        updatedBookDTO.setStockQuantity(20);

        when(bookService.updateBook(eq(1L), any(BookDTO.class))).thenReturn(updatedBookDTO);

        // When & Then
        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBookDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Clean Code - Updated")))
                .andExpect(jsonPath("$.price", is(45.99)))
                .andExpect(jsonPath("$.stockQuantity", is(20)));

        verify(bookService).updateBook(eq(1L), any(BookDTO.class));
    }

    @Test
    void testUpdateBook_WithNonExistentId_ShouldReturn404() throws Exception {
        // Given
        BookDTO updatedBookDTO = new BookDTO();
        updatedBookDTO.setTitle("Test Book");
        updatedBookDTO.setAuthor("Test Author");
        updatedBookDTO.setIsbn("111-1-11-111111-1");
        updatedBookDTO.setPrice(new BigDecimal("25.99"));

        when(bookService.updateBook(eq(999L), any(BookDTO.class)))
                .thenThrow(new ResourceNotFoundException("Book not found with id: 999"));

        // When & Then
        mockMvc.perform(put("/api/books/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBookDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Book not found")));

        verify(bookService).updateBook(eq(999L), any(BookDTO.class));
    }

    @Test
    void testDeleteBook_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(bookService).deleteBook(1L);

        // When & Then
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());

        verify(bookService).deleteBook(1L);
    }

    @Test
    void testDeleteBook_WithNonExistentId_ShouldReturn404() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Book not found with id: 999"))
                .when(bookService).deleteBook(999L);

        // When & Then
        mockMvc.perform(delete("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Book not found")));

        verify(bookService).deleteBook(999L);
    }

    @Test
    void testGetAllBooks_WithEmptyList_ShouldReturnEmptyArray() throws Exception {
        // Given
        when(bookService.getAllBooks()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(bookService).getAllBooks();
    }
}
