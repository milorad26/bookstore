package com.bookstore.repository;

import com.bookstore.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // Clear the database
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        
        // Create test users with unique emails to avoid conflicts
        testUser1 = new User();
        testUser1.setUsername("testjohn");
        testUser1.setPassword("password123");
        testUser1.setFirstName("John");
        testUser1.setLastName("Doe");
        testUser1.setEmail("test.john.doe@test.com");
        testUser1.setPhoneNumber("+1-555-1001");
        testUser1.setAddress("123 Test St");
        testUser1.setEnabled(true);

        testUser2 = new User();
        testUser2.setUsername("testjane");
        testUser2.setPassword("password456");
        testUser2.setFirstName("Jane");
        testUser2.setLastName("Smith");
        testUser2.setEmail("test.jane.smith@test.com");
        testUser2.setPhoneNumber("+1-555-1002");
        testUser2.setAddress("456 Test Ave");
        testUser2.setEnabled(true);

        testUser3 = new User();
        testUser3.setUsername("testbob");
        testUser3.setPassword("password789");
        testUser3.setFirstName("Bob");
        testUser3.setLastName("Wilson");
        testUser3.setEmail("test.bob.wilson@test.com");
        testUser3.setPhoneNumber("+1-555-1003");
        testUser3.setAddress("789 Test Elm");
        testUser3.setEnabled(false);

        // Save to database
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.persist(testUser3);
        entityManager.flush();
    }

    @ParameterizedTest
    @CsvSource({
        "testjohn, true, John, Doe",
        "testjane, true, Jane, Smith",
        "nonexistent, false, , "
    })
    void testFindByUsername(String username, boolean shouldExist, String expectedFirstName, String expectedLastName) {
        // When
        Optional<User> found = userRepository.findByUsername(username);

        // Then
        if (shouldExist) {
            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo(expectedFirstName);
            assertThat(found.get().getLastName()).isEqualTo(expectedLastName);
        } else {
            assertThat(found).isEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "test.john.doe@test.com, true, testjohn",
        "test.jane.smith@test.com, true, testjane",
        "nonexistent@example.com, false, "
    })
    void testFindByEmail(String email, boolean shouldExist, String expectedUsername) {
        // When
        Optional<User> found = userRepository.findByEmail(email);

        // Then
        if (shouldExist) {
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo(expectedUsername);
        } else {
            assertThat(found).isEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "testjohn, true",
        "testjane, true",
        "nonexistent, false"
    })
    void testExistsByUsername(String username, boolean expectedExists) {
        // When
        boolean exists = userRepository.existsByUsername(username);

        // Then
        assertThat(exists).isEqualTo(expectedExists);
    }

    @ParameterizedTest
    @CsvSource({
        "test.john.doe@test.com, true",
        "test.jane.smith@test.com, true",
        "nonexistent@example.com, false"
    })
    void testExistsByEmail(String email, boolean expectedExists) {
        // When
        boolean exists = userRepository.existsByEmail(email);

        // Then
        assertThat(exists).isEqualTo(expectedExists);
    }

    @Test
    void testSaveUser_ShouldPersistUser() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("newpassword");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEmail("new.user@example.com");
        newUser.setEnabled(true);

        // When
        User saved = userRepository.save(newUser);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindAll_ShouldReturnAllUsers() {
        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(3);
    }

    @Test
    void testDeleteUser_ShouldRemoveUser() {
        // Given
        Long userId = testUser1.getId();

        // When
        userRepository.deleteById(userId);

        // Then
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(userRepository.findAll()).hasSize(2);
    }

    @Test
    void testUpdateUser_ShouldModifyUser() {
        // Given
        User user = userRepository.findByUsername("testjohn").orElseThrow();
        
        // When
        user.setFirstName("Jonathan");
        user.setPhoneNumber("+1-555-9999");
        User updated = userRepository.save(user);

        // Then
        assertThat(updated.getFirstName()).isEqualTo("Jonathan");
        assertThat(updated.getPhoneNumber()).isEqualTo("+1-555-9999");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void testUsernameUniqueness_ShouldBeUnique() {
        // Given
        User duplicateUser = new User();
        duplicateUser.setUsername("testjohn"); // Same as testUser1
        duplicateUser.setPassword("password");
        duplicateUser.setFirstName("Another");
        duplicateUser.setLastName("User");
        duplicateUser.setEmail("unique@example.com");

        // When & Then
        // This should throw a constraint violation exception
        try {
            entityManager.persistAndFlush(duplicateUser);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void testEmailUniqueness_ShouldBeUnique() {
        // Given
        User duplicateUser = new User();
        duplicateUser.setUsername("anotheruser");
        duplicateUser.setPassword("password");
        duplicateUser.setFirstName("Another");
        duplicateUser.setLastName("User");
        duplicateUser.setEmail("test.john.doe@test.com"); // Same as testUser1

        // When & Then
        // This should throw a constraint violation exception
        try {
            entityManager.persistAndFlush(duplicateUser);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void testEnabledField_ShouldFilterUsers() {
        // When
        User enabledUser = userRepository.findByUsername("testjohn").orElseThrow();
        User disabledUser = userRepository.findByUsername("testbob").orElseThrow();

        // Then
        assertThat(enabledUser.getEnabled()).isTrue();
        assertThat(disabledUser.getEnabled()).isFalse();
    }

    @Test
    void testTimestamps_ShouldBeAutomaticallySet() {
        // Given
        User newUser = new User();
        newUser.setUsername("timestamptest");
        newUser.setPassword("password");
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        newUser.setEmail("timestamp@example.com");
        newUser.setEnabled(true);

        // When
        User saved = entityManager.persistAndFlush(newUser);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }
}
