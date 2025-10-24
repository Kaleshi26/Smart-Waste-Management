// Testing main application context loading
package com.CSSEProject.SmartWasteManagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test class to verify that the Spring Boot application context loads successfully.
 * This is a basic integration test that ensures all beans are properly configured.
 */
@SpringBootTest
class SmartWasteManagementApplicationTests {

    @Test
    void contextLoads() {
        // This test will pass if the Spring application context loads without errors
        // No additional assertions needed - the test framework will fail if context loading fails
    }
}