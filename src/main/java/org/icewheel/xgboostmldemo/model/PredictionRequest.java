package org.icewheel.xgboostmldemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequest {
    private float age;
    private float cholesterol;
    private float bloodPressure;
    private float heartRate;
    private float exerciseIntensity;

    public float[] toFeatureArray() {
        return new float[]{age, cholesterol, bloodPressure, heartRate, exerciseIntensity};
    }
}
