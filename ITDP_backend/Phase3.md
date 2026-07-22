# 🏗️ PHASE 3: REPOSITORY & SERVICE LAYERS

## 🎯 LEARNING GOALS

In this phase you'll understand:
1. **Repository Pattern** - Data access layer (talking to database)
2. **Service Layer** - Business logic layer (processing data)
3. **Separation of Concerns** - Each layer has one job
4. **Spring Data JPA** - Automatic SQL query generation

---

## 🏛️ LAYERED ARCHITECTURE

```
┌─────────────────────────────┐
│  Controller Layer           │  ← HTTP Requests/Responses
│  (REST endpoints)           │
└────────────┬────────────────┘
             │
             ↓
┌─────────────────────────────┐
│  Service Layer              │  ← Business Logic
│  (Business rules, validation)│
└────────────┬────────────────┘
             │
             ↓
┌─────────────────────────────┐
│  Repository Layer           │  ← Database Queries
│  (Data access only)         │
└────────────┬────────────────┘
             │
             ↓
┌─────────────────────────────┐
│  Database (PostgreSQL)      │  ← Data Storage
└─────────────────────────────┘
```

**Why This Structure?**
- **Testable** - Mock each layer independently
- **Maintainable** - Easy to find code
- **Scalable** - Add features without breaking others
- **Reusable** - Services used by multiple controllers

---

## 📦 DATA TRANSFER OBJECTS (DTOs)

DTOs are lightweight objects for API communication (not database entities).

**Why use DTOs instead of Entities?**
- API doesn't expose internal structure
- Avoid lazy loading issues
- Reduce JSON payload size
- Security (hide sensitive fields)

### File: `dto/TrainSearchRequest.java`

```java
package com.traindelay.dto;

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
```

### File: `dto/TrainResponse.java`

```java
package com.traindelay.dto;

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
```

### File: `dto/PredictionResponse.java`

```java
package com.traindelay.dto;

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
```

### File: `dto/ApiResponse.java` (Generic Response Wrapper)

```java
package com.traindelay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Generic API response wrapper
 * All endpoints return this format for consistency
 * 
 * Success response:
 * {
 *   "success": true,
 *   "statusCode": 200,
 *   "data": {...},
 *   "timestamp": "2024-07-22T10:30:00"
 * }
 * 
 * Error response:
 * {
 *   "success": false,
 *   "statusCode": 404,
 *   "error": "Train not found",
 *   "timestamp": "2024-07-22T10:30:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // Exclude null fields from JSON
public class ApiResponse<T> {
    
    private Boolean success;
    private Integer statusCode;
    
    private T data;           // Actual response data
    private String error;     // Error message (if failed)
    private String message;   // Success message
    
    private LocalDateTime timestamp;
    
    // Factory methods for convenience
    public static <T> ApiResponse<T> success(T data, String message, Integer statusCode) {
        return ApiResponse.<T>builder()
            .success(true)
            .statusCode(statusCode)
            .data(data)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static <T> ApiResponse<T> error(String error, Integer statusCode) {
        return ApiResponse.<T>builder()
            .success(false)
            .statusCode(statusCode)
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

---

## 🔍 REPOSITORY LAYER

**What:** Interfaces for database queries
**How:** Extends JpaRepository (Spring Data gives you CRUD methods automatically)
**Where:** `repository/` package

### File: `repository/StationRepository.java`

```java
package com.traindelay.repository;

import com.traindelay.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

/**
 * Repository for Station entity
 * JpaRepository provides:
 * - save(entity)              → INSERT
 * - findById(id)              → SELECT by ID
 * - findAll()                 → SELECT all rows
 * - delete(entity)            → DELETE
 * - exists(id)                → Check if exists
 * 
 * You add custom queries using method names or @Query
 */
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
```

**Method Naming Convention Magic:**

Spring Data JPA generates SQL from method names:

| Method Name | Generated SQL |
|------------|---------------|
| `findById(id)` | `SELECT * FROM table WHERE id = ?` |
| `findByName(name)` | `SELECT * FROM table WHERE name = ?` |
| `findByNameAndAge(name, age)` | `SELECT * FROM table WHERE name = ? AND age = ?` |
| `findByNameOrEmail(name, email)` | `SELECT * FROM table WHERE name = ? OR email = ?` |
| `findByStatusOrderByNameAsc(status)` | `SELECT * FROM table WHERE status = ? ORDER BY name ASC` |
| `deleteByStatus(status)` | `DELETE FROM table WHERE status = ?` |

### File: `repository/TrainRepository.java`

```java
package com.traindelay.repository;

import com.traindelay.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
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
```

### File: `repository/PredictionRepository.java`

```java
package com.traindelay.repository;

import com.traindelay.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

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
```

---

## ⚙️ SERVICE LAYER

**What:** Contains business logic (validation, calculations, decisions)
**How:** Uses repositories to access data
**Where:** `service/` package

### File: `service/TrainService.java`

```java
package com.traindelay.service;

