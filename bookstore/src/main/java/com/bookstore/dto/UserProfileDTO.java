package com.bookstore.dto;

import com.bookstore.model.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private Boolean enabled;
    private UserType userType;
    
    // Permission-related fields
    private List<String> permissions;
    private List<String> availableEndpoints;
}
