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
import org.testng.Assert;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TESTNG FEATURE: ASSERTIONS
 *
 * Demonstrates different types of TestNG assertions:
 * - Assert.assertEquals() - Check equality
 * - Assert.assertNotEquals() - Check inequality
 * - Assert.assertTrue() / assertFalse() - Check boolean conditions
 * - Assert.assertNull() / assertNotNull() - Check null/not null
 * - Assert.fail() - Force test failure
 * - Assert.expectThrows() - Check exceptions
 */
public class CollectionControllerAssertionsTest {

    private MockMvc mockMvc;

    @Mock
    private CollectionService collectionService;

    @InjectMocks
    private CollectionController collectionController;

    private ObjectMapper objectMapper;
    private CollectionRequestDto validRequest;
    private CollectionEvent mockCollection;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(collectionController).build();
        objectMapper = new ObjectMapper();

        validRequest = new CollectionRequestDto();
        validRequest.setBinId("BIN-001");
        validRequest.setWeight(10.5);

        mockCollection = new CollectionEvent();
        mockCollection.setId(1L);
        mockCollection.setWeight(10.5);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testAssertEquals() {
        System.out.println("   📌 Testing assertEquals");

        int expected = 10;
        int actual = 5 + 5;

        Assert.assertEquals(actual, expected, "10 should equal 5+5");
    }

    @Test
    public void testAssertNotEquals() {
        System.out.println("   📌 Testing assertNotEquals");

        int value1 = 10;
        int value2 = 20;

        Assert.assertNotEquals(value1, value2, "10 and 20 should be different");
    }

    @Test
    public void testAssertTrue() {
        System.out.println("   📌 Testing assertTrue");

        boolean isActive = true;

        Assert.assertTrue(isActive, "Status should be active");
    }

    @Test
    public void testAssertFalse() {
        System.out.println("   📌 Testing assertFalse");

        boolean isDeleted = false;

        Assert.assertFalse(isDeleted, "Should not be deleted");
    }

    @Test
    public void testAssertNotNull() {
        System.out.println("   📌 Testing assertNotNull");

        String binId = "BIN-001";

        Assert.assertNotNull(binId, "Bin ID should not be null");
    }

    @Test
    public void testAssertNull() {
        System.out.println("   📌 Testing assertNull");

        String error = null;

        Assert.assertNull(error, "Error should be null");
    }

    @Test
    public void testAssertWithDelta() {
        System.out.println("   📌 Testing assertEquals with delta");

        double expected = 10.5;
        double actual = 10.5;

        Assert.assertEquals(actual, expected, 0.001, "Values should match within 0.001");
    }

    @Test
    public void testExpectThrows() {
        System.out.println("   📌 Testing expectThrows");

        RuntimeException exception = Assert.expectThrows(RuntimeException.class, () -> {
            throw new RuntimeException("Test exception");
        });

        Assert.assertEquals(exception.getMessage(), "Test exception");
    }

    @Test
    public void testControllerResponse() throws Exception {
        System.out.println("   📌 Testing controller response with assertions");

        when(collectionService.recordCollection(any(CollectionRequestDto.class)))
                .thenReturn(mockCollection);

        String response = mockMvc.perform(post("/api/waste/collections/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assert.assertNotNull(response);
        Assert.assertTrue(response.contains("collection"));
        Assert.assertTrue(response.contains("1"));
    }
}