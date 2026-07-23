package org.rishi.itdp_backend.Repository;

@RRepository
public interface TrainRepository extends JpaRepository<Train, Long> {
    /**
     * Find train by train number
     */
    Optional<Train> findByTrainNumber(String trainNumber);
    
    /**
     * Find trains between two stations
     * JPQL: Like SQL but uses entity names and field names
     */
    @Query("SELECT t FROM Train t WHERE t.originStation.id = :originStationId " +
           "AND t.destinationStation.id = :destinationStationId " +
           "ORDER BY t.departureTime ASC")
    List<Train> findTrainsBetweenStations(
        @Param("originStationId") Long originStationId,
        @Param("destinationStationId") Long destinationStationId
    );
    
    /**
     * Find trains by train type (Express, Local, Superfast, etc.)
     */
    List<Train> findByTrainType(String trainType);
    
    /**
     * Native SQL query (if needed)
     * nativeQuery = true uses raw PostgreSQL SQL
     */
    @Query(value = "SELECT * FROM trains WHERE total_seats > :minSeats", 
           nativeQuery = true)
    List<Train> findTrainsWithMinSeats(@Param("minSeats") Integer minSeats);
}