import com.traindelay.dto.TrainResponse;
import com.traindelay.dto.TrainSearchRequest;
import com.traindelay.model.Station;
import com.traindelay.model.Train;
import com.traindelay.repository.StationRepository;
import com.traindelay.repository.TrainRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for train operations
 * 
 * @Service tells Spring this is a business logic component
 * Automatically creates singleton instance and manages lifecycle
 * 
 * @Slf4j (Lombok) generates logger: log.info(), log.error(), etc.
 */
@Service
@Slf4j
public class TrainService {
    
    @Autowired
    private TrainRepository trainRepository;
    
    @Autowired
    private StationRepository stationRepository;
    
    /**
     * Search trains between two stations
     * 
     * Business logic:
     * 1. Validate station codes exist
     * 2. Fetch corresponding station IDs
     * 3. Query database for trains
     * 4. Convert entities to DTOs
     * 5. Return to controller
     */
    public List<TrainResponse> searchTrains(TrainSearchRequest request) {
        log.info("Searching trains from {} to {}", 
                 request.getOriginStationCode(), 
                 request.getDestinationStationCode());
        
        // Validation: Check if origin station exists
        Station originStation = stationRepository.findByStationCode(request.getOriginStationCode())
            .orElseThrow(() -> new RuntimeException(
                "Origin station not found: " + request.getOriginStationCode()));
        
        // Validation: Check if destination station exists
        Station destinationStation = stationRepository.findByStationCode(request.getDestinationStationCode())
            .orElseThrow(() -> new RuntimeException(
                "Destination station not found: " + request.getDestinationStationCode()));
        
        // Same station validation
        if (originStation.getId().equals(destinationStation.getId())) {
            throw new RuntimeException("Origin and destination must be different");
        }
        
        // Query database
        List<Train> trains = trainRepository.findTrainsBetweenStations(
            originStation.getId(), 
            destinationStation.getId()
        );
        
        log.info("Found {} trains", trains.size());
        
        // Convert entities to DTOs
        return trains.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get train details by ID
     */
    public TrainResponse getTrainById(Long trainId) {
        log.info("Fetching train with ID: {}", trainId);
        
        Train train = trainRepository.findById(trainId)
            .orElseThrow(() -> new RuntimeException("Train not found with ID: " + trainId));
        
        return convertToResponse(train);
    }
    
    /**
     * Helper method: Convert Train entity to TrainResponse DTO
     * Encapsulates transformation logic
     */
    private TrainResponse convertToResponse(Train train) {
        return TrainResponse.builder()
            .id(train.getId())
            .trainNumber(train.getTrainNumber())
            .trainName(train.getTrainName())
            .trainType(train.getTrainType())
            .originStationCode(train.getOriginStation().getStationCode())
            .originStationName(train.getOriginStation().getStationName())
            .destinationStationCode(train.getDestinationStation().getStationCode())
            .destinationStationName(train.getDestinationStation().getStationName())
            .departureTime(train.getDepartureTime())
            .arrivalTime(train.getArrivalTime())
            .totalSeats(train.getTotalSeats())
            .build();
    }
}
```

### File: `service/PredictionService.java`

```java
package com.traindelay.service;

import com.traindelay.client.FastAPIClient;
import com.traindelay.dto.PredictionResponse;
import com.traindelay.model.Prediction;
import com.traindelay.model.Train;
import com.traindelay.repository.PredictionRepository;
import com.traindelay.repository.TrainRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * Service for ML predictions
 * Orchestrates communication with FastAPI service
 */
@Service
@Slf4j
public class PredictionService {
    
    @Autowired
    private FastAPIClient fastAPIClient;
    
    @Autowired
    private TrainRepository trainRepository;
    
    @Autowired
    private PredictionRepository predictionRepository;
    
    /**
     * Predict delay for a train
     * 
     * Flow:
     * 1. Fetch train details
     * 2. Prepare data for ML model
     * 3. Call FastAPI service
     * 4. Save prediction to database
     * 5. Return formatted response
     */
    public PredictionResponse predictTrainDelay(Long trainId) {
        log.info("Predicting delay for train ID: {}", trainId);
        
        // Fetch train
        Train train = trainRepository.findById(trainId)
            .orElseThrow(() -> new RuntimeException("Train not found: " + trainId));
        
        try {
            // Call FastAPI ML service
            PredictionResponse mlResponse = fastAPIClient.predictDelay(train);
            
            // Save prediction to database for audit
            Prediction prediction = Prediction.builder()
                .train(train)
                .predictedDelayMinutes(mlResponse.getPredictedDelayMinutes())
                .confidenceScore(mlResponse.getConfidenceScore())
                .predictionDate(LocalDateTime.now())
                .build();
            
            predictionRepository.save(prediction);
            
            log.info("Prediction saved. Delay: {} mins, Confidence: {}%", 
                     mlResponse.getPredictedDelayMinutes(), 
                     mlResponse.getConfidenceScore() * 100);
            
            return mlResponse;
            
        } catch (Exception e) {
            log.error("Error predicting delay: ", e);
            throw new RuntimeException("Failed to predict train delay: " + e.getMessage());
        }
    }
    
