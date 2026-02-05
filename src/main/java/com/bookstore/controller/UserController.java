package com.bookstore.controller;

import com.bookstore.dto.CreateUserRequest;
import com.bookstore.dto.UpdateUserRequest;
import com.bookstore.dto.UserDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.User;
import com.bookstore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API for managing users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with encrypted password")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // Will be encrypted by service
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setEnabled(true);
        
        User savedUser = userService.createUser(user);
        return new ResponseEntity<>(toDTO(savedUser), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves all users (passwords excluded)")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + id + ". Please check the ID and try again."));
        return ResponseEntity.ok(toDTO(user));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieves a user by their username")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with username: '" + username + "'. Please verify the username and try again."));
        return ResponseEntity.ok(toDTO(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates user information (excluding username and password)")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, 
                                               @Valid @RequestBody UpdateUserRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setEnabled(request.getEnabled());
        
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(toDTO(updatedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by their ID")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // Verify user exists before attempting deletion
        userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Cannot delete user. User not found with ID: " + id));
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Helper method to convert User entity to UserDTO (without password)
    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setEnabled(user.getEnabled());
        return dto;
    }
}
