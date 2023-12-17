package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.Park;
import com.example.apicrowdsensing.models.Track;
import com.example.apicrowdsensing.models.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;

@Service
public class CrowdsensingService {

    @Autowired
    public CrowdsensingService() {}

    public int getTraficByPark(Park park, ArrayList<Track> tracks) {
        int i = 0;
        LocalDate localDate = LocalDate.now();
        for(Track p: tracks) {
            LocalDate date = p.getDate();
            if (localDate.isEqual(date) && (pointInsidePoligon(p.getPoint(), park.getPoints()))){
                i++;
                System.out.println(p.getName());
            }
        }
        return i;
    }

    public static boolean pointInsidePoligon(Point point, ArrayList<Point> poligon) {
        int intersections = 0;
        int n = poligon.size();
        double x = point.getX();
        double y = point.getY();
        for (int i = 0; i < n; i++) {
            Point p1 = poligon.get(i);
            Point p2 = poligon.get((i + 1) % n);
            if ((p1.getY() <= y && y < p2.getY() || p2.getY() <= y && y < p1.getY())
                    && x < (p2.getX() - p1.getX()) * (y - p1.getY()) / (p2.getY() - p1.getY()) + p1.getX()) {
                intersections++;
            }
        }
        return intersections % 2 == 1;
    }
}
