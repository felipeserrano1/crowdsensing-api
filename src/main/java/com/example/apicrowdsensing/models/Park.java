package com.example.apicrowdsensing.models;

import java.util.ArrayList;

public class Park {
    private int id;
    private ArrayList<Point> points;
    //private String name;

    public Park(int id) {
        this.id = id;
        this.points = new ArrayList<>();
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    public int getId() {
        return id;
    }

    public void addPoint(Point p) {
        this.points.add(p);
    }

    @Override
    public String toString() {
        return "Park{" +
                "id=" + id +
                ", points=" + points +
                '}';
    }
}
