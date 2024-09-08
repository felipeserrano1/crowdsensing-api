package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.constants.TagConstants;
import com.example.apicrowdsensing.models.*;
import com.example.apicrowdsensing.repositories.ParkRepository;
import com.example.apicrowdsensing.repositories.ViajeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class CrowdsensingService {
    private final ViajeRepository viajeRepository;
    private final ParkRepository parkRepository;
    private final ObjectMapper objectMapper;
    private final TagConstants tagConstants;
    private String city = "City";
    private List<Park> parks = new ArrayList<>();

    @Autowired
    public CrowdsensingService(ViajeRepository viajeRepository, ParkRepository parkRepository, ObjectMapper objectMapper, TagConstants tagConstants) {
        this.viajeRepository = viajeRepository;
        this.parkRepository = parkRepository;
        this.objectMapper = objectMapper;
        this.tagConstants = tagConstants;
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
    private static String encodeQuery(String query) throws UnsupportedEncodingException {
        return URLEncoder.encode(query, "UTF-8");
    }
    private String getTag(String tag) {
        String tagType = null;
        if (tagConstants.HIGHWAY_TAGS.contains(tag)) {
            tagType = "highway";
        } else if (tagConstants.RAILWAY_TAGS.contains(tag)) {
            tagType = "railway";
        } else if (tagConstants.AEROWAY_TAGS.contains(tag)) {
            tagType = "aeroway";
        } else if (tagConstants.AMENITY_TAGS.contains(tag)) {
            tagType = "amenity";
        } else if (tagConstants.SHOP_TAGS.contains(tag)) {
            tagType = "shop";
        } else if (tagConstants.LEISURE_TAGS.contains(tag)) {
            tagType = "leisure";
        } else if (tagConstants.TOURISM_TAGS.contains(tag)) {
            tagType = "tourism";
        } else if (tagConstants.LANDUSE_TAGS.contains(tag)) {
            tagType = "landuse";
        } else if (tagConstants.NATURAL_TAGS.contains(tag)) {
            tagType = "natural";
        } else if (tagConstants.BUILDING_TAGS.contains(tag)) {
            tagType = "building";
        } else if (tagConstants.MAN_MADE_TAGS.contains(tag)) {
            tagType = "manMade";
        } else if (tagConstants.HISTORIC_TAGS.contains(tag)) {
            tagType = "historic";
        } else if (tagConstants.POWER_TAGS.contains(tag)) {
            tagType = "power";
        } else if (tagConstants.PIPELINE_TAGS.contains(tag)) {
            tagType = "pipeline";
        } else if (tagConstants.BOUNDARY_TAGS.contains(tag)) {
            tagType = "boundary";
        } else if (tagConstants.BARRIER_TAGS.contains(tag)) {
            tagType = "barrier";
        } else if (tagConstants.WATERWAY_TAGS.contains(tag)) {
            tagType = "waterway";
        }
        return tagType;
    }
    public class EmptyPointsException extends RuntimeException {
        public EmptyPointsException(String message) {
            super(message);
        }
    }
    public String createLocation(LocationRequest locationRequest) {
        //try {
            var park = parkRepository.findTopByOrderByIdDesc();
            var newPark = new Park();
            newPark.setId(park.getId() + 1);
            newPark.setType(locationRequest.getType());
            newPark.setName(locationRequest.getName());
            newPark.setCity(locationRequest.getCity());

            List<String> points = new ArrayList();
            if(locationRequest.getPositions().get(0).isEmpty()) {
                throw new RuntimeException("The list of points is empty");
            }
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
            return "New location created!";
//        } catch (Exception e) {
//            return "An unexpected error occurred: " + e.getMessage();
//        }
    }
    public String updateParkName(String name, String newName) {
        Park park = parkRepository.findByName(name);
        if (park == null) {
            throw new RuntimeException("Park not found");
        }
        park.setName(newName);
        parkRepository.save(park);
        return "Public space's name updated!";
    }
    public String deletePark(String name) {
        // Busca el parque en la base de datos por nombre
        Park park = parkRepository.findByName(name);

        // Si el parque no existe, lanza una excepción
        if (park == null) {
            throw new RuntimeException("Park not found");
        }

        // Marca el parque como eliminado
        park.setDeleted(true);

        // Guarda los cambios en la base de datos
        parkRepository.save(park);

        // Devuelve un mensaje de éxito
        return "Public space deleted successfully";
    }
    public String uploadCsv(MultipartFile file) {
        if (file.isEmpty()) {
            return "El archivo esta vacio";
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int numlinea = 0;
            List<Visitas> visitasList = new ArrayList<>();
            List<String> a = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                Visitas v = new Visitas();
                double doubleValue;
                long longValue;

                if((numlinea > 0) && (values.length != 0)) {
                    v.setId(Integer.parseInt(values[0]));
                    v.setUser_id(Integer.parseInt(values[1]));
                    String firstCoordinate =  values[2].substring(1).trim();
                    String secondCoordinate = values[3].substring(0, values[3].length() - 1);
                    String coordinate = firstCoordinate + "," + secondCoordinate;
                    v.setCenter(coordinate);
                    doubleValue = Double.parseDouble(values[4]);
                    longValue = (long) doubleValue;
                    v.setStartTime(longValue);
                    doubleValue = Double.parseDouble(values[5]);
                    longValue = (long) doubleValue;
                    v.setEnd_time(longValue);
                    visitasList.add(v);
                }
                numlinea++;
            }
            viajeRepository.saveAll(visitasList);
            return "Archivo procesado y datos guardados en la base de datos.";
        } catch (Exception e) {
            //e.printStackTrace();
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
    public ArrayList<Visitas> getVisitas() {
        return (ArrayList) viajeRepository.findAll();
    }
    public List<Park> getParks() {
        return parkRepository.findAll();
    }
    public boolean pointInsidePoligon(Point point, List<String> poligon) {
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
    public ResponseEntity<String> getMarkers(LocalDate initialDate, LocalDate finalDate, String tag) throws IOException {
        Path path = Paths.get("src", "main", "resources", "response.json");

        JsonNode jsonNode = objectMapper.readTree(new File(path.toString()));
        JsonNode elementsArray = jsonNode.get("elements");

        LocalDateTime initialDateTime = initialDate.atStartOfDay();
        LocalDateTime finalDateTime = finalDate.atStartOfDay();
        ZonedDateTime initial_zdt = initialDateTime.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
        ZonedDateTime final_zdt = finalDateTime.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
        long initial_ms = initial_zdt.toInstant().toEpochMilli();
        long final_ms  = final_zdt.toInstant().toEpochMilli();

        List<Visitas> visitas = viajeRepository.findByStartTimeBetween(initial_ms, final_ms);
        List<Park> parksCreated = parkRepository.findAllByCityAndCreatedIsTrueAndTypeAndDeletedIsFalse(city, tag);

        ResponseEntity<String> response = filterElements(elementsArray, visitas, parksCreated);
        return response;
    }
    public ResponseEntity<String> filterElements(JsonNode elementsArray, List<Visitas> visitas, List<Park> parksCreated) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
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
    public void loadParks(JsonNode elementsArray, String tag, Double minlat, Double maxlat, Double minlon, Double maxlon) {
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
    }
    public String getQuery(String city, String tag) {
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
                    return "City or limits not found";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String tagType = getTag(tag);


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
                Object json = objectMapper.readValue(response.getBody(), Object.class);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(path.toString()), json);

            } catch (Exception e) {
                e.printStackTrace();
            }

            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(new File(path.toString()));
            JsonNode elementsArray = jsonNode.get("elements");
            loadParks(elementsArray, tagType, minlat, maxlat, minlon, maxlon);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Overpass queried!";
    }
    JSONObject getBoundsCity(String city, String country) throws Exception {
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
}
