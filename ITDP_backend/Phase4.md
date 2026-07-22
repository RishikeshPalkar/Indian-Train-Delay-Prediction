# 🎮 PHASE 4: CONTROLLER LAYER & EXCEPTION HANDLING

## 🎯 LEARNING GOALS

In this phase you'll learn:
1. **REST Controllers** - Handle HTTP requests/responses
2. **Request mapping** - URL routing (@GetMapping, @PostMapping)
3. **Status codes** - Proper HTTP response codes
4. **Exception handling** - Global error responses
5. **Input validation** - Prevent bad data

---

## 📡 HTTP REQUEST/RESPONSE CYCLE

```
Client (Browser/Mobile)
    ↓
[HTTP Request]
GET /api/trains/search?origin=CSTM&destination=NDLS
    ↓
Spring Boot Controller
    ↓
Service Layer (Business Logic)
    ↓
Repository (Database Query)
    ↓
[HTTP Response]
Status: 200 OK
Body: { "success": true, "data": [...] }
    ↓
Client receives JSON
```

---

## 🎮 CONTROLLER LAYER

**What:** Handles HTTP requests and returns responses
**How:** Uses @RestController annotation
**Why:** Separates HTTP concerns from business logic

### File: `controller/TrainController.java`

```java
package com.traindelay.controller;

import com.traindelay.dto.*;
import com.traindelay.service.TrainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for train operations
 * 
 * @RestController = @Controller + @ResponseBody
 * Returns JSON responses, not HTML views
 * 
 * @RequestMapping("/api/trains")
 * Base URL for all endpoints in this controller
 * All methods will have /api/trains prefix
 * 
 * @Validated
 * Enable validation of request parameters
 */
@RestController
@RequestMapping("/api/trains")
@Validated
@Slf4j
public class TrainController {
    
    @Autowired
    private TrainService trainService;
    
    /**
     * Endpoint: GET /api/trains/search
     * 
     * Search trains between two stations
     * 
     * URL: GET /api/trains/search?origin=CSTM&destination=NDLS
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "data": [
     *     {
     *       "id": 1,
     *       "trainNumber": "12345",
     *       "trainName": "Rajdhani Express",
     *       "originStationCode": "CSTM",
     *       "originStationName": "Mumbai Central",
     *       "destinationStationCode": "NDLS",
     *       "destinationStationName": "New Delhi",
     *       "departureTime": "15:00:00",
     *       "arrivalTime": "07:00:00",
     *       "totalSeats": 120
     *     }
     *   ],
     *   "message": "Found 3 trains",
     *   "timestamp": "2024-07-22T10:30:00"
     * }
     * 
     * @RequestParam - Extract query parameters from URL
     * @Valid - Validate the TrainSearchRequest object
     * ResponseEntity - Control HTTP status code and body
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TrainResponse>>> searchTrains(
        @RequestParam(name = "origin") String originCode,
        @RequestParam(name = "destination") String destinationCode
    ) {
        log.info("Train search request: {} -> {}", originCode, destinationCode);
        
        // Create request object
        TrainSearchRequest request = TrainSearchRequest.builder()
            .originStationCode(originCode)
            .destinationStationCode(destinationCode)
            .build();
        
        // Call service
        List<TrainResponse> trains = trainService.searchTrains(request);
        
        // Return success response
        return ResponseEntity.ok(
            ApiResponse.success(
                trains,
                "Found " + trains.size() + " trains",
                200
            )
        );
    }
    
    /**
     * Alternative: POST /api/trains/search
     * 
     * Sometimes better to use POST for search with complex filters
     * 
     * Request body (JSON):
     * {
     *   "originStationCode": "CSTM",
     *   "destinationStationCode": "NDLS"
     * }
     * 
     * @RequestBody - Parse JSON body into TrainSearchRequest object
     * @Valid - Validate annotations in the DTO
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<TrainResponse>>> searchTrainsPost(
        @Valid @RequestBody TrainSearchRequest request
    ) {
        log.info("Train search request (POST): {}", request);
        
        List<TrainResponse> trains = trainService.searchTrains(request);
        
        return ResponseEntity.ok(
            ApiResponse.success(
                trains,
                "Found " + trains.size() + " trains",
                200
            )
        );
    }
    
    /**
     * Endpoint: GET /api/trains/{id}
     * 
     * Get train details by ID
     * 
     * URL: GET /api/trains/1
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "data": {
     *     "id": 1,
     *     "trainNumber": "12345",
     *     ...
     *   },
     *   "message": "Train fetched successfully",
     *   "timestamp": "2024-07-22T10:30:00"
     * }
     * 
     * Response: 404 Not Found
     * {
     *   "success": false,
     *   "statusCode": 404,
     *   "error": "Train not found with ID: 999",
     *   "timestamp": "2024-07-22T10:30:00"
     * }
     * 
     * @PathVariable - Extract ID from URL path
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainResponse>> getTrainById(
        @PathVariable(name = "id") Long trainId
    ) {
        log.info("Fetching train with ID: {}", trainId);
        
        TrainResponse train = trainService.getTrainById(trainId);
        
        return ResponseEntity.ok(
            ApiResponse.success(
                train,
                "Train fetched successfully",
                200
            )
        );
    }
    
    /**
     * Endpoint: GET /api/trains
     * 
     * Get all trains (with pagination)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<String>> getAllTrains() {
        log.info("Fetching all trains");
        
        return ResponseEntity.ok(
            ApiResponse.success(
                "This endpoint needs pagination implementation",
                "Use /api/trains?page=0&size=10",
                200
            )
        );
    }
}
```

