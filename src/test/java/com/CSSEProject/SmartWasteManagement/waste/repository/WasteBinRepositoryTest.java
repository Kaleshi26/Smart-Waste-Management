// Testing WasteBinRepository with @DataJpaTest and H2 in-memory database
package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WasteBinRepository using @DataJpaTest with H2 database.
 * Tests JPA repository methods with actual database operations in test environment.
 */
@DataJpaTest
class WasteBinRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WasteBinRepository wasteBinRepository;

    private User testResident;
    private WasteBin testBin;

    @BeforeEach
    void setUp() {
        // Arrange - Setup test data
        testResident = new User();
        testResident.setName("Test Resident");
        testResident.setEmail("test@example.com");
        testResident.setRole(UserRole.ROLE_RESIDENT);
        testResident = entityManager.persistAndFlush(testResident);

        testBin = new WasteBin();
        testBin.setBinId("TEST-BIN-001");
        testBin.setLocation("Test Location");
        testBin.setBinType(BinType.GENERAL_WASTE);
        testBin.setCapacity(120.0);
        testBin.setCurrentLevel(50.0);
        testBin.setStatus(BinStatus.ACTIVE);
        testBin.setResident(testResident);
        testBin.setInstallationDate(LocalDate.now());
        testBin = entityManager.persistAndFlush(testBin);
    }

    @Test
    void findByBinId_ShouldReturnBin_WhenBinExists() {
        // Act
        Optional<WasteBin> result = wasteBinRepository.findById("TEST-BIN-001");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("TEST-BIN-001", result.get().getBinId());
        assertEquals("Test Location", result.get().getLocation());
        assertEquals(BinType.GENERAL_WASTE, result.get().getBinType());
        assertEquals(BinStatus.ACTIVE, result.get().getStatus());
    }

    @Test
    void findByResidentId_ShouldReturnBins_WhenResidentHasBins() {
        // Act
        List<WasteBin> result = wasteBinRepository.findByResidentId(testResident.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TEST-BIN-001", result.get(0).getBinId());
        assertEquals(testResident.getId(), result.get(0).getResident().getId());
    }

    @Test
    void findByStatus_ShouldReturnBins_WhenBinsWithStatusExist() {
        // Act
        List<WasteBin> result = wasteBinRepository.findByStatus(BinStatus.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BinStatus.ACTIVE, result.get(0).getStatus());
        assertEquals("TEST-BIN-001", result.get(0).getBinId());
    }

    @Test
    void countByStatus_ShouldReturnCorrectCount_WhenBinsExist() {
        // Act
        long count = wasteBinRepository.countByStatus(BinStatus.ACTIVE);

        // Assert
        assertEquals(1, count);
    }
}
