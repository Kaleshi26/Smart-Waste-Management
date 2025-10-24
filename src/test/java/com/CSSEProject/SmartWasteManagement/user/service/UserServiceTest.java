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

    // ---------------------- NEWLY ADDED TESTS BELOW ----------------------

    @Test
    void registerUser_ShouldHandleNullRequest_WhenRequestIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(null);
        });
    }

    @Test
    void registerUser_ShouldHandleNullName_WhenNameIsNull() {
        // Arrange
        registerRequest.setName(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleEmptyName_WhenNameIsEmpty() {
        // Arrange
        registerRequest.setName("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleNullEmail_WhenEmailIsNull() {
        // Arrange
        registerRequest.setEmail(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleEmptyEmail_WhenEmailIsEmpty() {
        // Arrange
        registerRequest.setEmail("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleInvalidEmail_WhenEmailFormatIsInvalid() {
        // Arrange
        registerRequest.setEmail("invalid-email");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleNullPassword_WhenPasswordIsNull() {
        // Arrange
        registerRequest.setPassword(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleEmptyPassword_WhenPasswordIsEmpty() {
        // Arrange
        registerRequest.setPassword("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleShortPassword_WhenPasswordIsTooShort() {
        // Arrange
        registerRequest.setPassword("123");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleNullAddress_WhenAddressIsNull() {
        // Arrange
        registerRequest.setAddress(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleEmptyAddress_WhenAddressIsEmpty() {
        // Arrange
        registerRequest.setAddress("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleNullPhone_WhenPhoneIsNull() {
        // Arrange
        registerRequest.setPhone(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleEmptyPhone_WhenPhoneIsEmpty() {
        // Arrange
        registerRequest.setPhone("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleNullRole_WhenRoleIsNull() {
        // Arrange
        registerRequest.setRole(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registerRequest);
        });
    }

    @Test
    void registerUser_ShouldHandleRepositoryExceptions_WhenUserRepositoryFails() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registerRequest);
        });
        
        verify(userRepository).findByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandlePasswordEncoderExceptions_WhenPasswordEncoderFails() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenThrow(new RuntimeException("Password encoding failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registerRequest);
        });
        
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleSaveExceptions_WhenSaveFails() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database save failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registerRequest);
        });
        
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldRegisterStaffUser_WhenRoleIsStaff() {
        // Arrange
        registerRequest.setRole(UserRole.ROLE_STAFF);
        mockUser.setRole(UserRole.ROLE_STAFF);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        User result = userService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(UserRole.ROLE_STAFF, result.getRole());
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldRegisterAdminUser_WhenRoleIsAdmin() {
        // Arrange
        registerRequest.setRole(UserRole.ROLE_ADMIN);
        mockUser.setRole(UserRole.ROLE_ADMIN);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        User result = userService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(UserRole.ROLE_ADMIN, result.getRole());
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleDifferentEmailFormats_WhenValidEmailFormatsProvided() {
        // Arrange
        String[] validEmails = {
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "user123@test-domain.com"
        };

        for (String email : validEmails) {
            registerRequest.setEmail(email);
            mockUser.setEmail(email);
            
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // Act
            User result = userService.registerUser(registerRequest);

            // Assert
            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(userRepository).findByEmail(email);
        }

        verify(passwordEncoder, times(validEmails.length)).encode("password123");
        verify(userRepository, times(validEmails.length)).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleDifferentPasswordLengths_WhenValidPasswordLengthsProvided() {
        // Arrange
        String[] validPasswords = {
            "password123",
            "verylongpassword123456",
            "pass123",
            "complexP@ssw0rd!"
        };

        for (String password : validPasswords) {
            registerRequest.setPassword(password);
            
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // Act
            User result = userService.registerUser(registerRequest);

            // Assert
            assertNotNull(result);
            assertEquals("John Doe", result.getName());
            verify(passwordEncoder).encode(password);
        }

        verify(userRepository, times(validPasswords.length)).findByEmail("john@example.com");
        verify(userRepository, times(validPasswords.length)).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleDifferentAddressFormats_WhenValidAddressFormatsProvided() {
        // Arrange
        String[] validAddresses = {
            "123 Main St, City, State",
            "456 Oak Avenue, Apartment 2B, New York, NY 10001",
            "789 Pine Street, Unit 5, Los Angeles, CA",
            "321 Elm Road, Suite 100, Chicago, IL 60601"
        };

        for (String address : validAddresses) {
            registerRequest.setAddress(address);
            
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // Act
            User result = userService.registerUser(registerRequest);

            // Assert
            assertNotNull(result);
            assertEquals("John Doe", result.getName());
        }

        verify(userRepository, times(validAddresses.length)).findByEmail("john@example.com");
        verify(passwordEncoder, times(validAddresses.length)).encode("password123");
        verify(userRepository, times(validAddresses.length)).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleDifferentPhoneFormats_WhenValidPhoneFormatsProvided() {
        // Arrange
        String[] validPhones = {
            "123-456-7890",
            "(123) 456-7890",
            "+1-123-456-7890",
            "123.456.7890",
            "1234567890"
        };

        for (String phone : validPhones) {
            registerRequest.setPhone(phone);
            
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // Act
            User result = userService.registerUser(registerRequest);

            // Assert
            assertNotNull(result);
            assertEquals("John Doe", result.getName());
        }

        verify(userRepository, times(validPhones.length)).findByEmail("john@example.com");
        verify(passwordEncoder, times(validPhones.length)).encode("password123");
        verify(userRepository, times(validPhones.length)).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleSpecialCharactersInName_WhenNameContainsSpecialCharacters() {
        // Arrange
        registerRequest.setName("John O'Connor-Smith");
        mockUser.setName("John O'Connor-Smith");
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        User result = userService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("John O'Connor-Smith", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(UserRole.ROLE_RESIDENT, result.getRole());
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleUnicodeCharactersInName_WhenNameContainsUnicodeCharacters() {
        // Arrange
        registerRequest.setName("José María");
        mockUser.setName("José María");
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        User result = userService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("José María", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(UserRole.ROLE_RESIDENT, result.getRole());
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleLongNames_WhenNameIsVeryLong() {
        // Arrange
        String longName = "John Michael Christopher Alexander Smith-Johnson-Williams-Brown-Davis";
        registerRequest.setName(longName);
        mockUser.setName(longName);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        User result = userService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals(longName, result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(UserRole.ROLE_RESIDENT, result.getRole());
        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandleLongAddresses_WhenAddressIsVeryLong() {
        // Arrange
        String longAddress = "123 Very Long Street Name That Goes On And On And On, Apartment 12345, Suite 67890, Building A, Floor 10, City, State, Country, Postal Code 12345-6789";
        registerRequest.setAddress(longAddress);
        
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
}
