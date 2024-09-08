package com.example.apicrowdsensing.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public String getPayloadAsString(Object payload) {
        return payload instanceof String ? (String) payload : null;
    }

    public String formatPayloadToString(Object payloadJSON) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(this.getPayloadAsString(payloadJSON));
        String payload = rootNode.path("payload").asText();
        return payload;
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "payload=" + payload +
                ", errorResponse=" + errorResponse +
                '}';
    }
}
