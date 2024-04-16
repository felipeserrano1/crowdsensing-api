package com.example.apicrowdsensing.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;

public class NewFormatMarker {
    private double[] geocode;
    private String name;
    private ArrayList<DateTraffic> traffic;

    public NewFormatMarker() {
    }

    public NewFormatMarker(double[] geocode, String name, ArrayList<DateTraffic> traffic) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.geocode = geocode;
        this.name = name;
        this.traffic = traffic;
    }

    public NewFormatMarker(double[] geocode) {
        this.geocode = geocode;
    }

    public double[] getGeocode() {
        return geocode;
    }

    public void setGeocode(double[] geocode) {
        this.geocode = geocode;
    }

    public ArrayList<DateTraffic> getTraffic() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return traffic;
    }

    public void setTraffic(ArrayList<DateTraffic> traffic) {
        this.traffic = traffic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
