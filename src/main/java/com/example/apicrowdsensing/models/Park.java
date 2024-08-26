package com.example.apicrowdsensing.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "park")
public class Park {
    @Id
    private long id;

    private String name, city, type;

    @ElementCollection
    private List<String> points = new ArrayList<>();

    private boolean deleted = false;

    private boolean created = false;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public List<String> getPoints() {
        return points;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void addPoint(String s) {
        getPoints().add(s);
    }
    public void setPoints(List<String> points) {
        this.points = points;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }




    @Override
    public String toString() {
        return "Park{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", points=" + points +
                ", deleted=" + deleted +
                ", type=" + type +
                '}';
    }
}
