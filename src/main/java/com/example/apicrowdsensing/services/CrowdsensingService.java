package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.NewFormatMarker;
import com.example.apicrowdsensing.models.Park;
import com.example.apicrowdsensing.models.Track;
import com.example.apicrowdsensing.models.Point;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

    public ResponseEntity<String> getMarkers(JsonNode elementsArray, ArrayList<Track> tracks, ArrayList<Park> parks) throws JsonProcessingException {
        List<NewFormatMarker> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
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
                    if (p.getId() == id) {
                        ArrayList<Object> popUp = new ArrayList<>();
                        if(tags.get("name") != null) {
                            popUp.add(tags.get("name").asText());
                        }
                        int trafic = getTraficByPark(p, tracks);
                        if(trafic > 0) {
                            popUp.add(trafic);
                            result.add(new NewFormatMarker(new double[]{lat, lon}, popUp));
                        }
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
