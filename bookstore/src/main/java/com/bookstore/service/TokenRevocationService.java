package com.bookstore.service;

import com.bookstore.model.RevokedToken;
import com.bookstore.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenHash) {
        return revokedTokenRepository.existsByTokenHashAndExpiresAtGreaterThan(
                tokenHash, System.currentTimeMillis());
    }

    @Transactional
    public void revoke(String tokenHash, long expiresAt) {
        revokedTokenRepository.save(new RevokedToken(tokenHash, expiresAt));
        revokedTokenRepository.deleteByExpiresAtLessThanEqual(System.currentTimeMillis());
    }
}
