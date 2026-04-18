package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private String error;
    private List<String> details;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public static ErrorResponse validationError(List<String> details) {
        return new ErrorResponse("Ошибка валидации", details);
    }

    public static ErrorResponse messageError(String message) {
        return new ErrorResponse(message);
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }
}