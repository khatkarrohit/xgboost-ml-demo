package org.icewheel.xgboostmldemo.service;

import org.icewheel.xgboostmldemo.model.PredictionRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DataService {

    private final Random random = new Random();

    public List<PredictionRequest> generateFeatures(int count) {
        List<PredictionRequest> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(PredictionRequest.builder()
                    .age(20 + random.nextFloat() * 60)
                    .cholesterol(150 + random.nextFloat() * 150)
                    .bloodPressure(90 + random.nextFloat() * 90)
                    .heartRate(60 + random.nextFloat() * 40)
                    .exerciseIntensity(random.nextFloat() * 10)
                    .build());
        }
        return data;
    }

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
