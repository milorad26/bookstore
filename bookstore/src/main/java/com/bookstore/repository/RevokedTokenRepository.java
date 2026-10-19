package com.bookstore.repository;

import com.bookstore.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    boolean existsByTokenHashAndExpiresAtGreaterThan(String tokenHash, long currentTime);

    long deleteByExpiresAtLessThanEqual(long currentTime);
}
