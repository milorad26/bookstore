package com.bookstore.service;

import com.bookstore.dto.BookDTO;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import com.bookstore.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final MessageSource messageSource;

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
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

    public BookDTO getBookByIsbn(String isbn) {
        Locale locale = LocaleContextHolder.getLocale();
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("book.notfound.isbn", new Object[]{isbn}, locale)));
        return convertToDTO(book);
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
        dto.setStockQuantity(book.getStockQuantity());
        return dto;
    }

    private Book convertToEntity(BookDTO dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setPrice(dto.getPrice());
        book.setDescription(dto.getDescription());
        book.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        return book;
    }
}