### File: `controller/PredictionController.java`

```java
package com.traindelay.controller;

import com.traindelay.dto.ApiResponse;
import com.traindelay.dto.PredictionResponse;
import com.traindelay.service.PredictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST Controller for ML predictions
 */
@RestController
@RequestMapping("/api/predictions")
@Slf4j
public class PredictionController {
    
    @Autowired
    private PredictionService predictionService;
    
    /**
     * Endpoint: POST /api/predictions/predict/{trainId}
     * 
     * Predict delay for a specific train
     * 
     * URL: POST /api/predictions/predict/1
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "data": {
     *     "trainId": 1,
     *     "trainNumber": "12345",
     *     "trainName": "Rajdhani Express",
     *     "predictedDelayMinutes": 25,
     *     "confidenceScore": 0.87,
     *     "delayCategory": "MODERATE_DELAY",
     *     "predictionTime": "2024-07-22T10:30:00",
     *     "message": "Prediction received from ML model"
     *   },
     *   "message": "Prediction successful",
     *   "timestamp": "2024-07-22T10:30:00"
     * }
     * 
     * Status Codes:
     * - 200 OK: Prediction successful
     * - 404 Not Found: Train not found
     * - 503 Service Unavailable: FastAPI service down
     */
    @PostMapping("/predict/{trainId}")
    public ResponseEntity<ApiResponse<PredictionResponse>> predictTrainDelay(
        @PathVariable(name = "trainId") Long trainId
    ) {
        log.info("Prediction request for train ID: {}", trainId);
        
        try {
            PredictionResponse prediction = predictionService.predictTrainDelay(trainId);
            
            return ResponseEntity.ok(
                ApiResponse.success(
                    prediction,
                    "Prediction successful",
                    200
                )
            );
        } catch (RuntimeException e) {
            log.error("Prediction failed: {}", e.getMessage());
            throw e;  // Let global exception handler catch it
        }
    }
    
    /**
     * Endpoint: GET /api/predictions/history/{trainId}
     * 
     * Get prediction history for a train
     * 
     * URL: GET /api/predictions/history/1
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "data": [
     *     {
     *       "trainId": 1,
     *       "trainNumber": "12345",
     *       "predictedDelayMinutes": 25,
     *       "confidenceScore": 0.87,
     *       ...
     *     },
     *     {
     *       "trainId": 1,
     *       "trainNumber": "12345",
     *       "predictedDelayMinutes": 30,
     *       "confidenceScore": 0.89,
     *       ...
     *     }
     *   ],
     *   "message": "Found 2 predictions",
     *   "timestamp": "2024-07-22T10:30:00"
     * }
     */
    @GetMapping("/history/{trainId}")
    public ResponseEntity<ApiResponse<List<PredictionResponse>>> getPredictionHistory(
        @PathVariable(name = "trainId") Long trainId
    ) {
        log.info("Fetching prediction history for train ID: {}", trainId);
        
        List<PredictionResponse> history = predictionService.getPredictionHistory(trainId);
        
        return ResponseEntity.ok(
            ApiResponse.success(
                history,
                "Found " + history.size() + " predictions",
                200
            )
        );
    }
}
```

