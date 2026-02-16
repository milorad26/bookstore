package com.bookstore.controller;

import com.bookstore.dto.CreateUserRequest;
import com.bookstore.dto.UpdateUserRequest;
import com.bookstore.dto.UserDTO;
import com.bookstore.dto.UserProfileDTO;
import com.bookstore.exception.AccessDeniedException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.User;
import com.bookstore.model.UserType;
import com.bookstore.security.AuthenticationHelper;
import com.bookstore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API for managing users")
public class UserController {

    private final UserService userService;
    private final AuthenticationHelper authenticationHelper;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with encrypted password")
    public ResponseEntity<UserDTO> createUser(
            HttpServletRequest request,
            @Valid @RequestBody CreateUserRequest createRequest) {
        
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Validate permissions based on current user type
        UserType currentUserType = currentUser.getUserType();
        UserType newUserType = createRequest.getUserType() != null ? createRequest.getUserType() : UserType.USER;
        
        // Regular users cannot create other users
        if (currentUserType == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Regular users cannot create other users.");
        }
        
        // Super users can only create regular users
        if (currentUserType == UserType.SUPER_USER && newUserType != UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Super users can only create regular users.");
        }
        
        User user = new User();
        user.setUsername(createRequest.getUsername());
        user.setPassword(createRequest.getPassword()); // Will be encrypted by service
        user.setFirstName(createRequest.getFirstName());
        user.setLastName(createRequest.getLastName());
        user.setEmail(createRequest.getEmail());
        user.setPhoneNumber(createRequest.getPhoneNumber());
        user.setAddress(createRequest.getAddress());
        user.setEnabled(true);
        user.setUserType(newUserType);
        
        User savedUser = userService.createUser(user);
        return new ResponseEntity<>(toDTO(savedUser), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the authenticated user's own profile with permissions and available endpoints")
    public ResponseEntity<UserProfileDTO> getCurrentUser(HttpServletRequest request) {
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User user = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        return ResponseEntity.ok(toProfileDTO(user));
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves all users (passwords excluded) - SUPER_USER and ADMIN only")
    public ResponseEntity<List<UserDTO>> getAllUsers(HttpServletRequest request) {
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Only SUPER_USER and ADMIN can list all users
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Regular users cannot view all users.");
        }
        
        List<UserDTO> users = userService.getAllUsers().stream()
            .map(this::toDTO)
            .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID - SUPER_USER and ADMIN only")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id, HttpServletRequest request) {
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Only SUPER_USER and ADMIN can view other users' profiles
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Regular users can only view their own profile via /api/users/me");
        }
        
        User user = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + id + ". Please check the ID and try again."));
        return ResponseEntity.ok(toDTO(user));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieves a user by their username - SUPER_USER and ADMIN only")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username, HttpServletRequest request) {
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Only SUPER_USER and ADMIN can view other users' profiles
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Regular users can only view their own profile via /api/users/me");
        }
        
        User targetUser = userService.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with username: '" + username + "'. Please verify the username and try again."));
        
        return ResponseEntity.ok(toDTO(targetUser));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates the authenticated user's own profile")
    public ResponseEntity<UserDTO> updateCurrentUser(
            HttpServletRequest httpRequest,
            @RequestBody UpdateUserRequest updateRequest) {
        
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(httpRequest);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Regular users cannot change their own userType
        if (updateRequest.getUserType() != null && 
            updateRequest.getUserType() != currentUser.getUserType()) {
            throw new AccessDeniedException(
                "Permission denied: You cannot change your own user type.");
        }
        
        User user = new User();
        user.setFirstName(updateRequest.getFirstName());
        user.setLastName(updateRequest.getLastName());
        user.setEmail(updateRequest.getEmail());
        user.setPhoneNumber(updateRequest.getPhoneNumber());
        user.setAddress(updateRequest.getAddress());
        user.setEnabled(updateRequest.getEnabled());
        user.setUserType(updateRequest.getUserType());
        
        User updatedUser = userService.updateUser(currentUserId, user);
        return ResponseEntity.ok(toDTO(updatedUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates user information (excluding username and password) - SUPER_USER and ADMIN only")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            @RequestBody UpdateUserRequest updateRequest) {
        
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(httpRequest);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Regular users should use /api/users/me to update their own profile
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Regular users should use /api/users/me to update their profile.");
        }
        
        // Get the user being edited
        User targetUser = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Cannot update user. User not found with ID: " + id));
        
        // Validate permissions based on current user type
        UserType currentUserType = currentUser.getUserType();
        UserType targetUserType = targetUser.getUserType();
        
        // Super users can only edit regular users (not admins or other super users)
        if (currentUserType == UserType.SUPER_USER) {
            if (targetUserType != UserType.USER) {
                throw new AccessDeniedException(
                    "Permission denied: Super users can only edit regular users.");
            }
            // Super users cannot change userType
            if (updateRequest.getUserType() != null && updateRequest.getUserType() != UserType.USER) {
                throw new AccessDeniedException(
                    "Permission denied: Super users cannot change user type.");
            }
        }
        
        // Admins can edit everyone - no additional checks needed
        
        User user = new User();
        user.setFirstName(updateRequest.getFirstName());
        user.setLastName(updateRequest.getLastName());
        user.setEmail(updateRequest.getEmail());
        user.setPhoneNumber(updateRequest.getPhoneNumber());
        user.setAddress(updateRequest.getAddress());
        user.setEnabled(updateRequest.getEnabled());
        user.setUserType(updateRequest.getUserType());
        
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(toDTO(updatedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by their ID - ADMIN only")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        // Get the current user who is making the request from JWT
        Long currentUserId = authenticationHelper.getUserIdFromRequest(httpRequest);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Only ADMIN can delete users (already enforced by SecurityConfig, but double-check here)
        if (currentUser.getUserType() != UserType.ADMIN) {
            throw new AccessDeniedException(
                "Permission denied: Only administrators can delete users.");
        }
        
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
        dto.setUserType(user.getUserType());
        return dto;
    }
    
    // Helper method to convert User entity to UserProfileDTO with permissions
    private UserProfileDTO toProfileDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setEnabled(user.getEnabled());
        dto.setUserType(user.getUserType());
        
        // Set permissions and available endpoints based on user type
        List<String> permissions = new ArrayList<>();
        List<String> endpoints = new ArrayList<>();
        
        switch (user.getUserType()) {
            case ADMIN:
                // ADMIN has all permissions
                permissions.add("manage_users");
                permissions.add("create_any_user");
                permissions.add("edit_any_user");
                permissions.add("delete_any_user");
                permissions.add("view_all_users");
                permissions.add("manage_orders");
                permissions.add("view_all_orders");
                permissions.add("confirm_orders");
                permissions.add("update_order_status");
                permissions.add("delete_orders");
                permissions.add("manage_books");
                permissions.add("create_books");
                permissions.add("edit_books");
                permissions.add("delete_books");
                
                // ADMIN endpoints
                endpoints.add("GET /api/users");
                endpoints.add("GET /api/users/{id}");
                endpoints.add("GET /api/users/username/{username}");
                endpoints.add("POST /api/users");
                endpoints.add("PUT /api/users/{id}");
                endpoints.add("DELETE /api/users/{id}");
                endpoints.add("GET /api/orders");
                endpoints.add("GET /api/orders/{id}");
                endpoints.add("GET /api/orders/user/{userId}");
                endpoints.add("POST /api/orders");
                endpoints.add("PUT /api/orders/confirm/{id}");
                endpoints.add("PUT /api/orders/cancel/{id}");
                endpoints.add("DELETE /api/orders/delete/{id}");
                endpoints.add("GET /api/books");
                endpoints.add("GET /api/books/{id}");
                endpoints.add("POST /api/books");
                endpoints.add("PUT /api/books/{id}");
                endpoints.add("DELETE /api/books/{id}");
                endpoints.add("GET /api/order-items/{id}");
                endpoints.add("GET /api/order-items/order/{orderId}");
                endpoints.add("GET /api/order-items/book/{bookId}");
                break;
                
            case SUPER_USER:
                // SUPER_USER has limited admin permissions
                permissions.add("manage_regular_users");
                permissions.add("create_regular_users");
                permissions.add("edit_regular_users");
                permissions.add("view_all_users");
                permissions.add("view_all_orders");
                permissions.add("confirm_orders");
                permissions.add("update_order_status");
                permissions.add("view_books");
                
                // SUPER_USER endpoints
                endpoints.add("GET /api/users");
                endpoints.add("GET /api/users/{id}");
                endpoints.add("GET /api/users/username/{username}");
                endpoints.add("POST /api/users");
                endpoints.add("PUT /api/users/{id}");
                endpoints.add("GET /api/orders");
                endpoints.add("GET /api/orders/{id}");
                endpoints.add("GET /api/orders/user/{userId}");
                endpoints.add("POST /api/orders");
                endpoints.add("PUT /api/orders/confirm/{id}");
                endpoints.add("PUT /api/orders/cancel/{id}");
                endpoints.add("GET /api/books");
                endpoints.add("GET /api/books/{id}");
                endpoints.add("GET /api/order-items/{id}");
                endpoints.add("GET /api/order-items/order/{orderId}");
                endpoints.add("GET /api/order-items/book/{bookId}");
                break;
                
            case USER:
                // Regular USER has basic permissions
                permissions.add("view_own_profile");
                permissions.add("edit_own_profile");
                permissions.add("view_own_orders");
                permissions.add("create_orders");
                permissions.add("cancel_own_orders");
                permissions.add("view_books");
                
                // Regular USER endpoints
                endpoints.add("GET /api/users/me");
                endpoints.add("PUT /api/users/me");
                endpoints.add("GET /api/orders/me");
                endpoints.add("POST /api/orders");
                endpoints.add("PUT /api/orders/cancel/{id}");
                endpoints.add("GET /api/books");
                endpoints.add("GET /api/books/{id}");
                break;
        }
        
        dto.setPermissions(permissions);
        dto.setAvailableEndpoints(endpoints);
        
        return dto;
    }
}
