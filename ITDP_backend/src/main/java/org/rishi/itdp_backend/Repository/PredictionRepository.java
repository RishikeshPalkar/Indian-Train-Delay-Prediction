package org.rishi.itdp_backend.Repository;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    /**
     * Find all predictions for a specific train
     */
    List<Prediction> findByTrainId(Long trainId);
    
    /**
     * Find latest prediction for a train
     */
    @Query(value = "SELECT * FROM predictions WHERE train_id = :trainId " +
                   "ORDER BY prediction_date DESC LIMIT 1",
           nativeQuery = true)
    Prediction findLatestPredictionForTrain(@Param("trainId") Long trainId);
    
    /**
     * Get average delay prediction for a train
     */
    @Query("SELECT AVG(p.predictedDelayMinutes) FROM Prediction p WHERE p.train.id = :trainId")
    Double getAverageDelayForTrain(@Param("trainId") Long trainId);
}
