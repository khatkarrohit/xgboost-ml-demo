package org.icewheel.xgboostmldemo.service;

import ml.dmlc.xgboost4j.java.XGBoostError;
import org.icewheel.xgboostmldemo.model.PredictionRequest;
import org.icewheel.xgboostmldemo.model.PredictionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class XGBoostServiceTest {

    @Autowired
    private XGBoostService xgboostService;

    @Test
    void testHighRiskReasons() throws XGBoostError {
        // A profile with high age and high cholesterol - expect "Age is high" and "Cholesterol level is high"
        PredictionRequest request = PredictionRequest.builder()
                .age(75)
                .cholesterol(280)
                .bloodPressure(110)
                .heartRate(65)
                .exerciseIntensity(8)
                .build();

        PredictionResponse response = xgboostService.predict(request);

        assertNotNull(response);
        // Age and Cholesterol should be the top bad reasons as they are high
        assertTrue(response.getTopBadReasons().contains(XGBoostService.AGE_HIGH));
        assertTrue(response.getTopBadReasons().contains(XGBoostService.CHOLESTEROL_HIGH));
        // Exercise and Blood pressure should be good reasons as they are healthy/high intensity
        assertTrue(response.getTopGoodReasons().contains(XGBoostService.EXERCISE_HIGH));
    }

    @Test
    void testLowRiskReasons() throws XGBoostError {
        // A profile with low age and high exercise - expect good reasons related to age and exercise
        PredictionRequest request = PredictionRequest.builder()
                .age(25)
                .cholesterol(170)
                .bloodPressure(110)
                .heartRate(65)
                .exerciseIntensity(9)
                .build();

        PredictionResponse response = xgboostService.predict(request);

        assertNotNull(response);
        assertTrue(response.getRiskScore() < 0.3f);
        assertTrue(response.getTopGoodReasons().contains(XGBoostService.AGE_LOW));
        assertTrue(response.getTopGoodReasons().contains(XGBoostService.EXERCISE_HIGH));
    }

    @Test
    void testReasonLimits() throws XGBoostError {
        // Should not return more than 2 reasons per category
        PredictionRequest request = PredictionRequest.builder()
                .age(80)
                .cholesterol(300)
                .bloodPressure(180)
                .heartRate(100)
                .exerciseIntensity(0)
                .build();

        PredictionResponse response = xgboostService.predict(request);

        assertNotNull(response);
        assertTrue(response.getTopBadReasons().size() <= 2);
        assertTrue(response.getTopGoodReasons().size() <= 2);
    }
}
