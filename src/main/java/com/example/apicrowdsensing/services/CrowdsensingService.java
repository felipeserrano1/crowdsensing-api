package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class CrowdsensingService {

    @Autowired
    public CrowdsensingService() {}

    public ArrayList<DateTraffic> getTraficByPark(Park park, ArrayList<Track> tracks, LocalDate initialDate, LocalDate finalDate) {
        ArrayList<DateTraffic> l = new ArrayList<>();
        for(Track p: tracks) {
            LocalDate date = p.getDate();
            if ((date.isBefore(finalDate) || date.isEqual(finalDate)) && (date.isAfter(initialDate) || date.isEqual(initialDate)) && (pointInsidePoligon(p.getPoint(), park.getPoints()))){
                boolean contains = false;
                for(DateTraffic d: l) {
                    if(d.getDate().equals(date)) {
                        d.addTraffic();
                        contains = true;
                        break;
                    }
                }
                if(contains == false) {
                    l.add(new DateTraffic(date));
                }
            }
        }
        return l;
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

    public ResponseEntity<String> getMarkers(JsonNode elementsArray, ArrayList<Track> tracks, ArrayList<Park> parks, LocalDate initialDate, LocalDate finalDate) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        while(parks.isEmpty()) {
            if(parks != null)
                break;
        }
        List<NewFormatMarker> result = new ArrayList<>();
        if (elementsArray != null && elementsArray.isArray()) {
            Iterator<JsonNode> elementsIterator = elementsArray.elements();
            int id;
            double lat, lon;
            while (elementsIterator.hasNext()) {
                JsonNode element = elementsIterator.next();

                JsonNode tags = element.get("tags");
                id = element.get("id").asInt();
                if (element.has("bounds")) {
                    ArrayList<Point> nodes = new ArrayList<>();
                    JsonNode bounds = element.get("bounds");
                    double minlat = bounds.get("minlat").asDouble();
                    double maxlat = bounds.get("maxlat").asDouble();
                    double minlon = bounds.get("minlon").asDouble();
                    double maxlon = bounds.get("maxlon").asDouble();

                    lat = (minlat + maxlat) / 2;
                    lon = (minlon + maxlon) / 2;

                } else {
                    lat = element.get("lat").asDouble();
                    lon = element.get("lon").asDouble();
                }
                for(Park p: parks) {
                    String name = null;
                    if (p.getId() == id) {
                        ArrayList<Object> popUp = new ArrayList<>();
                        if(tags.get("name") != null) {
                            name = tags.get("name").asText();
                        }
                        ArrayList<DateTraffic> traffic = getTraficByPark(p, tracks, initialDate, finalDate);
                        result.add(new NewFormatMarker(new double[]{lat, lon}, name, traffic));
                        break;
                    }
                }
            }
        }
        String json = mapper.writeValueAsString(result);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(json);
    }

}
