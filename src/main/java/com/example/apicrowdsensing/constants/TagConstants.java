package com.example.apicrowdsensing.constants;

import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class TagConstants {

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
}