---

## ⚠️ EXCEPTION HANDLING

**Why needed:** Transform server errors into user-friendly JSON responses

```
Bad Request (400)
{
  "success": false,
  "statusCode": 400,
  "error": "Validation failed: Origin station code cannot be blank",
  "timestamp": "2024-07-22T10:30:00"
}

Server Error (500)
{
  "success": false,
  "statusCode": 500,
  "error": "An unexpected error occurred. Please try again later.",
  "timestamp": "2024-07-22T10:30:00"
}
```

### File: `exception/TrainNotFoundException.java`

```java
package com.traindelay.exception;

/**
 * Custom exception for when train is not found
 * Extends RuntimeException so it doesn't need try-catch everywhere
 */
public class TrainNotFoundException extends RuntimeException {
    
    public TrainNotFoundException(String message) {
        super(message);
    }
    
    public TrainNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### File: `exception/StationNotFoundException.java`

```java
package com.traindelay.exception;

public class StationNotFoundException extends RuntimeException {
    
    public StationNotFoundException(String message) {
        super(message);
    }
    
    public StationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### File: `exception/GlobalExceptionHandler.java`

```java
package com.traindelay.exception;

import com.traindelay.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * 
 * @ControllerAdvice
 * Applies exception handling to ALL controllers
 * Centralized error response formatting
 * 
 * Similar to try-catch for entire application
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle TrainNotFoundException
     * Catches all TrainNotFoundException thrown in the app
     */
    @ExceptionHandler(TrainNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)  // Return 404 status
    public ResponseEntity<ApiResponse<Object>> handleTrainNotFound(
        TrainNotFoundException ex,
        WebRequest request
    ) {
        log.warn("Train not found: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage(), 404));
    }
    
    /**
     * Handle StationNotFoundException
     */
    @ExceptionHandler(StationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<Object>> handleStationNotFound(
        StationNotFoundException ex,
        WebRequest request
    ) {
        log.warn("Station not found: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage(), 404));
    }
    
    /**
     * Handle validation errors (@Valid annotation)
     * Triggered when @Valid fails on request body
     * 
     * Example error:
     * {
     *   "success": false,
     *   "statusCode": 400,
     *   "error": "Validation failed: originStationCode: Origin station code cannot be blank",
     *   "timestamp": "2024-07-22T10:30:00"
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // Return 400 status
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
        MethodArgumentNotValidException ex,
        WebRequest request
    ) {
        log.warn("Validation error occurred");
        
        // Extract all validation errors
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String errorMessage = "Validation failed: " + errors.toString();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorMessage, 400));
    }
    
    /**
     * Handle FastAPI connection errors
     * Thrown when ML service is unavailable
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(
        RuntimeException ex,
        WebRequest request
    ) {
        log.error("Runtime exception occurred: ", ex);
        
        // Check if it's FastAPI related
        if (ex.getMessage() != null && ex.getMessage().contains("FastAPI")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                    "ML prediction service is temporarily unavailable. Please try again later.",
                    503
                ));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(
                "An unexpected error occurred. Please try again later.",
                500
            ));
    }
    
    /**
     * Catch-all for any unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        log.error("Unexpected error: ", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(
                "An unexpected error occurred. Please contact support.",
                500
            ));
    }
}
```

---

## 📝 HTTP STATUS CODES REFERENCE

| Code | Meaning | When to Use |
|------|---------|------------|
| **2xx Success** | | |
| 200 OK | Request successful | Successful GET, POST, PUT |
| 201 Created | Resource created | Successful POST creating resource |
| 204 No Content | No response body | Successful DELETE |
| **4xx Client Error** | | |
| 400 Bad Request | Invalid input | Validation failure |
| 401 Unauthorized | Need authentication | Not logged in |
| 403 Forbidden | Access denied | Insufficient permissions |
| 404 Not Found | Resource doesn't exist | Train/Station not found |
| 409 Conflict | Data conflict | Duplicate entry |
| **5xx Server Error** | | |
| 500 Internal Server Error | Server bug | Unexpected exception |
| 503 Service Unavailable | Service down | Database/FastAPI down |

---

## 🧪 TEST YOUR ENDPOINTS

Using cURL:

```bash
# Search trains (GET with query params)
curl -X GET "http://localhost:8080/api/trains/search?origin=CSTM&destination=NDLS" \
  -H "Content-Type: application/json"

# Get train by ID (GET with path param)
curl -X GET "http://localhost:8080/api/trains/1" \
  -H "Content-Type: application/json"

# Predict delay (POST)
curl -X POST "http://localhost:8080/api/predictions/predict/1" \
  -H "Content-Type: application/json"

# Get prediction history
curl -X GET "http://localhost:8080/api/predictions/history/1" \
  -H "Content-Type: application/json"
```

Using Postman:
1. Import collection
2. Set base URL: `http://localhost:8080`
3. Test each endpoint
4. View responses

---

## 🔐 REQUEST VALIDATION

### File: `dto/TrainSearchRequest.java` (Updated)

```java
package com.traindelay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainSearchRequest {
    
    @NotBlank(message = "Origin station code is required")
    @Size(min = 3, max = 10, message = "Station code must be 3-10 characters")
    private String originStationCode;
    
    @NotBlank(message = "Destination station code is required")
    @Size(min = 3, max = 10, message = "Station code must be 3-10 characters")
    private String destinationStationCode;
}
```

**Validation Annotations:**
- `@NotNull` - Field cannot be null
- `@NotBlank` - String cannot be blank
- `@Size(min, max)` - String/Collection size
- `@Min`, `@Max` - Number range
- `@Email` - Valid email format
- `@Pattern(regexp="...")` - Regex matching

---

## 📊 API ENDPOINT SUMMARY

| Method | URL | Purpose | Status Codes |
|--------|-----|---------|-------------|
| GET | `/api/trains/search` | Search trains | 200, 400, 404 |
| POST | `/api/trains/search` | Search trains (POST) | 200, 400, 404 |
| GET | `/api/trains/{id}` | Get train details | 200, 404 |
| GET | `/api/trains` | List all trains | 200 |
| POST | `/api/predictions/predict/{trainId}` | Predict delay | 200, 404, 503 |
| GET | `/api/predictions/history/{trainId}` | Get prediction history | 200, 404 |

---

## ✅ CHECKLIST FOR THIS PHASE

- [ ] Created TrainController.java
- [ ] Created PredictionController.java
- [ ] Created custom exceptions
- [ ] Created GlobalExceptionHandler
- [ ] Updated DTOs with validation
- [ ] Tested endpoints with cURL/Postman
- [ ] Verified error responses

---

## 🎓 KEY LEARNING POINTS

1. **@RestController** - REST API handler
2. **@GetMapping, @PostMapping** - HTTP method routing
3. **@PathVariable** - Extract from URL path
4. **@RequestParam** - Extract from query string
5. **@RequestBody** - Extract from request body
6. **ResponseEntity** - Control status code + body
7. **@Valid** - Input validation
8. **@ControllerAdvice** - Global exception handling
9. **ApiResponse wrapper** - Consistent response format

---

## 🚀 NEXT PHASE

Implement:
1. **Database seeding** - Initial train & station data
2. **Testing** - Unit & integration tests
3. **Docker setup** - Containerization
4. **FastAPI integration** - Connect Python service