package com.bookstore.controller;

import com.bookstore.dto.UpdateUserRequest;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.User;
import com.bookstore.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private User testUser;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setPassword("$2a$10$encoded.password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPhoneNumber("+1-555-0101");
        testUser.setAddress("123 Main St");
        testUser.setEnabled(true);

        updateRequest = new UpdateUserRequest();
        updateRequest.setFirstName("Jonathan");
        updateRequest.setLastName("Smith");
        updateRequest.setEmail("jonathan.smith@example.com");
        updateRequest.setPhoneNumber("+1-555-9999");
        updateRequest.setAddress("456 Oak Ave");
        updateRequest.setEnabled(true);
    }

    @Test
    void testUpdateUser_Success() throws Exception {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("johndoe"); // Username doesn't change
        updatedUser.setFirstName("Jonathan");
        updatedUser.setLastName("Smith");
        updatedUser.setEmail("jonathan.smith@example.com");
        updatedUser.setPhoneNumber("+1-555-9999");
        updatedUser.setAddress("456 Oak Ave");
        updatedUser.setEnabled(true);

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("johndoe")))
                .andExpect(jsonPath("$.firstName", is("Jonathan")))
                .andExpect(jsonPath("$.lastName", is("Smith")))
                .andExpect(jsonPath("$.email", is("jonathan.smith@example.com")))
                .andExpect(jsonPath("$.phoneNumber", is("+1-555-9999")))
                .andExpect(jsonPath("$.address", is("456 Oak Ave")))
                .andExpect(jsonPath("$.enabled", is(true)));

        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    void testUpdateUser_UserNotFound_ShouldReturn404() throws Exception {
        // Given
        when(userService.updateUser(eq(999L), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Cannot update user. User not found with ID: 999"));

        // When & Then
        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.title", is("Not Found")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("User not found")));

        verify(userService).updateUser(eq(999L), any(User.class));
    }

    @Test
    void testUpdateUser_EmailAlreadyExists_ShouldReturn400() throws Exception {
        // Given
        when(userService.updateUser(eq(1L), any(User.class)))
                .thenThrow(new IllegalArgumentException("Email address 'jonathan.smith@example.com' is already registered. Please use a different email."));

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email address")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already registered")));

        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    void testUpdateUser_InvalidEmail_ShouldReturn400() throws Exception {
        // Given
        updateRequest.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.title", is("Validation Failed")));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void testUpdateUser_MissingFirstName_ShouldReturn400() throws Exception {
        // Given
        updateRequest.setFirstName("");

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.title", is("Validation Failed")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("First name")));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void testUpdateUser_MissingLastName_ShouldReturn400() throws Exception {
        // Given
        updateRequest.setLastName("");

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.title", is("Validation Failed")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Last name")));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void testUpdateUser_WithDisabledStatus() throws Exception {
        // Given
        updateRequest.setEnabled(false);
        
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("johndoe");
        updatedUser.setFirstName("Jonathan");
        updatedUser.setLastName("Smith");
        updatedUser.setEmail("jonathan.smith@example.com");
        updatedUser.setEnabled(false);

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)));

        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    void testUpdateUser_WithMinimalFields() throws Exception {
        // Given
        UpdateUserRequest minimalRequest = new UpdateUserRequest();
        minimalRequest.setFirstName("John");
        minimalRequest.setLastName("Doe");
        minimalRequest.setEmail("john@example.com");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("johndoe");
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Doe");
        updatedUser.setEmail("john@example.com");
        updatedUser.setEnabled(true);

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));

        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    void testGetUserById_Success() throws Exception {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("johndoe")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));

        verify(userService).findById(1L);
    }

    @Test
    void testGetUserById_NotFound_ShouldReturn404() throws Exception {
        // Given
        when(userService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("User not found")));

        verify(userService).findById(999L);
    }

    @Test
    void testGetUserByUsername_Success() throws Exception {
        // Given
        when(userService.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/users/username/johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("johndoe")))
                .andExpect(jsonPath("$.firstName", is("John")));

        verify(userService).findByUsername("johndoe");
    }

    @Test
    void testGetUserByUsername_NotFound_ShouldReturn404() throws Exception {
        // Given
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/users/username/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("username")));

        verify(userService).findByUsername("nonexistent");
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userService).deleteUser(1L);

        // When & Then
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).findById(1L);
        verify(userService).deleteUser(1L);
    }

    @Test
    void testDeleteUser_NotFound_ShouldReturn404() throws Exception {
        // Given
        when(userService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot delete user")));

        verify(userService).findById(999L);
        verify(userService, never()).deleteUser(any());
    }
}
