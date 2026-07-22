package org.rishi.itdp_backend.models;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Train Entity
 * Represents trains running between stations
 * @ManyToOne: Many trains can originate from one station
 * @Builder: Allows convenient object creation:
 *           Train.builder().trainNumber("12345").trainName("Express").build()
 */
@Entity
@Table(name = "trains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder                        // Lombok: builder pattern for clean object creation
@JsonIgnoreProperties(ignoreUnknown = true)
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String trainNumber;

    @Column(nullable = false, length = 100)
    private String trainName;

    @Column(nullable = false, length = 20)
    private String trainType;  // Express, Local, Superfast, etc.

    // Many trains departing FROM one station
    @ManyToOne(fetch = FetchType.LAZY)  // LAZY = load only when accessed (better performance)
    @JoinColumn(name = "origin_station_id", nullable = false)
    private Station originStation;

    // Many trains arriving AT one station
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_station_id", nullable = false)
    private Station destinationStation;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private LocalTime arrivalTime;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions;
}