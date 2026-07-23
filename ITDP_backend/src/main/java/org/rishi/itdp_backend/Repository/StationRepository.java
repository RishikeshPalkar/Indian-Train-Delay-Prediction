package org.rishi.itdp_backend.Repository;


@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
     /**
     * Find station by code
     * Method name follows convention: findBy<FieldName>
     * Spring auto-generates: SELECT * FROM stations WHERE station_code = ?
     */
    Optional<Station> findByStationCode(String stationCode);
    
    /**
     * Find all stations by region
     */
    List<Station> findByRegion(String region);
    
    /**
     * Custom query using @Query
     * Write exact SQL or JPQL (Java Persistence Query Language)
     */
    @Query("SELECT s FROM Station s WHERE LOWER(s.stationName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Station> searchStationsByName(@Param("searchTerm") String searchTerm);
    
    /**
     * Check if station exists by code
     */
    boolean existsByStationCode(String stationCode);
}
