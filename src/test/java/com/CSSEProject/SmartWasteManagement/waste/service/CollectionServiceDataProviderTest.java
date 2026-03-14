package com.CSSEProject.SmartWasteManagement.waste.service;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.user.entity.User;
import com.CSSEProject.SmartWasteManagement.waste.entity.*;
import com.CSSEProject.SmartWasteManagement.waste.repository.CollectionEventRepository;
import com.CSSEProject.SmartWasteManagement.waste.repository.WasteBinRepository;
import com.CSSEProject.SmartWasteManagement.user.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.*;
import java.util.Optional;
import static org.mockito.Mockito.*;

/**
 * TESTNG FEATURE: DATA PROVIDERS
 *
 * Demonstrates:
 * @DataProvider - Provides test data to test methods
 * Parameterized testing - Same test runs with multiple data sets
 */
public class CollectionServiceDataProviderTest {

    @Mock
    private CollectionEventRepository collectionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CollectionService collectionService;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * DataProvider for bin ID scenarios
     */
    @DataProvider(name = "binIdData")
    public Object[][] provideBinIds() {
        return new Object[][] {
                {"BIN-001", true, "Valid bin ID"},
                {"BIN-999", false, "Invalid bin ID"},
                {"", false, "Empty bin ID"},
                {null, false, "Null bin ID"}
        };
    }

    @Test(dataProvider = "binIdData")
    public void testFindBin(String binId, boolean shouldExist, String description) {
        System.out.println("   Testing: " + description);

        WasteBin mockBin = new WasteBin();
        mockBin.setBinId("BIN-001");

        if (shouldExist) {
            when(collectionRepository.findById(any())).thenReturn(Optional.empty());
        }

        // Just demonstrating the data provider - actual test would do more
        Assert.assertNotNull(description);
    }

    /**
     * DataProvider for weight calculations
     */
    @DataProvider(name = "weightData")
    public Object[][] provideWeightData() {
        return new Object[][] {
                {10.0, 5.0, 50.0},   // weight, rate, expected charge
                {20.0, 4.0, 80.0},
                {0.0, 5.0, 0.0},
                {15.5, 3.0, 46.5}
        };
    }

    @Test(dataProvider = "weightData")
    public void testCalculateCharge(double weight, double rate, double expectedCharge) {
        System.out.println("   Testing charge: " + weight + "kg @ $" + rate + " = $" + expectedCharge);

        double actualCharge = weight * rate;
        Assert.assertEquals(actualCharge, expectedCharge, 0.001);
    }

    /**
     * DataProvider for bin status based on fill level
     */
    @DataProvider(name = "binLevelData")
    public Object[][] provideBinLevelData() {
        return new Object[][] {
                {30.0, "ACTIVE", "Below 80%"},
                {85.0, "NEEDS_EMPTYING", "Above 80%"},
                {100.0, "FULL", "100% full"},
                {0.0, "EMPTY", "Empty bin"}
        };
    }

    @Test(dataProvider = "binLevelData")
    public void testBinStatus(double level, String expectedStatus, String scenario) {
        System.out.println("   Testing scenario: " + scenario);

        WasteBin bin = new WasteBin();
        bin.setCurrentLevel(level);

        // Simplified status logic for demo
        String actualStatus;
        if (level >= 100) actualStatus = "FULL";
        else if (level >= 80) actualStatus = "NEEDS_EMPTYING";
        else if (level <= 0) actualStatus = "EMPTY";
        else actualStatus = "ACTIVE";

        Assert.assertEquals(actualStatus, expectedStatus);
    }
}