package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.Park;
import com.example.apicrowdsensing.models.Track;
import com.example.apicrowdsensing.models.Point;
import com.example.apicrowdsensing.models.Visitas;
import com.example.apicrowdsensing.repositories.ParkRepository;
import com.example.apicrowdsensing.repositories.ViajeRepository;
import com.example.apicrowdsensing.services.CrowdsensingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
public class CrowdsensingController {
    private CrowdsensingService crowdsensingService;
    private ViajeRepository viajeRepository;
    private ParkRepository parkRepository;
    private String city = "";
    private List<Park> parks = new ArrayList<>();

    @Autowired
    public CrowdsensingController(CrowdsensingService crowdsensingService, ViajeRepository viajeRepository, ParkRepository parkRepository) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.viajeRepository = viajeRepository;
        this.parkRepository = parkRepository;
        this.crowdsensingService = crowdsensingService;
    }

    private JSONObject parseBoundsFromJsonResponse(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray elements = jsonObject.getJSONArray("elements");

        if (elements.length() > 0) {
            JSONObject firstElement = elements.getJSONObject(0);
            if (firstElement.has("bounds")) {
                return firstElement.getJSONObject("bounds");
            }
        }
        return null;
    }

    private JSONObject getBoundsCity(String city, String country) throws Exception {
        //String query = "[out:json];area[\"name\"=\"" + country + "\"]->.searchArea;relation(area.searchArea)[\"name\"=\"" + city + "\"][\"boundary\"=\"administrative\"];out geom;";
        String query = "[out:json];area['name'='Buenos Aires']->.searchArea;relation(area.searchArea)['name'='Ayacucho']['boundary'='administrative'];out geom;";

        String encodedQuery = encodeQuery(query);
        String url = "https://overpass-api.de/api/interpreter?data=" + encodedQuery;

        HttpResponse<String> response = Unirest.get(url).asString();

        if (response.getStatus() == 200) {
            String jsonResponse = response.getBody();
            return parseBoundsFromJsonResponse(jsonResponse);
        } else {
            throw new Exception("Error: " + response.getStatusText());
        }
    }

    private static String encodeQuery(String query) throws UnsupportedEncodingException {
        return URLEncoder.encode(query, "UTF-8");
    }
    @GetMapping("/query/city/{city}")
    public void getQuery(@PathVariable(value="city") String city) throws Exception {
        List<Park> parksByCity= parkRepository.findAllByCityAndDeletedIsFalse(city);

        if(!parksByCity.isEmpty()) {
            this.parks = parksByCity;
            this.city = city;
        }
        else {
            this.city = city;
            this.parks.clear();
            double minlat = 0;
            double minlon = 0;
            double maxlat = 0;
            double maxlon = 0;
            try {
                Unirest.setTimeouts(0, 0);

                try {
                    JSONObject bounds = getBoundsCity(city, "Argentina");
                    if (bounds != null) {
                        minlat = bounds.getDouble("minlat");
                        minlon = bounds.getDouble("minlon");
                        maxlat = bounds.getDouble("maxlat");
                        maxlon = bounds.getDouble("maxlon");
                    } else {
                        System.out.println("City or limits not found");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

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


                double lat = 0;
                double lon = 0;
                if (elementsArray != null && elementsArray.isArray()) {
                    Iterator<JsonNode> elementsIterator = elementsArray.elements();
                    while (elementsIterator.hasNext()) {
                        JsonNode element = elementsIterator.next();
                        if (element.has("geometry")) {
                            ArrayList<Point> nodes = new ArrayList<>();
                            JsonNode geometry = element.get("geometry");
                            JsonNode tags = element.get("tags");
                            var park = new Park();
                            park.setId(element.get("id").asLong());
                            String name = null;
                            if(tags.get("name") != null) {
                                name = tags.get("name").asText();
                            }
                            park.setCity(this.city);
                            if(name == null) {
                                name = "Plaza";
                            }
                            park.setName(name);
                            Iterator<JsonNode> geometryIterator = geometry.elements();
                            while(geometryIterator.hasNext()) {
                                JsonNode geometryElement = geometryIterator.next();
                                lat = geometryElement.get("lat").asDouble();
                                lon = geometryElement.get("lon").asDouble();
                                String point = geometryElement.get("lat") + "; " + geometryElement.get("lon");
                                park.addPoint(point);
                            }
                            if((minlat <= lat && maxlat >= lat) && (minlon <= lon && maxlon >= lon)) {
                                parks.add(park);
                                parkRepository.save(park);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/markers")
    public ResponseEntity<String> getMarkers(@RequestParam("initialDate") LocalDate initialDate,
                                             @RequestParam("finalDate") LocalDate finalDate) throws IOException {
        Path path = Paths.get("src", "main", "resources", "response.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(new File(path.toString()));
        JsonNode elementsArray = jsonNode.get("elements");

        LocalDateTime initialDateTime = initialDate.atStartOfDay();
        LocalDateTime finalDateTime = finalDate.atStartOfDay();

        ZonedDateTime initial_zdt = initialDateTime.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
        ZonedDateTime final_zdt = finalDateTime.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
        long initial_ms = initial_zdt.toInstant().toEpochMilli();
        long final_ms  = final_zdt.toInstant().toEpochMilli();
        List<Visitas> visitas = viajeRepository.findByStartTimeBetween(initial_ms, final_ms);
        //List<Park> parksByCity= parkRepository.findAllByCityAndDeletedIsFalse(city);
        List<Park> parksByCity= parkRepository.findAllByCity(city);
        return crowdsensingService.getMarkers(elementsArray, visitas, parksByCity);
    }

    @GetMapping("/delete/park")
    public void deletePark(@RequestParam("name") String name) {
        Park park = parkRepository.findByName(name);
        park.setDeleted(true);
        parkRepository.save(park);
    }

    @GetMapping("/update/park")
    public void updateParkName(@RequestParam("name") String name, @RequestParam("newName") String newName) {
        Park park = parkRepository.findByName(name);
        park.setName(newName);
        parkRepository.save(park);
    }
}
