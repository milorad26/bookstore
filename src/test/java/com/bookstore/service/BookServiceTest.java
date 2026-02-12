package com.bookstore.service;

import com.bookstore.dto.BookDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book testBook1;
    private Book testBook2;
    private BookDTO testBookDTO1;

    @BeforeEach
    void setUp() {
        testBook1 = new Book();
        testBook1.setId(1L);
        testBook1.setTitle("Clean Code");
        testBook1.setAuthor("Robert C. Martin");
        testBook1.setIsbn("978-0-13-235088-4");
        testBook1.setPrice(new BigDecimal("42.99"));
        testBook1.setDescription("A Handbook of Agile Software Craftsmanship");
        testBook1.setStockQuantity(10);

        testBook2 = new Book();
        testBook2.setId(2L);
        testBook2.setTitle("The Pragmatic Programmer");
        testBook2.setAuthor("Andrew Hunt");
        testBook2.setIsbn("978-0-13-595705-9");
        testBook2.setPrice(new BigDecimal("39.99"));
        testBook2.setDescription("Your Journey to Mastery");
        testBook2.setStockQuantity(5);

        testBookDTO1 = new BookDTO();
        testBookDTO1.setTitle("Clean Code");
        testBookDTO1.setAuthor("Robert C. Martin");
        testBookDTO1.setIsbn("978-0-13-235088-4");
        testBookDTO1.setPrice(new BigDecimal("42.99"));
        testBookDTO1.setDescription("A Handbook of Agile Software Craftsmanship");
        testBookDTO1.setStockQuantity(10);
    }

    @Test
    void testGetAllBooks_ShouldReturnBookDTOList() {
        // Given
        List<Book> books = Arrays.asList(testBook1, testBook2);
        when(bookRepository.findAll()).thenReturn(books);

        // When
        List<BookDTO> result = bookService.getAllBooks();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.get(1).getTitle()).isEqualTo("The Pragmatic Programmer");
        verify(bookRepository).findAll();
    }

    @Test
    void testGetBookById_ShouldReturnBookDTO() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook1));

        // When
        BookDTO result = bookService.getBookById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
        verify(bookRepository).findById(1L);
    }

    @Test
    void testGetBookById_WithNonExistentId_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getBookById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found with id: 999");
        
        verify(bookRepository).findById(999L);
    }

    @Test
    void testGetBookByIsbn_ShouldReturnBookDTO() {
        // Given
        when(bookRepository.findByIsbn("978-0-13-235088-4")).thenReturn(Optional.of(testBook1));

        // When
        BookDTO result = bookService.getBookByIsbn("978-0-13-235088-4");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsbn()).isEqualTo("978-0-13-235088-4");
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        verify(bookRepository).findByIsbn("978-0-13-235088-4");
    }

    @Test
    void testGetBookByIsbn_WithNonExistentIsbn_ShouldThrowException() {
        // Given
        when(bookRepository.findByIsbn("999-9-99-999999-9")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getBookByIsbn("999-9-99-999999-9"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found with ISBN: 999-9-99-999999-9");
        
        verify(bookRepository).findByIsbn("999-9-99-999999-9");
    }

    @Test
    void testSearchByAuthor_ShouldReturnMatchingBooks() {
        // Given
        when(bookRepository.findByAuthorContainingIgnoreCase("Martin"))
                .thenReturn(Arrays.asList(testBook1));

        // When
        List<BookDTO> result = bookService.searchByAuthor("Martin");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor()).contains("Martin");
        verify(bookRepository).findByAuthorContainingIgnoreCase("Martin");
    }

    @Test
    void testSearchByTitle_ShouldReturnMatchingBooks() {
        // Given
        when(bookRepository.findByTitleContainingIgnoreCase("Code"))
                .thenReturn(Arrays.asList(testBook1));

        // When
        List<BookDTO> result = bookService.searchByTitle("Code");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).contains("Code");
        verify(bookRepository).findByTitleContainingIgnoreCase("Code");
    }

    @Test
    void testCreateBook_ShouldSaveAndReturnBookDTO() {
        // Given
        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook1);

        // When
        BookDTO result = bookService.createBook(testBookDTO1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        verify(bookRepository).existsByIsbn("978-0-13-235088-4");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testCreateBook_WithExistingIsbn_ShouldThrowException() {
        // Given
        when(bookRepository.existsByIsbn("978-0-13-235088-4")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> bookService.createBook(testBookDTO1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Book with ISBN 978-0-13-235088-4 already exists");
        
        verify(bookRepository).existsByIsbn("978-0-13-235088-4");
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void testUpdateBook_ShouldModifyAndReturnBookDTO() {
        // Given
        BookDTO updatedDTO = new BookDTO();
        updatedDTO.setTitle("Clean Code - Updated");
        updatedDTO.setAuthor("Robert C. Martin");
        updatedDTO.setIsbn("978-0-13-235088-4");
        updatedDTO.setPrice(new BigDecimal("45.99"));
        updatedDTO.setDescription("Updated description");
        updatedDTO.setStockQuantity(15);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook1));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook1);

        // When
        BookDTO result = bookService.updateBook(1L, updatedDTO);

        // Then
        assertThat(result).isNotNull();
        verify(bookRepository).findById(1L);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testUpdateBook_WithNonExistentId_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.updateBook(999L, testBookDTO1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found with id: 999");
        
        verify(bookRepository).findById(999L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void testUpdateBook_WithDifferentIsbnThatAlreadyExists_ShouldThrowException() {
        // Given
        BookDTO updatedDTO = new BookDTO();
        updatedDTO.setTitle("Clean Code");
        updatedDTO.setAuthor("Robert C. Martin");
        updatedDTO.setIsbn("978-0-13-595705-9"); // Different ISBN that already exists
        updatedDTO.setPrice(new BigDecimal("42.99"));
        updatedDTO.setStockQuantity(10);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook1));
        when(bookRepository.existsByIsbn("978-0-13-595705-9")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> bookService.updateBook(1L, updatedDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Book with ISBN 978-0-13-595705-9 already exists");
        
        verify(bookRepository).findById(1L);
        verify(bookRepository).existsByIsbn("978-0-13-595705-9");
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void testUpdateBook_WithSameIsbn_ShouldNotCheckExistence() {
        // Given
        BookDTO updatedDTO = new BookDTO();
        updatedDTO.setTitle("Clean Code - Updated");
        updatedDTO.setAuthor("Robert C. Martin");
        updatedDTO.setIsbn("978-0-13-235088-4"); // Same ISBN
        updatedDTO.setPrice(new BigDecimal("45.99"));
        updatedDTO.setStockQuantity(15);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook1));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook1);

        // When
        BookDTO result = bookService.updateBook(1L, updatedDTO);

        // Then
        assertThat(result).isNotNull();
        verify(bookRepository).findById(1L);
        verify(bookRepository, never()).existsByIsbn(anyString());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testDeleteBook_ShouldRemoveBook() {
        // Given
        when(bookRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(1L);

        // When
        bookService.deleteBook(1L);

        // Then
        verify(bookRepository).existsById(1L);
        verify(bookRepository).deleteById(1L);
    }

    @Test
    void testDeleteBook_WithNonExistentId_ShouldThrowException() {
        // Given
        when(bookRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> bookService.deleteBook(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found with id: 999");
        
        verify(bookRepository).existsById(999L);
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void testCreateBook_WithNullStockQuantity_ShouldDefaultToZero() {
        // Given
        BookDTO dtoWithNullStock = new BookDTO();
        dtoWithNullStock.setTitle("Test Book");
        dtoWithNullStock.setAuthor("Test Author");
        dtoWithNullStock.setIsbn("111-1-11-111111-1");
        dtoWithNullStock.setPrice(new BigDecimal("25.99"));
        dtoWithNullStock.setStockQuantity(null); // null stock

        Book savedBook = new Book();
        savedBook.setId(3L);
        savedBook.setTitle("Test Book");
        savedBook.setAuthor("Test Author");
        savedBook.setIsbn("111-1-11-111111-1");
        savedBook.setPrice(new BigDecimal("25.99"));
        savedBook.setStockQuantity(0);

        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        // When
        BookDTO result = bookService.createBook(dtoWithNullStock);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStockQuantity()).isZero();
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testSearchByAuthor_WithNoResults_ShouldReturnEmptyList() {
        // Given
        when(bookRepository.findByAuthorContainingIgnoreCase("NonExistent"))
                .thenReturn(Arrays.asList());

        // When
        List<BookDTO> result = bookService.searchByAuthor("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(bookRepository).findByAuthorContainingIgnoreCase("NonExistent");
    }

    @Test
    void testSearchByTitle_WithNoResults_ShouldReturnEmptyList() {
        // Given
        when(bookRepository.findByTitleContainingIgnoreCase("NonExistent"))
                .thenReturn(Arrays.asList());

        // When
        List<BookDTO> result = bookService.searchByTitle("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(bookRepository).findByTitleContainingIgnoreCase("NonExistent");
    }
}
