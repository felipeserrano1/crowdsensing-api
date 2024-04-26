package com.example.apicrowdsensing.models;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Visitas {
    @Id
    private int id;

    @Column
    private int user_id;

    @Column(nullable = false)
    private String center;

    @Column(name = "start_time")
    private long startTime;

    @Column(nullable = false)
    private long end_time;


    public Visitas(Integer user_id, String center, long startTime, long end_time) {
        this.user_id = user_id;
        this.center = center;
        this.startTime = startTime;
        this.end_time = end_time;
    }

    public Visitas() {}

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getCenter() {
        return center;
    }

    public void setCenter(String center) {
        this.center = center;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }

    @Override
    public String toString() {
        return "Visitas{" +
                "user_id=" + user_id +
                ", center='" + center + '\'' +
                ", start_time=" + startTime +
                ", end_time=" + end_time +
                '}';
    }

    public Date getDate() {
        Date date = new Date(this.getStartTime());
        return date;
    }
}
