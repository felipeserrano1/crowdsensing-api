package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.utils.CustomException;
import com.example.apicrowdsensing.models.*;
import com.example.apicrowdsensing.repositories.PublicSpaceRepository;
import com.example.apicrowdsensing.repositories.VisitRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CrowdsensingService {
    private final VisitRepository visitRepository;
    private final PublicSpaceRepository publicSpaceRepository;
    private final Utils utils;

    @Autowired
    public CrowdsensingService(VisitRepository visitRepository, PublicSpaceRepository publicSpaceRepository, Utils utils) {
        this.visitRepository = visitRepository;
        this.publicSpaceRepository = publicSpaceRepository;
        this.utils = utils;
    }

    public void createLocation(LocationRequest locationRequest) throws CustomException {
        var newPublicSpace = new PublicSpace();
        newPublicSpace.setType(locationRequest.getType());
        newPublicSpace.setName(locationRequest.getName());
        newPublicSpace.setCity(locationRequest.getCity());
        List<String> points = new ArrayList<>();

        if(locationRequest.getPositions().get(0).isEmpty()) {
            throw new CustomException("List of points is empty", HttpStatus.BAD_REQUEST);
        }

        for(List l: locationRequest.getPositions()) {
            StringBuilder point = new StringBuilder();
            for(Object o: l) {
                if(point.toString().equals("")) {
                    point.append(o);
                } else {
                    point.append("; ").append(o);
                }
            }
            points.add(point.toString());
        }

        newPublicSpace.setPoints(points);
        newPublicSpace.setCreated(true);
        publicSpaceRepository.save(newPublicSpace);

    }

    public void updatePublicSpaceName(long id, String newName) throws CustomException {
        PublicSpace publicSpace = publicSpaceRepository.findById(id);
        if (publicSpace == null) {
            throw new CustomException("PublicSpace not found", HttpStatus.BAD_REQUEST);
        }
        publicSpace.setName(newName);
        publicSpaceRepository.save(publicSpace);

    }

    public void softDeletePublicSpace(long id) throws CustomException {
        PublicSpace publicSpace = publicSpaceRepository.findById(id);
        if (publicSpace == null) {
            throw new CustomException("PublicSpace not found", HttpStatus.BAD_REQUEST);
        }
        publicSpace.setDeleted(true);
        publicSpaceRepository.save(publicSpace);
    }

    public List<Visit> getVisits() throws CustomException {
        List<Visit> visits;
        visits = visitRepository.findAll();
        if(visits.isEmpty()) {
            throw new CustomException("No visits available", HttpStatus.BAD_REQUEST);
        }
        return visits;
    }

    public List<PublicSpace> getPublicSpaces() throws CustomException {
        List<PublicSpace> publicSpaces;
        publicSpaces = publicSpaceRepository.findAll();
        if(publicSpaces.isEmpty()) {
            throw new CustomException("No public spaces available", HttpStatus.BAD_REQUEST);
        }
        return publicSpaces;
    }

    public void uploadCsv(MultipartFile file) throws CustomException, IOException {
        if (file.isEmpty()) {
            throw new CustomException("File is empty", HttpStatus.BAD_REQUEST);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int numlinea = 0;
            List<Visit> visitList = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                if (numlinea > 0 && values.length < 6) {
                    throw new CustomException("Malformed CSV: Insufficient columns on line " + numlinea, HttpStatus.BAD_REQUEST);
                }
                Visit v = new Visit();
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
                    visitList.add(v);
                }
                numlinea++;
            }
            visitRepository.saveAll(visitList);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    public ArrayList<DateTraffic> getTrafficByPublicSpace(PublicSpace publicSpace, List<Visit> visitas) {
        ArrayList<DateTraffic> l = new ArrayList<>();
        for(Visit v: visitas) {
            String[] partes = v.getCenter().substring(1, v.getCenter().length() - 1).split(",");
            double primerCoordenada = Double.parseDouble(partes[0]);
            double segundoCoordenada = Double.parseDouble(partes[1]);
            if (utils.pointInsidePoligon(new Point(primerCoordenada, segundoCoordenada), publicSpace.getPoints())) {
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
                if (!contains) {
                    l.add(new DateTraffic(date));
                }
            }
        }
        return l;
    }

    public ResponseEntity<String> getMarkers(LocalDate initialDate, LocalDate finalDate, String tag, String city) throws IOException, CustomException {
        LocalDateTime initialDateTime = initialDate.atStartOfDay();
        LocalDateTime finalDateTime = finalDate.atStartOfDay();
        ZonedDateTime initial_zdt = initialDateTime.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
        ZonedDateTime final_zdt = finalDateTime.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
        long initial_ms = initial_zdt.toInstant().toEpochMilli();
        long final_ms  = final_zdt.toInstant().toEpochMilli();

        List<Visit> visits = visitRepository.findByStartTimeBetween(initial_ms, final_ms);
        if(visits.isEmpty()) {
            throw new CustomException("No visits in that range of time", HttpStatus.BAD_REQUEST);
        }
        List<PublicSpace> publicSpaces = publicSpaceRepository.findAllByCityAndDeletedIsFalseAndType(city, tag);
        if(publicSpaces.isEmpty()) {
            throw new CustomException("No public spaces with that specification", HttpStatus.BAD_REQUEST);
        }
        return filterElements(visits, publicSpaces);
    }

    public ResponseEntity<String> filterElements(List<Visit> visits, List<PublicSpace> publicSpaces) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        double lat, lon;
        List<NewFormatMarker> result = new ArrayList<>();
        for(PublicSpace p: publicSpaces) {
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
                ArrayList<DateTraffic> traffic = getTrafficByPublicSpace(p, visits);
                result.add(new NewFormatMarker(p.getId(), new double[]{lat, lon}, name, traffic));
            }
        String json = mapper.writeValueAsString(result);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(json);
    }
}
