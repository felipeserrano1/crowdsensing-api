package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.Park;
import com.example.apicrowdsensing.models.Track;
import com.example.apicrowdsensing.models.Point;
import com.example.apicrowdsensing.services.CrowdsensingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

@RestController
public class CrowdsensingController {
    private CrowdsensingService crowdsensingService;
    private String city;
    private ArrayList<Park> parks = new ArrayList<>();
    private String query =
            "[out:json][timeout:25];\n" +
            "area[name=\"" + this.city + "\"]->.searchArea;\n" +
            "nwr[\"leisure\"=\"park\"](area.searchArea);\n" +
            "out geom;";

    @Autowired
    public CrowdsensingController(CrowdsensingService crowdsensingService) {
        this.crowdsensingService = crowdsensingService;
    }

    @GetMapping("/query/city/{city}")
    public void getQuery(@PathVariable(value="city") String city) throws Exception {
        this.city = city;
        this.parks.clear();
        try {
            Unirest.setTimeouts(0, 0);

            HttpResponse<String> response = Unirest.post("https://overpass-api.de/api/interpreter")
                    .header("Content-Type", "text/plain")
                    .body("[out:json][timeout:25];\n" +
                            "area[name=\"" + this.city + "\"]->.searchArea;\n" +
                            "nwr[\"leisure\"=\"park\"](area.searchArea);\n" +
                            "out geom;")
                    .asString();

            Path path = Paths.get("src", "main", "resources", "response.json");
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                Object json = objectMapper.readValue(response.getBody().toString(), Object.class);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(path.toString()), json);

            } catch (Exception e) {
                e.printStackTrace();
            }

            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(new File(path.toString()));
            JsonNode elementsArray = jsonNode.get("elements");


            
            if (elementsArray != null && elementsArray.isArray()) {
                Iterator<JsonNode> elementsIterator = elementsArray.elements();
                while (elementsIterator.hasNext()) {
                    JsonNode element = elementsIterator.next();
                    if (element.has("geometry")) {
                        ArrayList<Point> nodes = new ArrayList<>();
                        JsonNode geometry = element.get("geometry");
                        var park = new Park(element.get("id").asInt());
                        Iterator<JsonNode> geometryIterator = geometry.elements();
                        while(geometryIterator.hasNext()) {
                            JsonNode geometryElement = geometryIterator.next();
                            double lat = geometryElement.get("lat").asDouble();
                            double lon = geometryElement.get("lon").asDouble();
                            var p = new Point(lon, lat);
                            park.addPoint(p);
                        }
                        parks.add(park);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/traffic/id/{id}")
    public int getTraficByPark(@PathVariable(value="id") int parkId) throws Exception {
        for (Park p : parks) {
                if (p.getId() == parkId) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new JavaTimeModule());
                    try {
                        Path path = Paths.get("src", "main", "resources", "tracks.json");
                        File file = new File(path.toString());
                        ArrayList<Track> tracks = new ArrayList<>();
                        Track[] objetos = objectMapper.readValue(file, Track[].class);
                        for(Track t: objetos) {
                            tracks.add(t);
                        }
                        return crowdsensingService.getTraficByPark(p, tracks);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
        return -1;
    }
}
