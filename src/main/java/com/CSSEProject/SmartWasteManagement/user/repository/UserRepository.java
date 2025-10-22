package com.CSSEProject.SmartWasteManagement.user.repository;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(UserRole role);
    boolean existsByEmail(String email);
    List<User> findByResidentIdIsNotNull(); // Only residents have residentId
    Optional<User> findByResidentId(String residentId);
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.id != :userId")
    Optional<User> findByEmailAndIdNot(@Param("email") String email, @Param("userId") Long userId);
}