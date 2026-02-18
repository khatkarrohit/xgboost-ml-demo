package org.icewheel.xgboostmldemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {
    private String patientId;
    private float riskScore;
    private int riskCategory; // 0 for low, 1 for high
    private List<String> topBadReasons;
    private List<String> topGoodReasons;
}
