package com.example.apicrowdsensing.views;

public class BaseResponse {
    private Object payload;
    private ErrorResponse errorResponse;

    public BaseResponse(Object payload, ErrorResponse errorResponse) {
        this.payload = payload;
        this.errorResponse = errorResponse;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
