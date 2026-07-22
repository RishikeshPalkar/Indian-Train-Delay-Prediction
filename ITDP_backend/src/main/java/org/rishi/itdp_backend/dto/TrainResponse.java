package org.rishi.itdp_backend.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

/**
 * DTO for train response
 * Returned to client with filtered fields only
 *
 * Client receives JSON:
 * {
 *   "id": 1,
 *   "trainNumber": "12345",
 *   "trainName": "Rajdhani Express",
 *   ...
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainResponse {

    private Long id;
    private String trainNumber;
    private String trainName;
    private String trainType;

    // Origin/Destination info
    private String originStationCode;
    private String originStationName;

    private String destinationStationCode;
    private String destinationStationName;

    // Timing
    private LocalTime departureTime;
    private LocalTime arrivalTime;

    // Capacity
    private Integer totalSeats;
}