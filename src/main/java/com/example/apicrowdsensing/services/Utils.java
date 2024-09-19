package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.Point;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class Utils {
    public static final List<String> HIGHWAY_TAGS = Arrays.asList("motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential", "service", "footway", "cycleway", "path", "track", "pedestrian");
    public static final List<String> RAILWAY_TAGS = Arrays.asList("rail", "light_rail", "subway", "tram", "narrow_gauge", "funicular", "monorail");
    public static final List<String> AEROWAY_TAGS = Arrays.asList("aerodrome", "runway", "taxiway", "helipad");
    public static final List<String> AMENITY_TAGS = Arrays.asList("restaurant", "bar", "cafe", "hospital", "school", "kindergarten", "university", "library", "bank", "atm", "parking", "fuel", "police", "fire_station", "post_office", "toilets", "theatre", "cinema", "pharmacy", "marketplace");
    public static final List<String> SHOP_TAGS = Arrays.asList("supermarket", "bakery", "butcher", "clothes", "convenience", "electronics", "furniture", "hardware", "jewelry", "mall", "optician", "sports", "toys");
    public static final List<String> LEISURE_TAGS = Arrays.asList("park", "playground", "sports_centre", "swimming_pool", "stadium", "golf_course", "marina", "garden", "dog_park");
    public static final List<String> TOURISM_TAGS = Arrays.asList("hotel", "motel", "guest_house", "hostel", "camp_site", "caravan_site", "chalet", "alpine_hut", "information", "museum", "zoo", "theme_park", "viewpoint");
    public static final List<String> LANDUSE_TAGS = Arrays.asList("residential", "commercial", "industrial", "forest", "farmland", "meadow", "vineyard", "orchard", "cemetery", "military", "recreation_ground");
    public static final List<String> NATURAL_TAGS = Arrays.asList("wood", "water", "wetland", "beach", "cliff", "rock", "scrub", "sand", "heath", "peak", "volcano");
    public static final List<String> BUILDING_TAGS = Arrays.asList("yes", "residential", "commercial", "industrial", "church", "school", "hospital", "apartments", "house", "detached", "terrace", "warehouse", "barn");
    public static final List<String> MAN_MADE_TAGS = Arrays.asList("tower", "chimney", "water_tower", "lighthouse", "communications_tower");
    public static final List<String> HISTORIC_TAGS = Arrays.asList("castle", "fort", "ruins", "archaeological_site", "monument", "memorial", "battlefield", "wayside_cross");
    public static final List<String> POWER_TAGS = Arrays.asList("plant", "generator", "substation", "tower", "line");
    public static final List<String> PIPELINE_TAGS = Arrays.asList("oil", "gas", "water");
    public static final List<String> BOUNDARY_TAGS = Arrays.asList("administrative", "national_park", "protected_area");
    public static final List<String> BARRIER_TAGS = Arrays.asList("fence", "wall", "hedge", "gate");
    public static final List<String> WATERWAY_TAGS = Arrays.asList("river", "stream", "canal", "drain", "ditch");

    public String getTag(String tag) {
        String tagType = null;
        if (HIGHWAY_TAGS.contains(tag)) {
            tagType = "highway";
        } else if (RAILWAY_TAGS.contains(tag)) {
            tagType = "railway";
        } else if (AEROWAY_TAGS.contains(tag)) {
            tagType = "aeroway";
        } else if (AMENITY_TAGS.contains(tag)) {
            tagType = "amenity";
        } else if (SHOP_TAGS.contains(tag)) {
            tagType = "shop";
        } else if (LEISURE_TAGS.contains(tag)) {
            tagType = "leisure";
        } else if (TOURISM_TAGS.contains(tag)) {
            tagType = "tourism";
        } else if (LANDUSE_TAGS.contains(tag)) {
            tagType = "landuse";
        } else if (NATURAL_TAGS.contains(tag)) {
            tagType = "natural";
        } else if (BUILDING_TAGS.contains(tag)) {
            tagType = "building";
        } else if (MAN_MADE_TAGS.contains(tag)) {
            tagType = "manMade";
        } else if (HISTORIC_TAGS.contains(tag)) {
            tagType = "historic";
        } else if (POWER_TAGS.contains(tag)) {
            tagType = "power";
        } else if (PIPELINE_TAGS.contains(tag)) {
            tagType = "pipeline";
        } else if (BOUNDARY_TAGS.contains(tag)) {
            tagType = "boundary";
        } else if (BARRIER_TAGS.contains(tag)) {
            tagType = "barrier";
        } else if (WATERWAY_TAGS.contains(tag)) {
            tagType = "waterway";
        }
        return tagType;
    }

    public boolean pointInsidePoligon(Point point, List<String> poligon) {
        int intersections = 0;
        int n = poligon.size();
        double x = point.getX();
        double y = point.getY();
        for (int i = 0; i < n; i++) {
            String[] parts1 = poligon.get(i).split("; ");
            Point p1 = new Point(Double.parseDouble(parts1[0]), Double.parseDouble(parts1[1]));
            String[] parts2 = poligon.get((i + 1) % n).split("; ");
            Point p2 = new Point(Double.parseDouble(parts2[0]), Double.parseDouble(parts2[1]));
            if ((p1.getY() <= y && y < p2.getY() || p2.getY() <= y && y < p1.getY())
                    && x < (p2.getX() - p1.getX()) * (y - p1.getY()) / (p2.getY() - p1.getY()) + p1.getX()) {
                intersections++;
            }
        }
        return intersections % 2 == 1;
    }

    public JSONObject parseBoundsFromJsonResponse(String jsonResponse) {
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

    public String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
