package com.CSSEProject.SmartWasteManagement.user.service;

import com.CSSEProject.SmartWasteManagement.dto.LoginRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.RegisterRequestDto;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(RegisterRequestDto registerRequestDto) {
        if (userRepository.findByEmail(registerRequestDto.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User newUser = new User();
        newUser.setName(registerRequestDto.getName());
        newUser.setEmail(registerRequestDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        newUser.setAddress(registerRequestDto.getAddress());
        newUser.setPhone(registerRequestDto.getPhone());
        newUser.setRole(registerRequestDto.getRole());

        // Set resident-specific fields
        if (registerRequestDto.getRole() == UserRole.ROLE_RESIDENT) {
            newUser.setResidentId(generateResidentId());
            newUser.setAccountActivationDate(LocalDate.now());
        }

        return userRepository.save(newUser);
    }

    public User loginUser(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found with email: " + loginRequestDto.getEmail()));

        if (passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            return user;
        } else {
            throw new RuntimeException("Error: Invalid password.");
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User assignResidentId(Long userId, String residentId) {
        User user = getUserById(userId);
        if (user.getRole() != UserRole.ROLE_RESIDENT) {
            throw new RuntimeException("Only residents can be assigned resident IDs");
        }
        user.setResidentId(residentId);
        return userRepository.save(user);
    }

    private String generateResidentId() {
        return "RES" + System.currentTimeMillis(); // Simple ID generation
    }
}