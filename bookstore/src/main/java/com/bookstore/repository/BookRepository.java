package com.bookstore.repository;

import com.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("select b from Book b where lower(b.isbn) like lower(concat('%', :isbn, '%'))")
    List<Book> findByIsbn(@Param("isbn") String isbn);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    List<Book> findByTitleContainingIgnoreCase(String title);

    Optional<Book> findByTitleAndAuthor(String title, String author);

    boolean existsByIsbn(String isbn);

    @Query("select b from Book b where b.stockQuantity <= :threshold order by b.stockQuantity asc")
    List<Book> findLowStockBooks(@Param("threshold") Integer threshold);
}
