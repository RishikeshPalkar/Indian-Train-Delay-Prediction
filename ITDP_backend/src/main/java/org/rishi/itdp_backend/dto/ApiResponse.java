package org.rishi.itdp_backend.dto;


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