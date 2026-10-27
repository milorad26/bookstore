package com.bookstore.repository;

import com.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("select b from Book b where lower(b.isbn) like lower(concat('%', :isbn, '%'))")
    List<Book> findByIsbn(@Param("isbn") String isbn);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    List<Book> findByTitleContainingIgnoreCase(String title);

    Optional<Book> findByTitleAndAuthor(String title, String author);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.title = :title and b.author = :author")
    Optional<Book> findByTitleAndAuthorForUpdate(@Param("title") String title,
                                                 @Param("author") String author);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    boolean existsByIsbn(String isbn);

    @Query("select b from Book b where b.stockQuantity <= :threshold order by b.stockQuantity asc")
    List<Book> findLowStockBooks(@Param("threshold") Integer threshold);
}
