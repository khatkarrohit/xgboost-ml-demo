package org.icewheel.xgboostmldemo.service;

import org.icewheel.xgboostmldemo.model.PredictionRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DataService {

    private final Random random = new Random();

    /**
     * Generates a list of synthetic health profiles for training.
     * Each profile contains random but realistic health metrics.
     */
    public List<PredictionRequest> generateFeatures(int count) {
        List<PredictionRequest> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(PredictionRequest.builder()
                    .patientId("SYNTHETIC_" + i)
                    .age(20 + random.nextFloat() * 60)
                    .cholesterol(150 + random.nextFloat() * 150)
                    .bloodPressure(90 + random.nextFloat() * 90)
                    .heartRate(60 + random.nextFloat() * 40)
                    .exerciseIntensity(random.nextFloat() * 10)
                    .build());
        }
        return data;
    }

    /**
     * Generates labels (0 or 1) for a list of health profiles.
     * This method implements a "Ground Truth" logic that the XGBoost model will try to learn.
     * 
     * The formula assigns weights to different factors:
     * - Age (40% weight): Higher age increases risk.
     * - Cholesterol (30% weight): Higher cholesterol increases risk.
     * - Blood Pressure (20% weight): Higher BP increases risk.
     * - Heart Rate (10% weight): Higher HR increases risk.
     * - Exercise (-20% weight): Higher exercise intensity decreases risk.
     * 
     * We also add some random noise to make the learning task more realistic.
     */
    public float[] generateLabels(List<PredictionRequest> features) {
        float[] labels = new float[features.size()];
        for (int i = 0; i < features.size(); i++) {
            PredictionRequest f = features.get(i);
            float score = (f.getAge() / 100 * 0.4f) + 
                          (f.getCholesterol() / 300 * 0.3f) + 
                          (f.getBloodPressure() / 200 * 0.2f) + 
                          (f.getHeartRate() / 150 * 0.1f) - 
                          (f.getExerciseIntensity() / 10 * 0.2f);
            
            // Add some noise
            score += (random.nextFloat() - 0.5f) * 0.1f;
            
            labels[i] = score > 0.5f ? 1.0f : 0.0f;
        }
        return labels;
    }
}
