package com.example.apicrowdsensing.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class Path {
    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;

    @JsonProperty("punto")
    private Punto punto;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime dateTime;
    //private ArrayList<Punto> path;

    public Path() {
    }

    public Path(int id, String name, LocalDateTime dateTime, Punto punto) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.punto = punto;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Punto getPunto() {
        return punto;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPunto(Punto punto) {
        this.punto = punto;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
}