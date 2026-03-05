package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.user.entity.UserRole;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinStatus;
import com.CSSEProject.SmartWasteManagement.waste.entity.BinType;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Integration tests for WasteBinRepository using @DataJpaTest with H2 database and TestNG.
 */
@DataJpaTest
public class WasteBinRepositoryTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WasteBinRepository wasteBinRepository;

    private User testResident;
    private WasteBin testBin;

    @BeforeMethod
    public void setUp() {
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
    public void findByBinId_ShouldReturnBin_WhenBinExists() {
        // Act
        Optional<WasteBin> result = wasteBinRepository.findById("TEST-BIN-001");

        // Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getBinId(), "TEST-BIN-001");
        Assert.assertEquals(result.get().getLocation(), "Test Location");
        Assert.assertEquals(result.get().getBinType(), BinType.GENERAL_WASTE);
        Assert.assertEquals(result.get().getStatus(), BinStatus.ACTIVE);
    }

    @Test
    public void findByResidentId_ShouldReturnBins_WhenResidentHasBins() {
        // Act
        List<WasteBin> result = wasteBinRepository.findByResidentId(testResident.getId());

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getBinId(), "TEST-BIN-001");
        Assert.assertEquals(result.get(0).getResident().getId(), testResident.getId());
    }

    @Test
    public void findByStatus_ShouldReturnBins_WhenBinsWithStatusExist() {
        // Act
        List<WasteBin> result = wasteBinRepository.findByStatus(BinStatus.ACTIVE);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getStatus(), BinStatus.ACTIVE);
        Assert.assertEquals(result.get(0).getBinId(), "TEST-BIN-001");
    }

    @Test
    public void countByStatus_ShouldReturnCorrectCount_WhenBinsExist() {
        // Act
        long count = wasteBinRepository.countByStatus(BinStatus.ACTIVE);

        // Assert
        Assert.assertEquals(count, 1L);
    }
}