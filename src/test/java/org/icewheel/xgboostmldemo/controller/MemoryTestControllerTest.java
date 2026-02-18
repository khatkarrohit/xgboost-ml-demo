package org.icewheel.xgboostmldemo.controller;

import ml.dmlc.xgboost4j.java.XGBoostError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MemoryTestControllerTest {

    @Autowired
    private MemoryTestController memoryTestController;

    @Test
    void testStressEndpoint() throws XGBoostError {
        // Direct call to verify the controller is wired and logic works
        String result = memoryTestController.stressTest(100, false);
        assertNotNull(result);
        assertTrue(result.contains("Stress test completed"));
    }
}
