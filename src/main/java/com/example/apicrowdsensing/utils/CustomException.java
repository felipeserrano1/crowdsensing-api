package com.example.apicrowdsensing.utils;

import org.springframework.http.HttpStatus;


public class CustomException extends Exception {
    private HttpStatus status;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    @Override
    public String toString() {
        return "CustomException{" +
                "status=" + status +
                '}';
    }

    public HttpStatus getStatus() {
        return status;
    }
}

