package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.*;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@RestController
public class CrowdsensingController {
    private CrowdsensingService crowdsensingService;
    private ViajeRepository viajeRepository;
    private ParkRepository parkRepository;
    private String city = "";
    private List<Park> parks = new ArrayList<>();

    List<String> highwayTags = new ArrayList<>(Arrays.asList("motorway", "trunk", "primary", "secondary","tertiary", "unclassified", "residential","service", "footway", "cycleway","path", "track", "pedestrian"));
    List<String> railwayTags = new ArrayList<>(Arrays.asList("rail", "light_rail", "subway", "tram", "narrow_gauge", "funicular", "monorail"));
    List<String> aerowayTags = new ArrayList<>(Arrays.asList("aerodrome", "runway", "taxiway", "helipad"));
    List<String> amenityTags = new ArrayList<>(Arrays.asList("restaurant", "bar", "cafe", "hospital", "school", "kindergarten", "university", "library", "bank", "atm", "parking", "fuel", "police", "fire_station", "post_office", "toilets", "theatre",  "cinema", "pharmacy", "marketplace"));
    List<String> shopTags = new ArrayList<>(Arrays.asList("supermarket", "bakery", "butcher", "clothes", "convenience", "electronics", "furniture", "hardware", "jewelry", "mall", "optician", "sports", "toys"));
    List<String> leisureTags = new ArrayList<>(Arrays.asList("park", "playground", "sports_centre", "swimming_pool", "stadium", "golf_course", "marina", "garden", "dog_park"));
    List<String> tourismTags = new ArrayList<>(Arrays.asList("hotel", "motel", "guest_house", "hostel", "camp_site", "caravan_site", "chalet", "alpine_hut", "information", "museum", "zoo", "theme_park", "viewpoint"));
    List<String> landuseTags = new ArrayList<>(Arrays.asList("residential", "commercial", "industrial", "forest", "farmland", "meadow","vineyard", "orchard", "cemetery", "military", "recreation_ground"));
    List<String> naturalTags = new ArrayList<>(Arrays.asList("wood", "water", "wetland", "beach", "cliff", "rock", "scrub", "sand", "heath", "peak", "volcano"));
    List<String> buildingTags = new ArrayList<>(Arrays.asList("yes", "residential", "commercial", "industrial", "church", "school", "hospital", "apartments", "house", "detached", "terrace", "warehouse", "barn"));
    List<String> manMadeTags = new ArrayList<>(Arrays.asList("tower", "chimney", "water_tower", "lighthouse", "communications_tower"));
    List<String> historicTags = new ArrayList<>(Arrays.asList("castle", "fort", "ruins", "archaeological_site", "monument", "memorial", "battlefield", "wayside_cross"));
    List<String> powerTags = new ArrayList<>(Arrays.asList("plant", "generator", "substation","tower", "line"));
    List<String> pipelineTags = new ArrayList<>(Arrays.asList("oil", "gas", "water"));
    List<String> boundaryTags = new ArrayList<>(Arrays.asList("administrative", "national_park","protected_area"));
    List<String> barrierTags = new ArrayList<>(Arrays.asList("fence", "wall", "hedge", "gate"));
    List<String> waterwayTags = new ArrayList<>(Arrays.asList("river", "stream", "canal","drain", "ditch"));

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
        String query = null;
        if(city.equals("Ayacucho")) {
            query = "[out:json];area['name'='Buenos Aires']->.searchArea;relation(area.searchArea)['name'='Ayacucho']['boundary'='administrative'];out geom;";
        } else {
            query = "[out:json];area[\"name\"=\"Argentina\"]->.searchArea;relation(area.searchArea)[\"name\"=\"" + city + "\"][\"boundary\"=\"administrative\"];out geom;";
        }

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
    @GetMapping("/query/city")
    public void getQuery(@RequestParam("city") String city, @RequestParam("tag") String tag) throws Exception {
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

                String tagType = null;
                if (highwayTags.contains(tag)) {
                    tagType = "highway";
                } else if (railwayTags.contains(tag)) {
                    tagType = "railway";
                } else if (aerowayTags.contains(tag)) {
                    tagType = "aeroway";
                } else if (amenityTags.contains(tag)) {
                    tagType = "amenity";
                } else if (shopTags.contains(tag)) {
                    tagType = "shop";
                } else if (leisureTags.contains(tag)) {
                    tagType = "leisure";
                } else if (tourismTags.contains(tag)) {
                    tagType = "tourism";
                } else if (landuseTags.contains(tag)) {
                    tagType = "landuse";
                } else if (naturalTags.contains(tag)) {
                    tagType = "natural";
                } else if (buildingTags.contains(tag)) {
                    tagType = "building";
                } else if (manMadeTags.contains(tag)) {
                    tagType = "manMade";
                } else if (historicTags.contains(tag)) {
                    tagType = "historic";
                } else if (powerTags.contains(tag)) {
                    tagType = "power";
                } else if (pipelineTags.contains(tag)) {
                    tagType = "pipeline";
                } else if (boundaryTags.contains(tag)) {
                    tagType = "boundary";
                } else if (barrierTags.contains(tag)) {
                    tagType = "barrier";
                } else if (waterwayTags.contains(tag)) {
                    tagType = "waterway";
                }
                HttpResponse<String> response = Unirest.post("https://overpass-api.de/api/interpreter")
                        .header("Content-Type", "text/plain")
                        .body("[out:json][timeout:25][maxsize:800000000];\n" +
                                "area[name=\"" + this.city + "\"]->.searchArea;\n" +
                                "nwr[\"" + tagType + "\"=\"" + tag + "\"](area.searchArea);\n" +
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
                            park.setType(tag);
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
//        }
    }

    @GetMapping("/markers")
    public ResponseEntity<String> getMarkers(@RequestParam("initialDate") LocalDate initialDate,
                                             @RequestParam("finalDate") LocalDate finalDate, @RequestParam("tag") String tag) throws IOException {
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
        List<Park> parksByCity = parkRepository.findAllByCityAndDeletedIsFalseAndType(city, tag);
        List<Park> parksCreated = parkRepository.findAllByCityAndCreatedIsTrueAndTypeAndDeletedIsFalse(city, tag);
        return crowdsensingService.getMarkers(elementsArray, visitas, parksByCity, parksCreated);
    }

    @DeleteMapping("/delete/park")
    public void deletePark(@RequestParam("name") String name) {
        Park park = parkRepository.findByName(name);
        park.setDeleted(true);
        parkRepository.save(park);
    }

    @PutMapping("/update/park")
    public void updateParkName(@RequestParam("name") String name, @RequestParam("newName") String newName) {
        Park park = parkRepository.findByName(name);
        park.setName(newName);
        parkRepository.save(park);
    }

    @PostMapping("/create/location")
    public void createLocation(@RequestBody LocationRequest locationRequest) {
        var park = parkRepository.findTopByOrderByIdDesc();
        var newPark = new Park();
        newPark.setId(park.getId() + 1);
        newPark.setType(locationRequest.getType());
        newPark.setName(locationRequest.getName());
        newPark.setCity(locationRequest.getCity());
        List<String> points = new ArrayList();
        for(List l: locationRequest.getPositions()) {
            String point = "";
            for(Object o: l) {
                if(point == "") {
                    point += o;
                } else {
                    point = point + "; " + o;
                }
            }
            points.add(point);
        }
        newPark.setPoints(points);
        newPark.setCreated(true);
        parkRepository.save(newPark);
    }

    @GetMapping("/parks")
    public List<Park> getParks() {
        return parkRepository.findAll();
    }

    @GetMapping("/visitas")
    public List<Visitas> getVisitas() {
        return viajeRepository.findAll();
    }


    @PostMapping("/upload")
    public String uploadCsv(@RequestParam("file") MultipartFile file) {
        String response = crowdsensingService.uploadCsv(file);
        return response;
    }
}
