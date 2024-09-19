package com.example.apicrowdsensing.views.responses;

public class ErrorResponse {
    private Exception exception;

    public ErrorResponse(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "exception=" + exception +
                '}';
    }
}
