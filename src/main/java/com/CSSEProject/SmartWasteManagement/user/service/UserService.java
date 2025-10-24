package com.CSSEProject.SmartWasteManagement.user.service;

import com.CSSEProject.SmartWasteManagement.dto.LoginRequestDto;
import com.CSSEProject.SmartWasteManagement.dto.RegisterRequestDto;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WasteBinRepository wasteBinRepository;


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

        // FIRST save the user to get an ID
        User savedUser = userRepository.save(newUser);

        // THEN create waste bin for resident
        if (registerRequestDto.getRole() == UserRole.ROLE_RESIDENT) {
            createDefaultWasteBin(savedUser);
        }

        return savedUser;
    }

    private void createDefaultWasteBin(User resident) {
        WasteBin defaultBin = new WasteBin();
        defaultBin.setBinId("BIN-" + resident.getResidentId());
        defaultBin.setLocation(resident.getAddress());
        defaultBin.setBinType(BinType.GENERAL_WASTE);
        defaultBin.setCapacity(120.0);
        defaultBin.setCurrentLevel(0.0);
        defaultBin.setStatus(BinStatus.ACTIVE);
        defaultBin.setResident(resident); // Now resident is a persistent entity
        defaultBin.setInstallationDate(LocalDate.now());

        wasteBinRepository.save(defaultBin);
    }

    public User loginUser(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found with email: " + loginRequestDto.getEmail()));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Error: Invalid password.");
        }



        return user;
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
        return "RES" + System.currentTimeMillis();
    }

    public User updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("User or user ID cannot be null");
        }

        // Check if user exists
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + user.getId()));

        // Update allowed fields (don't update password, role, or resident-specific fields directly)
        if (user.getName() != null) {
            existingUser.setName(user.getName());
        }
        if (user.getEmail() != null) {
            // Check if email is already taken by another user
            userRepository.findByEmail(user.getEmail())
                    .ifPresent(foundUser -> {
                        if (!foundUser.getId().equals(user.getId())) {
                            throw new RuntimeException("Email is already in use by another user");
                        }
                    });
            existingUser.setEmail(user.getEmail());
        }
        if (user.getAddress() != null) {
            existingUser.setAddress(user.getAddress());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }

        // Update pending charges and recycling credits (for residents)
        if (user.getPendingCharges() != null) {
            existingUser.setPendingCharges(user.getPendingCharges());
        }
        if (user.getRecyclingCredits() != null) {
            existingUser.setRecyclingCredits(user.getRecyclingCredits());
        }
        if (user.getTotalCharges() != null) {
            existingUser.setTotalCharges(user.getTotalCharges());
        }

        return userRepository.save(existingUser);
    }
}