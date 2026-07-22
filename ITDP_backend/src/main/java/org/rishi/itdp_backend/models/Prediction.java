package org.rishi.itdp_backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Prediction Entity
 * Stores ML model predictions for audit & analytics
 */
@Entity
@Table(name = "predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false)
    private Integer predictedDelayMinutes;

    @Column(name = "confidence_score")
    private Double confidenceScore;  // 0.0 to 1.0 (0% to 100%)

    @Column(name = "prediction_date", nullable = false, updatable = false)
    private LocalDateTime predictionDate = LocalDateTime.now();
}