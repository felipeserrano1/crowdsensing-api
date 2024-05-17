package com.example.apicrowdsensing.models;

import java.time.LocalDate;

public class DateTraffic {

    private LocalDate date;
    private int traffic = 1;

    public DateTraffic(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTraffic() {
        return traffic;
    }

    public void setTraffic(int traffic) {
        this.traffic = traffic;
    }

    public void addTraffic() {
        this.traffic++;
    }


    @Override
    public String toString() {
        return "DateTraffic{" +
                "date=" + date +
                ", traffic=" + traffic +
                '}';
    }
}
