package org.rishi.itdp_backend.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String stationCode;

    @Column(nullable = false, length = 100)
    private String stationName;

    @Column(length = 50)
    private String region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // One station can have many trains departing from it
    @OneToMany(mappedBy = "originStation", cascade = CascadeType.ALL)
    private List<Train> departingTrains;

    // One station can have many trains arriving at it
    @OneToMany(mappedBy = "destinationStation", cascade = CascadeType.ALL)
    private List<Train> arrivingTrains;
}
