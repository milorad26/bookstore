package com.bookstore.service;

import com.bookstore.dto.BookDTO;
import com.bookstore.dto.BookPageDTO;
import com.bookstore.exception.CatalogPageNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.bookstore.exception.ResourceNotFoundException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final MessageSource messageSource;

    @Value("${catalog.page-size}")
    private int catalogPageSize;

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public BookPageDTO getCatalogPage(int page, Integer requestedSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be at least 1");
        }
        int size = requestedSize == null ? catalogPageSize : requestedSize;
        if (size < 1 || size > catalogPageSize) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and the configured maximum of "
                            + catalogPageSize + " books");
        }

        Page<Book> bookPage = bookRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by("title").ascending().and(Sort.by("id").ascending())));
        if (bookPage.isEmpty() || page > bookPage.getTotalPages()) {
            throw new CatalogPageNotFoundException(
                    "Catalog page " + page + " does not exist. Available pages: " + bookPage.getTotalPages());
        }

        return new BookPageDTO(
                bookPage.getContent().stream().map(this::convertToDTO).toList(),
                page,
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages());
    }

    public List<BookDTO> getLowStockBooks(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Stock threshold cannot be negative");
        }
        return bookRepository.findLowStockBooks(threshold).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public BookDTO getBookById(Long id) {
        Locale locale = LocaleContextHolder.getLocale();
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("book.notfound.id", new Object[]{id}, locale)));
        return convertToDTO(book);
    }

    public List<BookDTO> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<BookDTO> searchByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<BookDTO> searchByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public BookDTO createBook(BookDTO bookDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        if (bookRepository.existsByIsbn(bookDTO.getIsbn())) {
            String message = messageSource.getMessage("book.isbn.exists", 
                new Object[]{bookDTO.getIsbn()}, locale);
            throw new IllegalArgumentException(message);
        }
        Book book = convertToEntity(bookDTO);
        Book savedBook = bookRepository.save(book);
        return convertToDTO(savedBook);
    }

    @Transactional
    public BookDTO updateBook(Long id, BookDTO bookDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("book.notfound.id", new Object[]{id}, locale)));

        // Check if ISBN is being changed and if new ISBN already exists
        if (!existingBook.getIsbn().equals(bookDTO.getIsbn()) 
            && bookRepository.existsByIsbn(bookDTO.getIsbn())) {
            String message = messageSource.getMessage("book.isbn.exists", 
                new Object[]{bookDTO.getIsbn()}, locale);
            throw new IllegalArgumentException(message);
        }

        existingBook.setTitle(bookDTO.getTitle());
        existingBook.setAuthor(bookDTO.getAuthor());
        existingBook.setIsbn(bookDTO.getIsbn());
        existingBook.setPrice(bookDTO.getPrice());
        existingBook.setDescription(bookDTO.getDescription());
        existingBook.setCategory(bookDTO.getCategory());
        if (bookDTO.getStockQuantity() != null) {
            existingBook.setStockQuantity(bookDTO.getStockQuantity());
        }

        Book updatedBook = bookRepository.save(existingBook);
        return convertToDTO(updatedBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        Locale locale = LocaleContextHolder.getLocale();
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                messageSource.getMessage("book.notfound.id", new Object[]{id}, locale));
        }
        bookRepository.deleteById(id);
    }

    private BookDTO convertToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setIsbn(book.getIsbn());
        dto.setPrice(book.getPrice());
        dto.setDescription(book.getDescription());
        dto.setCategory(book.getCategory());
        dto.setStockQuantity(book.getStockQuantity());
        dto.setAverageRating(book.getAverageRating());
        dto.setReviewCount(book.getReviewCount());
        return dto;
    }

    private Book convertToEntity(BookDTO dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setPrice(dto.getPrice());
        book.setDescription(dto.getDescription());
        book.setCategory(dto.getCategory());
        book.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        return book;
    }
}
