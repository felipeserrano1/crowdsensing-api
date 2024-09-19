package com.example.apicrowdsensing.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.Arrays;

public class NewFormatMarker {
    private long id;
    private double[] geocode;
    private String name;
    private ArrayList<DateTraffic> traffic;

    public NewFormatMarker() {
    }

    public NewFormatMarker(long id, double[] geocode, String name, ArrayList<DateTraffic> traffic) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.id = id;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NewFormatMarker{" +
                "id=" + id +
                ", geocode=" + Arrays.toString(geocode) +
                ", name='" + name + '\'' +
                ", traffic=" + traffic +
                '}';
    }
}
