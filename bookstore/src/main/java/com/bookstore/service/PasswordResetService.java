package com.bookstore.service;

import com.bookstore.model.User;
import com.bookstore.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public String createToken(User user) {
        return jwtUtil.generatePasswordResetToken(user.getId(), user.getPassword());
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Long userId = jwtUtil.extractPasswordResetUserId(token);
        if (userId == null) {
            return false;
        }

        User user = userService.findById(userId).orElse(null);
        if (user == null || !userId.equals(jwtUtil.validatePasswordResetToken(token, user.getPassword()))) {
            return false;
        }

        userService.resetPassword(userId, newPassword);
        return true;
    }
}
