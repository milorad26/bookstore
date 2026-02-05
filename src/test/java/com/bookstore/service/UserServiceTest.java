package com.bookstore.service;

import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setPassword("plainPassword123");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setEnabled(true);
    }

    @Test
    void testCreateUser_ShouldEncryptPassword() {
        // Given
        String plainPassword = "plainPassword123";
        String encodedPassword = "$2a$10$encoded.password.hash";
        
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(testUser);

        // Then
        verify(passwordEncoder).encode(plainPassword);
        verify(userRepository).save(any(User.class));
        assertThat(result).isNotNull();
    }

    @Test
    void testCreateUser_WithExistingUsername_ShouldThrowException() {
        // Given
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Username")
            .hasMessageContaining("already taken");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email")
            .hasMessageContaining("already registered");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdatePassword_ShouldEncryptNewPassword() {
        // Given
        Long userId = 1L;
        String newPassword = "newPassword456";
        String encodedPassword = "$2a$10$new.encoded.password";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updatePassword(userId, newPassword);

        // Then
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
        assertThat(result).isNotNull();
    }

    @Test
    void testUpdatePassword_WithNonExistentUser_ShouldThrowException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updatePassword(userId, "newPassword"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
        
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void testVerifyPassword_WithCorrectPassword_ShouldReturnTrue() {
        // Given
        String rawPassword = "plainPassword123";
        String encodedPassword = "$2a$10$encoded.password.hash";
        
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        // When
        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        // Then
        assertThat(result).isTrue();
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    void testVerifyPassword_WithIncorrectPassword_ShouldReturnFalse() {
        // Given
        String rawPassword = "wrongPassword";
        String encodedPassword = "$2a$10$encoded.password.hash";
        
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        // When
        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        // Then
        assertThat(result).isFalse();
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    void testFindByUsername_ShouldReturnUser() {
        // Given
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("johndoe");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("johndoe");
        verify(userRepository).findByUsername("johndoe");
    }

    @Test
    void testFindByUsername_WithNonExistentUser_ShouldReturnEmpty() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByEmail_ShouldReturnUser() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail("john.doe@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void testFindById_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetAllUsers_ShouldReturnUserList() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("janesmith");
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        
        List<User> users = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testUser, user2);
        verify(userRepository).findAll();
    }

    @Test
    void testDeleteUser_ShouldCallRepository() {
        // Given
        Long userId = 1L;
        doNothing().when(userRepository).deleteById(userId);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).deleteById(userId);
    }

    @Test
    void testCreateUser_WithNullEmail_ShouldNotCheckEmailExists() {
        // Given
        testUser.setEmail(null);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.createUser(testUser);

        // Then
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUser_ShouldUpdateAllFields() {
        // Given
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("johndoe");
        existingUser.setPassword("$2a$10$encoded.password");
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");
        existingUser.setEmail("john.doe@example.com");
        existingUser.setPhoneNumber("+1-555-0101");
        existingUser.setAddress("123 Main St");
        existingUser.setEnabled(true);

        User updatedUser = new User();
        updatedUser.setFirstName("Jonathan");
        updatedUser.setLastName("Smith");
        updatedUser.setEmail("jonathan.smith@example.com");
        updatedUser.setPhoneNumber("+1-555-9999");
        updatedUser.setAddress("456 Oak Ave");
        updatedUser.setEnabled(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("jonathan.smith@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = userService.updateUser(userId, updatedUser);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("johndoe"); // Username should not change
        assertThat(result.getPassword()).isEqualTo("$2a$10$encoded.password"); // Password should not change
    }

    @Test
    void testUpdateUser_WithNonExistentUser_ShouldThrowException() {
        // Given
        Long userId = 999L;
        User updatedUser = new User();
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Doe");
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userId, updatedUser))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Cannot update user");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_WithExistingEmail_ShouldThrowException() {
        // Given
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("john.doe@example.com");

        User updatedUser = new User();
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Doe");
        updatedUser.setEmail("taken@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userId, updatedUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email address")
            .hasMessageContaining("already registered");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_WithSameEmail_ShouldNotThrowException() {
        // Given
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("john.doe@example.com");
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");

        User updatedUser = new User();
        updatedUser.setFirstName("Jonathan");
        updatedUser.setLastName("Doe");
        updatedUser.setEmail("john.doe@example.com"); // Same email

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = userService.updateUser(userId, updatedUser);

        // Then
        verify(userRepository).save(any(User.class));
        verify(userRepository, never()).existsByEmail(anyString());
        assertThat(result).isNotNull();
    }

    @Test
    void testUpdateUser_WithNullEmail_ShouldUpdate() {
        // Given
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("john.doe@example.com");
        existingUser.setFirstName("John");

        User updatedUser = new User();
        updatedUser.setFirstName("Jonathan");
        updatedUser.setLastName("Doe");
        updatedUser.setEmail(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = userService.updateUser(userId, updatedUser);

        // Then
        verify(userRepository).save(any(User.class));
        assertThat(result).isNotNull();
    }

    @Test
    void testUpdateUser_WithEnabledStatus_ShouldUpdateStatus() {
        // Given
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEnabled(true);
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");

        User updatedUser = new User();
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Doe");
        updatedUser.setEnabled(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = userService.updateUser(userId, updatedUser);

        // Then
        verify(userRepository).save(any(User.class));
        assertThat(result).isNotNull();
    }

    @Test
    void testUpdateUser_WithNullEnabled_ShouldNotUpdateStatus() {
        // Given
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEnabled(true);
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");

        User updatedUser = new User();
        updatedUser.setFirstName("Jonathan");
        updatedUser.setLastName("Doe");
        updatedUser.setEnabled(null); // Should not change enabled status

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = userService.updateUser(userId, updatedUser);

        // Then
        verify(userRepository).save(any(User.class));
        assertThat(result).isNotNull();
    }
}
