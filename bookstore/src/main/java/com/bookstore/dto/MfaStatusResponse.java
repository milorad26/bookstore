package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaStatusResponse {
    private boolean mfaEnabled;
    private String message;
    
    public MfaStatusResponse(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }
}
