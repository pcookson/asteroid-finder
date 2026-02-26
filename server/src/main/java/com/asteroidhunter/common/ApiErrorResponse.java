package com.asteroidhunter.common;

public record ApiErrorResponse(
        String error,
        String message,
        Integer status) {
}
