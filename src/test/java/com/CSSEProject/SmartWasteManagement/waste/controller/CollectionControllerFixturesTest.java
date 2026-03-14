package com.CSSEProject.SmartWasteManagement.waste.controller;

import com.CSSEProject.SmartWasteManagement.dto.CollectionRequestDto;
import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.service.CollectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TESTNG FEATURE: FIXTURES/SETUP/TEARDOWN
 *
 * Demonstrates:
 * @BeforeSuite - Runs once before all tests
 * @BeforeClass - Runs once before any test in this class
 * @BeforeMethod - Runs before each test method
 * @AfterMethod - Runs after each test method
 * @AfterClass - Runs once after all tests
 * @AfterSuite - Runs once after all tests
 */
public class CollectionControllerFixturesTest {

    private MockMvc mockMvc;

    @Mock
    private CollectionService collectionService;

    @InjectMocks
    private CollectionController collectionController;

    private ObjectMapper objectMapper;
    private CollectionRequestDto validRequest;
    private CollectionEvent mockCollection;
    private AutoCloseable mocks;

    @BeforeSuite
    public void beforeSuite() {
        System.out.println("🏁 BEFORE SUITE - Initialize test suite resources");
    }

    @BeforeClass
    public void beforeClass() {
        System.out.println("📋 BEFORE CLASS - Setup for CollectionController tests");
    }

    @BeforeMethod
    public void setUp() {
        System.out.println("🔧 BEFORE METHOD - Creating fresh test fixtures");

        mocks = MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(collectionController).build();
        objectMapper = new ObjectMapper();

        // Create test fixtures
        validRequest = new CollectionRequestDto();
        validRequest.setBinId("BIN-001");
        validRequest.setWeight(10.5);

        mockCollection = new CollectionEvent();
        mockCollection.setId(1L);
        mockCollection.setWeight(10.5);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        System.out.println("🧹 AFTER METHOD - Cleaning up after test");
        if (mocks != null) {
            mocks.close();
        }
    }

    @AfterClass
    public void afterClass() {
        System.out.println("📋 AFTER CLASS - CollectionController tests completed");
    }

    @AfterSuite
    public void afterSuite() {
        System.out.println("🏁 AFTER SUITE - Release test suite resources");
    }

    @Test
    public void testRecordCollection_Success() throws Exception {
        System.out.println("   ✅ TEST 1: Record Collection Success");

        when(collectionService.recordCollection(any(CollectionRequestDto.class)))
                .thenReturn(mockCollection);

        mockMvc.perform(post("/api/waste/collections/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    public void testRecordCollection_Error() throws Exception {
        System.out.println("   ✅ TEST 2: Record Collection Error");

        when(collectionService.recordCollection(any(CollectionRequestDto.class)))
                .thenThrow(new RuntimeException("Bin not found"));

        mockMvc.perform(post("/api/waste/collections/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }
}