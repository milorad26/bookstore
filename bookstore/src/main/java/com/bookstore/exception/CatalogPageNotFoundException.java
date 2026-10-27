package com.bookstore.exception;

public class CatalogPageNotFoundException extends RuntimeException {
    public CatalogPageNotFoundException(String message) {
        super(message);
    }
}
