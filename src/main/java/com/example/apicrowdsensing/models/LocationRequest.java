package com.example.apicrowdsensing.models;

import java.util.List;

public class LocationRequest {
    private String name, city, type;
    private List<List<String>> positions;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<List<String>> getPositions() {
        return positions;
    }

    public void setPositions(List<List<String>> positions) {
        this.positions = positions;
    }
}
