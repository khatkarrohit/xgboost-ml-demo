package org.icewheel.xgboostmldemo.controller;

import org.icewheel.xgboostmldemo.service.XGBoostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PredictionControllerTest {

    @Autowired
    private PredictionController predictionController;

    @Autowired
    private XGBoostService xgboostService;

    @Test
    void contextLoads() {
        assertNotNull(predictionController);
        assertNotNull(xgboostService);
    }
}