    /**
     * Get prediction history for a train
     */
    public java.util.List<PredictionResponse> getPredictionHistory(Long trainId) {
        Train train = trainRepository.findById(trainId)
            .orElseThrow(() -> new RuntimeException("Train not found: " + trainId));
        
        return predictionRepository.findByTrainId(trainId)
            .stream()
            .map(p -> convertToResponse(p, train))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private PredictionResponse convertToResponse(Prediction prediction, Train train) {
        // Determine delay category
        String delayCategory = determineDelayCategory(prediction.getPredictedDelayMinutes());
        
        return PredictionResponse.builder()
            .trainId(train.getId())
            .trainNumber(train.getTrainNumber())
            .trainName(train.getTrainName())
            .predictedDelayMinutes(prediction.getPredictedDelayMinutes())
            .confidenceScore(prediction.getConfidenceScore())
            .delayCategory(delayCategory)
            .predictionTime(prediction.getPredictionDate())
            .build();
    }
    
    /**
     * Business logic: Categorize delay severity
     */
    private String determineDelayCategory(Integer delayMinutes) {
        if (delayMinutes <= 0) {
            return "ON_TIME";
        } else if (delayMinutes <= 15) {
            return "MILD_DELAY";
        } else if (delayMinutes <= 60) {
            return "MODERATE_DELAY";
        } else {
            return "SEVERE_DELAY";
        }
    }
}
```

---

## 🔗 FastAPI CLIENT

### File: `client/FastAPIClient.java`

```java
package com.traindelay.client;

import com.traindelay.dto.PredictionResponse;
import com.traindelay.model.Train;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * Client to communicate with FastAPI ML service
 * Wraps HTTP calls to Python backend
 * 
 * @Component makes this a Spring-managed singleton
 * Can be @Autowired in other components
 */
@Component
@Slf4j
public class FastAPIClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${fastapi.url:http://localhost:8000}")  // From application.properties
    private String fastApiUrl;
    
    /**
     * Call FastAPI service to predict delay
     * 
     * Sends train data to Python service:
     * POST http://localhost:8000/api/predict
     * Body: {
     *   "train_number": "12345",
     *   "origin": "Mumbai Central",
     *   "destination": "Delhi Junction",
     *   ...
     * }
     * 
     * Response:
     * {
     *   "predicted_delay_minutes": 25,
     *   "confidence_score": 0.87
     * }
     */
    public PredictionResponse predictDelay(Train train) {
        log.info("Calling FastAPI for train: {}", train.getTrainNumber());
        
        try {
            // Prepare request payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("train_number", train.getTrainNumber());
            requestBody.put("train_name", train.getTrainName());
            requestBody.put("train_type", train.getTrainType());
            requestBody.put("origin", train.getOriginStation().getStationName());
            requestBody.put("destination", train.getDestinationStation().getStationName());
            requestBody.put("departure_time", train.getDepartureTime().toString());
            requestBody.put("arrival_time", train.getArrivalTime().toString());
            
            // Call FastAPI service
            Map<String, Object> response = restTemplate.postForObject(
                fastApiUrl + "/api/predict",
                requestBody,
                Map.class
            );
            
            log.info("FastAPI response: {}", response);
            
            // Parse response
            return PredictionResponse.builder()
                .trainId(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .predictedDelayMinutes(((Number) response.get("predicted_delay_minutes")).intValue())
                .confidenceScore(((Number) response.get("confidence_score")).doubleValue())
                .predictionTime(java.time.LocalDateTime.now())
                .message("Prediction received from ML model")
                .build();
                
        } catch (Exception e) {
            log.error("Error calling FastAPI: {}", e.getMessage(), e);
            throw new RuntimeException("FastAPI service unavailable: " + e.getMessage());
        }
    }
}
```

### File: `config/WebClientConfig.java`

```java
package com.traindelay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST communication
 * Creates RestTemplate bean for making HTTP calls
 * 
 * @Configuration tells Spring this class has @Bean methods
 * @Bean methods create singleton beans
 */
@Configuration
public class WebClientConfig {
    
    /**
     * RestTemplate bean
     * Used by FastAPIClient to make HTTP requests
     * Automatically injected where needed
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

## ✅ SUMMARY: REPOSITORY & SERVICE

| Layer | Responsibility | Example |
|-------|-----------------|---------|
| **Repository** | Database queries only | `findByStationCode()`, `findById()` |
| **Service** | Business logic + validation | `searchTrains()`, `validateStations()` |
| **Controller** | HTTP handling | Will create in next phase |

**Data Flow:**
```
Controller → Service (validation) → Repository (query) → Database
                ↓
            Return formatted response
```

---

## 📚 NEXT PHASE

Create **Controller Layer**:
- REST endpoints (@RestController)
- Request mapping (@GetMapping, @PostMapping)
- Exception handling
- API documentation