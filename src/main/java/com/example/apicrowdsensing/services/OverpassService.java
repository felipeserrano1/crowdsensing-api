package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.Point;
import com.example.apicrowdsensing.models.PublicSpace;
import com.example.apicrowdsensing.repositories.PublicSpaceRepository;

import com.example.apicrowdsensing.utils.CustomException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

@Service
public class OverpassService {
    private final Utils utils;
    private final PublicSpaceRepository publicSpaceRepository;
    protected JsonNode response;

    @Autowired
    public OverpassService(Utils utils, PublicSpaceRepository publicSpaceRepository) {
        this.utils = utils;
        this.publicSpaceRepository = publicSpaceRepository;
    }

    public void getOverpassQuery(String city, String tag) throws CustomException, UnirestException {
        var publicSpaces = publicSpaceRepository.findAllByCityAndDeletedIsFalseAndType(city, tag);
        if (!publicSpaces.isEmpty()) {
            throw new CustomException("Info already in bd", HttpStatus.OK);
        }
        double minlat = 0;
        double minlon = 0;
        double maxlat = 0;
        double maxlon = 0;
        Unirest.setTimeouts(0, 0);
        try {
            JSONObject bounds = getBoundsCity(city, "Argentina");
            if (bounds != null) {
                minlat = bounds.getDouble("minlat");
                minlon = bounds.getDouble("minlon");
                maxlat = bounds.getDouble("maxlat");
                maxlon = bounds.getDouble("maxlon");
            } else {
                throw new CustomException("City or limits not found", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

        String tagType = utils.getTag(tag);

        String reqBody = String.format(
                """
                        [out:json][timeout:25][maxsize:800000000];
                        area[name="%s"]->.searchArea;
                        nwr["%s"="%s"](area.searchArea);
                        out geom;""",
                city, tagType, tag
        );

        HttpResponse<String> requestOverpass = Unirest.post("https://overpass-api.de/api/interpreter")
                .header("Content-Type", "text/plain")
                .body(reqBody)
                .asString();

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(requestOverpass.getBody());
            this.response = jsonNode.get("elements");
        } catch (Exception e) {
            //e.printStackTrace();
        }

        savePublicSpaces(response, tag, minlat, maxlat, minlon, maxlon, city);
    }

    public void savePublicSpaces(JsonNode elementsArray, String tag, Double minlat, Double maxlat, Double minlon, Double maxlon, String city) {
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
                    var park = new PublicSpace();
                    long overpassId = element.get("id").asLong();
                    park.setOverpassId(overpassId);
                    var ps = publicSpaceRepository.findByOverpassId(park.getOverpassId());
                    if (ps != null) {
                        break;
                    }
                    String name = null;
                    if (tags.get("name") != null) {
                        name = tags.get("name").asText();
                    }
                    park.setCity(city);
                    park.setType(tag);
                    if (name == null) {
                        name = "Plaza";
                    }
                    park.setName(name);
                    Iterator<JsonNode> geometryIterator = geometry.elements();
                    while (geometryIterator.hasNext()) {
                        JsonNode geometryElement = geometryIterator.next();
                        lat = geometryElement.get("lat").asDouble();
                        lon = geometryElement.get("lon").asDouble();
                        String point = geometryElement.get("lat") + "; " + geometryElement.get("lon");
                        park.addPoint(point);
                    }
                    if ((minlat <= lat && maxlat >= lat) && (minlon <= lon && maxlon >= lon)) {

                        publicSpaceRepository.save(park);
                    }
                }
            }
        }
    }

    public JSONObject getBoundsCity(String city, String country) throws Exception {
        String req;
        if (city.equals("Ayacucho")) {
            req = "[out:json];area['name'='Buenos Aires']->.searchArea;relation(area.searchArea)['name'='Ayacucho']['boundary'='administrative'];out geom;";
        } else {
            req = String.format(
                    "[out:json];area[\"name\"=\"Argentina\"]->.searchArea;relation(area.searchArea)[\"name\"=\"%s\"][\"boundary\"=\"administrative\"];out geom;",
                    city
            );
        }
        String encodedQuery = URLEncoder.encode(req, StandardCharsets.UTF_8);
        String url = "https://overpass-api.de/api/interpreter?data=" + encodedQuery;
        HttpResponse<String> response = Unirest.get(url).asString();

        if (response.getStatus() == 200) {
            String jsonResponse = response.getBody();
            return utils.parseBoundsFromJsonResponse(jsonResponse);
        } else {
            throw new Exception("Error: " + response.getStatusText());
        }
    }
}
