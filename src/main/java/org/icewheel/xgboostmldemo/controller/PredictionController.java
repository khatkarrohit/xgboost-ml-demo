package org.icewheel.xgboostmldemo.controller;

import ml.dmlc.xgboost4j.java.XGBoostError;
import org.icewheel.xgboostmldemo.model.PredictionRequest;
import org.icewheel.xgboostmldemo.model.PredictionResponse;
import org.icewheel.xgboostmldemo.service.XGBoostService;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prediction")
public class PredictionController {

    private final XGBoostService xgboostService;

    public PredictionController(XGBoostService xgboostService) {
        this.xgboostService = xgboostService;
    }

    @PostMapping("/predict")
    public PredictionResponse predict(@RequestBody PredictionRequest request) throws XGBoostError {
        return xgboostService.predict(request);
    }

    @PostMapping("/predict-batch")
    public List<PredictionResponse> predictBatch(@RequestBody List<PredictionRequest> requests) throws XGBoostError {
        return xgboostService.predictBatch(requests);
    }

    @PostMapping("/retrain")
    public String retrain() throws XGBoostError {
        xgboostService.trainModel();
        return "Model retrained successfully";
    }
}
