package com.example.apicrowdsensing.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class Track {
    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("point")
    private Point point;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;
    //private ArrayList<Point> path;

    public Track() {
    }

    public Track(int id, String name, Point point, LocalDate date) {
        this.id = id;
        this.name = name;
        this.point = point;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Point getPoint() {
        return point;
    }

    public LocalDate getDate() {
        return date;
    }
}