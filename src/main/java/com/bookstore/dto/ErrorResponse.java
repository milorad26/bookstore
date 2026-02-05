package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int code;
    private String message;
    private String title;
    private LocalDateTime timestamp;
    
    public ErrorResponse(int code, String message, String title) {
        this.code = code;
        this.message = message;
        this.title = title;
        this.timestamp = LocalDateTime.now();
    }
}
