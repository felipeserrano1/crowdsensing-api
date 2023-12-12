package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.Path;
import com.example.apicrowdsensing.models.Punto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CrowdsensingService {

    @Autowired
    public CrowdsensingService() {
    }

    public boolean getPeople(Path path) {
        var p1 = new Punto(0,0);
        var p2 = new Punto(5,0);
        var p3 = new Punto(0,5);
        var p4 = new Punto(5,5);
        LocalDate date1 = path.getDateTime().toLocalDate();
        LocalDate date2 = LocalDateTime.now().toLocalDate();
        int comparisonResult = date1.compareTo(date2);


        if(comparisonResult == 0) {
            // encontrar zona
            return path.getPunto().getX() >= p1.getX() && path.getPunto().getX() <= p2.getX() && path.getPunto().getY() >= p1.getY() && path.getPunto().getY() <= p4.getY();
        }
        return false;
    }
}
