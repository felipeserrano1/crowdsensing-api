package com.example.apicrowdsensing.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
//@Table(name = "parques")
public class Park {
    @Id
    //@GeneratedValue
    private long id;

    private String name, city;

    @ElementCollection
    //@CollectionTable(name = "park_points", joinColumns = @JoinColumn(name = "id"))
    private List<String> points = new ArrayList<>();

    private boolean deleted = false;
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

    //
//
//    public Park(int id) {
//        this.id = id;
//        this.points = new ArrayList<>();
//    }
//
//    public Park() {
//
//    }
//
////    public ArrayList<Point> getPoints() {
////        return points;
////    }
////
////    public int getId() {
////        return id;
////    }
////
////    public void addPoint(Point p) {
////        this.points.add(p);
////    }
//


    @Override
    public String toString() {
        return "Park{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", points=" + points +
                ", deleted=" + deleted +
                '}';
    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Park park = (Park) o;
//        return id == park.id;
//    }

}
