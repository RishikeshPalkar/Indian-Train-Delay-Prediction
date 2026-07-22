package org.rishi.itdp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for ML prediction response
 * Contains prediction results from FastAPI service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponse {

    private Long trainId;
    private String trainNumber;
    private String trainName;

    // Prediction results
    private Integer predictedDelayMinutes;
    private Double confidenceScore;  // 0.0 to 1.0
    private String delayCategory;    // "On-time", "Mild Delay", "Severe Delay"

    // Metadata
    private LocalDateTime predictionTime;
    private String message;
}