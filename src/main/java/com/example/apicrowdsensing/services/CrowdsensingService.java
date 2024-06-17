package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.*;
import com.example.apicrowdsensing.repositories.ParkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class CrowdsensingService {

    @Autowired
    public CrowdsensingService() {}

    public ArrayList<DateTraffic> getTraficByPark(Park park, List<Visitas> visitas) {
        ArrayList<DateTraffic> l = new ArrayList<>();
        for(Visitas v: visitas) {
                String[] partes = v.getCenter().substring(1, v.getCenter().length() - 1).split(",");
                double primerCoordenada = Double.parseDouble(partes[0]);
                double segundoCoordenada = Double.parseDouble(partes[1]);
                if (pointInsidePoligon(new Point(primerCoordenada, segundoCoordenada), park.getPoints())) {
                    long start_time = v.getStartTime();
                    boolean contains = false;
                    LocalDate date = Instant.ofEpochMilli(start_time).atZone(ZoneId.systemDefault()).toLocalDate();
                    for (DateTraffic d : l) {
                        if (d.getDate().isEqual(date)) {
                            d.addTraffic();
                            contains = true;
                            break;
                        }
                    }
                    if (contains == false) {
                        l.add(new DateTraffic(date));
                    }
                }
        }
        return l;
    }

    public static boolean pointInsidePoligon(Point point, List<String> poligon) {
        int intersections = 0;
        int n = poligon.size();
        double x = point.getX();
        double y = point.getY();
        for (int i = 0; i < n; i++) {
            String[] partes1 = poligon.get(i).split("; ");
            Point p1 = new Point(Double.parseDouble(partes1[0]), Double.parseDouble(partes1[1]));
            String[] partes2 = poligon.get((i + 1) % n).split("; ");
            Point p2 = new Point(Double.parseDouble(partes2[0]), Double.parseDouble(partes2[1]));
            if ((p1.getY() <= y && y < p2.getY() || p2.getY() <= y && y < p1.getY())
                    && x < (p2.getX() - p1.getX()) * (y - p1.getY()) / (p2.getY() - p1.getY()) + p1.getX()) {
                intersections++;
            }
        }
        return intersections % 2 == 1;
    }

    public ResponseEntity<String> getMarkers(JsonNode elementsArray, List<Visitas> visitas, List<Park> parks, List<Park> parksCreated) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        while(parks.isEmpty()) {
            if(!parks.isEmpty())
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
                    String name = p.getName();
                    if (Math.toIntExact(p.getId()) == id && name != null) {
                        ArrayList<DateTraffic> traffic = getTraficByPark(p, visitas);
                        result.add(new NewFormatMarker(new double[]{lat, lon}, name, traffic));
                        break;
                    }
                }
            }
            for(Park p: parksCreated) {
                lat = 0;
                lon = 0;
                for (String punto : p.getPoints()) {
                    String[] partes = punto.split("; ");
                    double x = Double.parseDouble(partes[0]);
                    double y = Double.parseDouble(partes[1]);
                    lat += x;
                    lon += y;
                }
                int length = p.getPoints().size();
                lat /= length;
                lon /= length;
                String name = p.getName();
                ArrayList<DateTraffic> traffic = getTraficByPark(p, visitas);
                result.add(new NewFormatMarker(new double[]{lat, lon}, name, traffic));
            }
        }
        String json = mapper.writeValueAsString(result);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(json);
    }

}
