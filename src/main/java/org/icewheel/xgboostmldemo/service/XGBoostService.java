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
import jakarta.annotation.PreDestroy;
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
    private final Map<Integer, FeatureDescriptor> featureDescriptors = new HashMap<>();

    public XGBoostService(DataService dataService, @Value("${xgboost.model.path}") String modelPath) {
        this.dataService = dataService;
        this.modelPath = modelPath;
        initializeFeatureDescriptors();
    }

    private void initializeFeatureDescriptors() {
        featureDescriptors.put(FEATURE_AGE, new FeatureDescriptor(
                "Age", AGE_HIGH, AGE_LOW));
        featureDescriptors.put(FEATURE_CHOLESTEROL, new FeatureDescriptor(
                "Cholesterol", CHOLESTEROL_HIGH, CHOLESTEROL_HEALTHY));
        featureDescriptors.put(FEATURE_BLOOD_PRESSURE, new FeatureDescriptor(
                "Blood Pressure", BP_HIGH, BP_HEALTHY));
        featureDescriptors.put(FEATURE_HEART_RATE, new FeatureDescriptor(
                "Heart Rate", HR_HIGH, HR_HEALTHY));
        featureDescriptors.put(FEATURE_EXERCISE_INTENSITY, new FeatureDescriptor(
                "Exercise Intensity", EXERCISE_LOW, EXERCISE_HIGH));
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

    /**
     * Proper cleanup of native resources when the Spring bean is destroyed.
     */
    @PreDestroy
    public void cleanup() {
        if (this.booster != null) {
            this.booster.dispose();
            System.out.println("Booster native resources disposed.");
        }
    }

    /**
     * Trains the XGBoost model using synthetic data from DataService.
     * This method:
     * 1. Generates training data (features and labels).
     * 2. Converts features to a flattened float array.
     * 3. Wraps data in a DMatrix object.
     * 4. Configures training parameters.
     * 5. Trains the Booster and saves it to a JSON file.
     * 6. Disposes of the DMatrix to free native memory.
     */
    public void trainModel() throws XGBoostError {
        // trainCount = 1000: For a simple synthetic dataset with 5 features, 1000 samples 
        // provide a good balance between training speed and model accuracy. It's enough 
        // for the model to capture the linear-ish relationships we defined in DataService.
        int trainCount = 1000;
        List<PredictionRequest> features = dataService.generateFeatures(trainCount);
        float[] labels = dataService.generateLabels(features);

        float[] flatFeatures = new float[trainCount * FEATURE_COUNT];
        for (int i = 0; i < trainCount; i++) {
            float[] f = features.get(i).toFeatureArray();
            System.arraycopy(f, 0, flatFeatures, i * FEATURE_COUNT, FEATURE_COUNT);
        }

        // DMatrix is the core data structure used by XGBoost.
        // It's a memory-efficient representation of the data, optimized for the
        // underlying C++ implementation. We specify -1.0f as the 'missing' value.
        DMatrix trainMat = new DMatrix(flatFeatures, trainCount, FEATURE_COUNT, -1.0f);
        try {
            // Set labels (target values) for training
            trainMat.setLabel(labels);

            // Define training parameters
            Map<String, Object> params = new HashMap<>();
            params.put("eta", 0.1); // Learning rate: scales the contribution of each tree
            params.put("max_depth", 5); // Maximum depth of each tree: controls model complexity
            params.put("objective", "binary:logistic"); // Logistic regression for binary classification (outputs probabilities)
            params.put("eval_metric", "logloss"); // Metric to evaluate model performance during training

            // Watches allow us to monitor the performance on a dataset (e.g., training set) during training
            Map<String, DMatrix> watches = new HashMap<>();
            watches.put("train", trainMat);

            // round = 50: Number of boosting rounds (number of trees to build).
            // 50 rounds are usually sufficient for this simple classification problem to converge
            // without overfitting. Too many rounds might over-learn the noise in synthetic data.
            int round = 50; 
            
            // If there's an existing booster, dispose it before creating a new one
            if (this.booster != null) {
                this.booster.dispose();
            }
            
            // XGBoost.train is the core method that starts the learning process
            this.booster = XGBoost.train(trainMat, params, round, watches, null, null);
            
            // Save the trained model to disk in the configured format (JSON)
            this.booster.saveModel(modelPath);
            System.out.println("Model trained and saved to " + modelPath);
        } finally {
            // IMPORTANT: DMatrix objects allocate memory in the native (C++) layer via JNI.
            // We MUST call dispose() to free this memory and avoid memory leaks in Java.
            // Using a finally block ensures disposal even if training fails.
            trainMat.dispose();
        }
    }

    /**
     * Makes a prediction for a single health profile.
     * Returns a response with the probability score, category, and explanatory reason codes.
     */
    public PredictionResponse predict(PredictionRequest request) throws XGBoostError {
        if (this.booster == null) {
            throw new RuntimeException("Model not initialized");
        }

        float[] features = request.toFeatureArray();
        // Create a DMatrix for a single prediction row. 
        // Even for one prediction, XGBoost requires a DMatrix wrapper.
        DMatrix data = new DMatrix(features, 1, FEATURE_COUNT, -1.0f);
        
        float score;
        float[] rowContribs;
        
        try {
            // 1. Get the probability score
            // predict() returns a 2D array [number of rows][number of classes/output]
            float[][] prediction = this.booster.predict(data);
            score = prediction[0][0];

            // 2. Get SHAP values (contributions) using predictContrib
            // predictContrib() calculates how much each feature contributed to the final score.
            // The result is an array: [feature0_contrib, feature1_contrib, ..., bias_term]
            // ntreeLimit 0 means use all trees in the model for calculation.
            float[][] contribs = this.booster.predictContrib(data, 0);
            rowContribs = contribs[0]; // size is FEATURE_COUNT + 1 (last is bias)
        } finally {
            // Free native memory allocated for this prediction's DMatrix.
            // Using a finally block ensures we don't leak memory even if prediction fails.
            data.dispose();
        }

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
        FeatureDescriptor descriptor = featureDescriptors.get(index);
        if (descriptor == null) {
            return UNKNOWN_FACTOR;
        }
        return isPositiveContribution ? descriptor.positiveReason : descriptor.negativeReason;
    }

    private record FeatureDescriptor(String name, String positiveReason, String negativeReason) {}

    private static class FeatureContribution {
        int index;
        float value;

        FeatureContribution(int index, float value) {
            this.index = index;
            this.value = value;
        }
    }
}
