package org.rishi.itdp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for train search request
 * @NotBlank - Validation annotation (provided by spring-boot-starter-validation)
 *
 * When client sends JSON:
 * {
 *   "originStationCode": "CSTM",
 *   "destinationStationCode": "NDLS"
 * }
 *
 * Spring automatically converts to this object (deserialization)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainSearchRequest {

    @NotBlank(message = "Origin station code cannot be blank")
    private String originStationCode;

    @NotBlank(message = "Destination station code cannot be blank")
    private String destinationStationCode;
}