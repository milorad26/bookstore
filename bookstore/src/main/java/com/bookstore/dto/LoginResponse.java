package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    
    @Builder.Default
    private String type = "Bearer";
    
    @Builder.Default
    private boolean mfaRequired = false;
    
    private String mfaToken; // Temporary token for MFA verification
    
    @Builder.Default
    private boolean shouldPromptMfa = false; // Suggest enabling MFA if not set up
}
