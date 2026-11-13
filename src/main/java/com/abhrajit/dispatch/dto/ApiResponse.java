package com.abhrajit.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(String message, String status, List<String> errors) {

    public static ApiResponse success(String message) {
        return new ApiResponse(message, "success", null);
    }

    public static ApiResponse error(String message, List<String> errors) {
        return new ApiResponse(message, "error", errors == null || errors.isEmpty() ? null : errors);
    }
}
