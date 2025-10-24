// Testing UserService business logic with mocked dependencies
package com.CSSEProject.SmartWasteManagement.user.service;

import com.CSSEProject.SmartWasteManagement.dto.RegisterRequestDto;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService business logic.
 * Tests user registration and authentication with mocked repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WasteBinRepository wasteBinRepository;

    @InjectMocks
    private UserService userService;

    private RegisterRequestDto registerRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // Arrange - Setup test data
        registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setRole(UserRole.ROLE_RESIDENT);
    }

    @Test
    void registerUser_ShouldReturnUser_WhenValidRequest() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        User result = userService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(UserRole.ROLE_RESIDENT, result.getRole());
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailExists() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registerRequest);
        });

        assertEquals("Error: Email is already in use!", exception.getMessage());
        verify(userRepository).findByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
}
