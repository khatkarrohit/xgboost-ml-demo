package org.icewheel.xgboostmldemo.service;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.icewheel.xgboostmldemo.model.PredictionRequest;
import org.icewheel.xgboostmldemo.model.PredictionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class XGBoostService {

    public static final String AGE_HIGH = "Age is high";
    public static final String AGE_LOW = "Age is low/young";
    public static final String CHOLESTEROL_HIGH = "Cholesterol level is high";
    public static final String CHOLESTEROL_HEALTHY = "Cholesterol level is healthy";
    public static final String BP_HIGH = "Blood pressure is high";
    public static final String BP_HEALTHY = "Blood pressure is healthy";
    public static final String HR_HIGH = "Heart rate is high";
    public static final String HR_HEALTHY = "Heart rate is healthy";
    public static final String EXERCISE_LOW = "Low exercise intensity";
    public static final String EXERCISE_HIGH = "High exercise intensity";
    public static final String UNKNOWN_FACTOR = "Unknown factor";

    public static final int FEATURE_AGE = 0;
    public static final int FEATURE_CHOLESTEROL = 1;
    public static final int FEATURE_BLOOD_PRESSURE = 2;
    public static final int FEATURE_HEART_RATE = 3;
    public static final int FEATURE_EXERCISE_INTENSITY = 4;
    public static final int FEATURE_COUNT = 5;

    private Booster booster;
    private final String modelPath;
    private final DataService dataService;

    public XGBoostService(DataService dataService, @Value("${xgboost.model.path}") String modelPath) {
        this.dataService = dataService;
        this.modelPath = modelPath;
    }

    @PostConstruct
    public void init() throws XGBoostError {
        File modelFile = new File(modelPath);
        if (modelFile.exists()) {
            this.booster = XGBoost.loadModel(modelPath);
            System.out.println("Model loaded from " + modelPath);
        } else {
            System.out.println("No model found. Training a new one...");
            trainModel();
        }
    }

    public void trainModel() throws XGBoostError {
        int trainCount = 1000;
        List<PredictionRequest> features = dataService.generateFeatures(trainCount);
        float[] labels = dataService.generateLabels(features);

        float[] flatFeatures = new float[trainCount * FEATURE_COUNT];
        for (int i = 0; i < trainCount; i++) {
            float[] f = features.get(i).toFeatureArray();
            System.arraycopy(f, 0, flatFeatures, i * FEATURE_COUNT, FEATURE_COUNT);
        }

        DMatrix trainMat = new DMatrix(flatFeatures, trainCount, FEATURE_COUNT, -1.0f);
        trainMat.setLabel(labels);

        Map<String, Object> params = new HashMap<>();
        params.put("eta", 0.1);
        params.put("max_depth", 5);
        params.put("objective", "binary:logistic");
        params.put("eval_metric", "logloss");

        Map<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainMat);

        int round = 50;
        this.booster = XGBoost.train(trainMat, params, round, watches, null, null);
        this.booster.saveModel(modelPath);
        trainMat.dispose();
        System.out.println("Model trained and saved to " + modelPath);
    }

    public PredictionResponse predict(PredictionRequest request) throws XGBoostError {
        if (this.booster == null) {
            throw new RuntimeException("Model not initialized");
        }

        float[] features = request.toFeatureArray();
        DMatrix data = new DMatrix(features, 1, FEATURE_COUNT, -1.0f);
        
        // 1. Get the probability score
        float[][] prediction = this.booster.predict(data);
        float score = prediction[0][0];

        // 2. Get SHAP values (contributions)
        // Parameters: data, ntreeLimit (0 means use all)
        float[][] contribs = this.booster.predictContrib(data, 0);
        float[] rowContribs = contribs[0]; // numFeatures + 1 (bias is the last)
        
        data.dispose();

        // 3. Process reason codes
        List<FeatureContribution> allContribs = new ArrayList<>();
        for (int i = 0; i < FEATURE_COUNT; i++) {
            allContribs.add(new FeatureContribution(i, rowContribs[i]));
        }
        
        List<String> badReasons = allContribs.stream()
                .filter(c -> c.value > 0.01f) // Filter out negligible contributions
                .sorted((a, b) -> Float.compare(b.value, a.value)) // High positive first
                .limit(2)
                .map(c -> getReasonDescription(c.index, true))
                .collect(Collectors.toList());

        List<String> goodReasons = allContribs.stream()
                .filter(c -> c.value < -0.01f) // Filter out negligible contributions
                .sorted((a, b) -> Float.compare(a.value, b.value)) // High negative first
                .limit(2)
                .map(c -> getReasonDescription(c.index, false))
                .collect(Collectors.toList());

        return PredictionResponse.builder()
                .riskScore(score)
                .riskCategory(score > 0.5f ? 1 : 0)
                .topBadReasons(badReasons)
                .topGoodReasons(goodReasons)
                .build();
    }

    private String getReasonDescription(int index, boolean isPositiveContribution) {
        return switch (index) {
            case FEATURE_AGE -> isPositiveContribution ? AGE_HIGH : AGE_LOW;
            case FEATURE_CHOLESTEROL -> isPositiveContribution ? CHOLESTEROL_HIGH : CHOLESTEROL_HEALTHY;
            case FEATURE_BLOOD_PRESSURE -> isPositiveContribution ? BP_HIGH : BP_HEALTHY;
            case FEATURE_HEART_RATE -> isPositiveContribution ? HR_HIGH : HR_HEALTHY;
            case FEATURE_EXERCISE_INTENSITY -> isPositiveContribution ? EXERCISE_LOW : EXERCISE_HIGH;
            default -> UNKNOWN_FACTOR;
        };
    }

    private static class FeatureContribution {
        int index;
        float value;

        FeatureContribution(int index, float value) {
            this.index = index;
            this.value = value;
        }
    }
}
