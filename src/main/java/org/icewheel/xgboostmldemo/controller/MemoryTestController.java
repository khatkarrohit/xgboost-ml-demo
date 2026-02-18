package org.icewheel.xgboostmldemo.controller;

import ml.dmlc.xgboost4j.java.XGBoostError;
import org.icewheel.xgboostmldemo.model.PredictionRequest;
import org.icewheel.xgboostmldemo.service.DataService;
import org.icewheel.xgboostmldemo.service.XGBoostService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class MemoryTestController {

    private final XGBoostService xgboostService;
    private final DataService dataService;

    public MemoryTestController(XGBoostService xgboostService, DataService dataService) {
        this.xgboostService = xgboostService;
        this.dataService = dataService;
    }

    /**
     * Stress test endpoint that runs many predictions.
     * @param count Number of prediction requests to process.
     * @param leak If true, DMatrix objects will NOT be disposed, causing a native memory leak.
     * @return Status message.
     */
    @PostMapping("/stress")
    public String stressTest(@RequestParam(defaultValue = "1000") int count, 
                             @RequestParam(defaultValue = "false") boolean leak) throws XGBoostError {
        
        System.out.println("Starting stress test: count=" + count + ", leak=" + leak);
        
        // Use a reasonable batch size to balance performance and native memory leak visibility
        int batchSize = 1000;
        int batches = Math.max(1, count / batchSize);
        
        for (int i = 0; i < batches; i++) {
            List<PredictionRequest> requests = dataService.generateFeatures(batchSize);
            // Calling the version of predictBatch that allows toggling dispose()
            xgboostService.predictBatch(requests, !leak);
            
            if (i % 10 == 0) {
                System.out.println("Processed " + (i * batchSize) + " requests...");
            }
        }
        
        return "Stress test completed. Processed " + count + " requests with leak=" + leak;
    }
}
