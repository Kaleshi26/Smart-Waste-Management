// Testing AuthController REST endpoints with MockMvc standalone setup
package com.CSSEProject.SmartWasteManagement.auth.controller;

import com.CSSEProject.SmartWasteManagement.controller.AuthController;
import com.CSSEProject.SmartWasteManagement.dto.RegisterRequestDto;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController using MockMvc in standalone mode.
 * Tests REST endpoints without full Spring context for fast execution.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerUser_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setRole(UserRole.ROLE_RESIDENT);

        when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.name").value("John Doe"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));
    }

    // ---------------------- NEWLY ADDED TESTS BELOW ----------------------

    @Test
    void registerUser_ShouldReturn400_WhenRequestIsNull() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenNameIsMissing() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenEmailIsMissing() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenPasswordIsMissing() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenAddressIsMissing() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenPhoneIsMissing() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenRoleIsMissing() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenEmailFormatIsInvalid() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("invalid-email");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenPasswordIsTooShort() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenServiceThrowsException() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        when(userService.registerUser(any(RegisterRequestDto.class))).thenThrow(new RuntimeException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void registerUser_ShouldReturn200_WhenStaffUserRegistration() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("Jane Staff");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("456 Oak St");
        registerRequest.setPhone("987-654-3210");
        registerRequest.setRole(UserRole.ROLE_STAFF);

        User mockUser = new User();
        mockUser.setId(2L);
        mockUser.setName("Jane Staff");
        mockUser.setEmail("jane@example.com");
        mockUser.setRole(UserRole.ROLE_STAFF);

        when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.name").value("Jane Staff"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_STAFF"));
    }

    @Test
    void registerUser_ShouldReturn200_WhenAdminUserRegistration() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("Admin User");
        registerRequest.setEmail("admin@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("789 Pine St");
        registerRequest.setPhone("555-123-4567");
        registerRequest.setRole(UserRole.ROLE_ADMIN);

        User mockUser = new User();
        mockUser.setId(3L);
        mockUser.setName("Admin User");
        mockUser.setEmail("admin@example.com");
        mockUser.setRole(UserRole.ROLE_ADMIN);

        when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.name").value("Admin User"))
                .andExpect(jsonPath("$.user.email").value("admin@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"));
    }

    @Test
    void registerUser_ShouldReturn200_WhenValidJsonWithSpecialCharacters() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("José María O'Connor");
        registerRequest.setEmail("jose@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Calle Principal, Ciudad");
        registerRequest.setPhone("+1-555-123-4567");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        User mockUser = new User();
        mockUser.setId(4L);
        mockUser.setName("José María O'Connor");
        mockUser.setEmail("jose@example.com");
        mockUser.setRole(UserRole.ROLE_RESIDENT);

        when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.name").value("José María O'Connor"))
                .andExpect(jsonPath("$.user.email").value("jose@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_RESIDENT"));
    }

    @Test
    void registerUser_ShouldReturn200_WhenValidJsonWithLongNames() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Michael Christopher Alexander Smith-Johnson-Williams");
        registerRequest.setEmail("longname@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Very Long Street Name That Goes On And On");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        User mockUser = new User();
        mockUser.setId(5L);
        mockUser.setName("John Michael Christopher Alexander Smith-Johnson-Williams");
        mockUser.setEmail("longname@example.com");
        mockUser.setRole(UserRole.ROLE_RESIDENT);

        when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.name").value("John Michael Christopher Alexander Smith-Johnson-Williams"))
                .andExpect(jsonPath("$.user.email").value("longname@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_RESIDENT"));
    }

    @Test
    void registerUser_ShouldReturn400_WhenInvalidJsonFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenEmptyJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenNullValuesInJson() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName(null);
        registerRequest.setEmail(null);
        registerRequest.setPassword(null);
        registerRequest.setAddress(null);
        registerRequest.setPhone(null);
        registerRequest.setRole(null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenEmptyStringsInJson() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("");
        registerRequest.setEmail("");
        registerRequest.setPassword("");
        registerRequest.setAddress("");
        registerRequest.setPhone("");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenWhitespaceOnlyStringsInJson() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("   ");
        registerRequest.setEmail("   ");
        registerRequest.setPassword("   ");
        registerRequest.setAddress("   ");
        registerRequest.setPhone("   ");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ShouldReturn400_WhenInvalidContentType() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void registerUser_ShouldReturn200_WhenValidJsonWithDifferentPhoneFormats() throws Exception {
        // Arrange
        String[] phoneFormats = {
            "123-456-7890",
            "(123) 456-7890",
            "+1-123-456-7890",
            "123.456.7890",
            "1234567890"
        };

        for (int i = 0; i < phoneFormats.length; i++) {
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setName("John Doe");
            registerRequest.setEmail("john" + i + "@example.com");
            registerRequest.setPassword("password123");
            registerRequest.setAddress("123 Main St");
            registerRequest.setPhone(phoneFormats[i]);
            registerRequest.setRole(UserRole.ROLE_RESIDENT);

            User mockUser = new User();
            mockUser.setId((long) (i + 1));
            mockUser.setName("John Doe");
            mockUser.setEmail("john" + i + "@example.com");
            mockUser.setRole(UserRole.ROLE_RESIDENT);

            when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.user.name").value("John Doe"))
                    .andExpect(jsonPath("$.user.email").value("john" + i + "@example.com"))
                    .andExpect(jsonPath("$.user.role").value("ROLE_RESIDENT"));
        }
    }

    @Test
    void registerUser_ShouldReturn200_WhenValidJsonWithDifferentEmailFormats() throws Exception {
        // Arrange
        String[] emailFormats = {
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "user123@test-domain.com"
        };

        for (int i = 0; i < emailFormats.length; i++) {
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setName("John Doe");
            registerRequest.setEmail(emailFormats[i]);
            registerRequest.setPassword("password123");
            registerRequest.setAddress("123 Main St");
            registerRequest.setPhone("123-456-7890");
            registerRequest.setRole(UserRole.ROLE_RESIDENT);

            User mockUser = new User();
            mockUser.setId((long) (i + 1));
            mockUser.setName("John Doe");
            mockUser.setEmail(emailFormats[i]);
            mockUser.setRole(UserRole.ROLE_RESIDENT);

            when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.user.name").value("John Doe"))
                    .andExpect(jsonPath("$.user.email").value(emailFormats[i]))
                    .andExpect(jsonPath("$.user.role").value("ROLE_RESIDENT"));
        }
    }

    @Test
    void registerUser_ShouldReturn200_WhenValidJsonWithComplexPassword() throws Exception {
        // Arrange
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("ComplexP@ssw0rd123!");
        registerRequest.setAddress("123 Main St");
        registerRequest.setPhone("123-456-7890");
        registerRequest.setRole(UserRole.ROLE_RESIDENT);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setRole(UserRole.ROLE_RESIDENT);

        when(userService.registerUser(any(RegisterRequestDto.class))).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.name").value("John Doe"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_RESIDENT"));
    }
}
