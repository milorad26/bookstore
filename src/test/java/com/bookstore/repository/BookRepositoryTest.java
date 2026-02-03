package com.bookstore.repository;

import com.bookstore.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    private Book testBook1;
    private Book testBook2;
    private Book testBook3;

    @BeforeEach
    void setUp() {
        // Clear the database
        bookRepository.deleteAll();
        
        // Create test books
        testBook1 = new Book();
        testBook1.setTitle("Clean Code");
        testBook1.setAuthor("Robert C. Martin");
        testBook1.setIsbn("978-0-13-235088-4");
        testBook1.setPrice(new BigDecimal("42.99"));
        testBook1.setDescription("A Handbook of Agile Software Craftsmanship");
        testBook1.setStockQuantity(10);

        testBook2 = new Book();
        testBook2.setTitle("The Pragmatic Programmer");
        testBook2.setAuthor("Andrew Hunt");
        testBook2.setIsbn("978-0-13-595705-9");
        testBook2.setPrice(new BigDecimal("39.99"));
        testBook2.setDescription("Your Journey to Mastery");
        testBook2.setStockQuantity(5);

        testBook3 = new Book();
        testBook3.setTitle("Design Patterns");
        testBook3.setAuthor("Erich Gamma");
        testBook3.setIsbn("978-0-20-163361-0");
        testBook3.setPrice(new BigDecimal("54.99"));
        testBook3.setDescription("Elements of Reusable Object-Oriented Software");
        testBook3.setStockQuantity(7);

        // Save to database
        entityManager.persist(testBook1);
        entityManager.persist(testBook2);
        entityManager.persist(testBook3);
        entityManager.flush();
    }

    @ParameterizedTest
    @CsvSource({
        "978-0-13-235088-4, true, Clean Code, Robert C. Martin",
        "999-9-99-999999-9, false, , "
    })
    void testFindByIsbn(String isbn, boolean shouldExist, String expectedTitle, String expectedAuthor) {
        // When
        Optional<Book> found = bookRepository.findByIsbn(isbn);

        // Then
        if (shouldExist) {
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo(expectedTitle);
            assertThat(found.get().getAuthor()).isEqualTo(expectedAuthor);
        } else {
            assertThat(found).isEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "martin, 1, Robert C. Martin",
        "ROBERT, 1, Robert C. Martin",
        "NonExistent Author, 0, "
    })
    void testFindByAuthorContainingIgnoreCase(String searchTerm, int expectedSize, String expectedAuthor) {
        // When
        List<Book> books = bookRepository.findByAuthorContainingIgnoreCase(searchTerm);

        // Then
        assertThat(books).hasSize(expectedSize);
        if (expectedSize > 0) {
            assertThat(books.get(0).getAuthor()).isEqualTo(expectedAuthor);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "code, 1, Clean Code",
        "PRAGMATIC, 1, The Pragmatic Programmer",
        "pattern, 1, Design Patterns"
    })
    void testFindByTitleContainingIgnoreCase(String searchTerm, int expectedSize, String expectedTitle) {
        // When
        List<Book> books = bookRepository.findByTitleContainingIgnoreCase(searchTerm);

        // Then
        assertThat(books).hasSize(expectedSize);
        if (expectedSize > 0) {
            assertThat(books.get(0).getTitle()).isEqualTo(expectedTitle);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "978-0-13-235088-4, true",
        "999-9-99-999999-9, false"
    })
    void testExistsByIsbn(String isbn, boolean expectedExists) {
        // When
        boolean exists = bookRepository.existsByIsbn(isbn);

        // Then
        assertThat(exists).isEqualTo(expectedExists);
    }

    @Test
    void testSaveBook_ShouldPersistBook() {
        // Given
        Book newBook = new Book();
        newBook.setTitle("Effective Java");
        newBook.setAuthor("Joshua Bloch");
        newBook.setIsbn("978-0-13-468599-1");
        newBook.setPrice(new BigDecimal("49.99"));
        newBook.setStockQuantity(15);

        // When
        Book saved = bookRepository.save(newBook);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(bookRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindAll_ShouldReturnAllBooks() {
        // When
        List<Book> books = bookRepository.findAll();

        // Then
        assertThat(books).hasSize(3);
    }

    @Test
    void testDeleteBook_ShouldRemoveBook() {
        // Given
        Long bookId = testBook1.getId();

        // When
        bookRepository.deleteById(bookId);

        // Then
        assertThat(bookRepository.findById(bookId)).isEmpty();
        assertThat(bookRepository.findAll()).hasSize(2);
    }

    @Test
    void testUpdateBook_ShouldModifyBook() {
        // Given
        Book book = bookRepository.findByIsbn("978-0-13-235088-4").orElseThrow();
        
        // When
        book.setPrice(new BigDecimal("45.99"));
        book.setStockQuantity(20);
        Book updated = bookRepository.save(book);

        // Then
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("45.99"));
        assertThat(updated.getStockQuantity()).isEqualTo(20);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }
}
