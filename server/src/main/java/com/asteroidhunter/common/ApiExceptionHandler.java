package com.asteroidhunter.common;

import com.asteroidhunter.nasa.NeoWsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NeoWsException.class)
    public ResponseEntity<ApiErrorResponse> handleNeoWsException(NeoWsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(
                        "NASA_NEO_WS_ERROR",
                        "NASA NeoWs request failed",
                        ex.getStatus()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("NASA_API_KEY")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(
                            "CONFIG_ERROR",
                            "NASA_API_KEY is not configured",
                            null));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        "INTERNAL_ERROR",
                        "Unexpected error",
                        null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        "INTERNAL_ERROR",
                        "Unexpected error",
                        null));
    }
}
