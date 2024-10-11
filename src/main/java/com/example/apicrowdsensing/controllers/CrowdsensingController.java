package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.*;
import com.example.apicrowdsensing.services.CrowdsensingService;
import com.example.apicrowdsensing.services.OverpassService;
import com.example.apicrowdsensing.utils.CustomException;
import com.example.apicrowdsensing.views.responses.BaseResponse;
import com.example.apicrowdsensing.views.responses.ErrorResponse;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;

import java.util.List;


@RestController
public class CrowdsensingController {
    private final CrowdsensingService crowdsensingService;
    private final OverpassService overpassService;

    @Autowired
    public CrowdsensingController(CrowdsensingService crowdsensingService, OverpassService overpassService) {
        this.crowdsensingService = crowdsensingService;
        this.overpassService = overpassService;
    }

    @GetMapping("/publicspaces")
    public ResponseEntity<BaseResponse> getPublicSpaces() {
        List<PublicSpace> publicSpaceList;
        try {
            publicSpaceList = crowdsensingService.getPublicSpaces();
        } catch (RuntimeException | CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse(publicSpaceList, null));
    }

    @GetMapping("/visits")
    public ResponseEntity<BaseResponse> getVisits() {
        List<Visit> visitasList;
        try {
            visitasList = crowdsensingService.getVisits();
        } catch (RuntimeException | CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse(visitasList, null));
    }

    @GetMapping("/publicspaces/created")
    public ResponseEntity<BaseResponse> getPublicSpacesCreated() {
        List<PublicSpace> createdPS;
        try {
            createdPS = crowdsensingService.getPublicSpacesCreated();
        } catch (RuntimeException | CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse(createdPS, null));
    }

    @GetMapping("/query/overpass")
    public ResponseEntity<BaseResponse> getOverpassQuery(@RequestParam("city") String city, @RequestParam("tag") String tag) {
        try {
            overpassService.getOverpassQuery(city, tag);
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        } catch (UnirestException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse("Overpass queried!", null));
    }

    @GetMapping("/markers")
    public ResponseEntity<String> getMarkers(@RequestParam("initialDate") LocalDate initialDate, @RequestParam("finalDate") LocalDate finalDate, @RequestParam("tag") String tag, @RequestParam("city") String city) throws IOException {
        ResponseEntity<String> response;
        try {
            response = crowdsensingService.getMarkers(initialDate, finalDate, tag, city);
        } catch (CustomException e) {
//            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return response;
    }

    @PutMapping("/delete/publicspace/{id}")
    public ResponseEntity<BaseResponse> softDeletePublicSpace(@PathVariable long id) {
        try {
            crowdsensingService.softDeletePublicSpace(id);
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse("Public space deleted successfully", null));
    }

    @PutMapping("/publicspaces/name/{id}")
    public ResponseEntity<BaseResponse> updatePublicSpaceName(@PathVariable(value="id") long id, @RequestParam("newName") String newName) {
        try {
            crowdsensingService.updatePublicSpaceName(id, newName);
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse("Public space's name updated", null));
    }

    @PostMapping("/create/location")
    public ResponseEntity<BaseResponse> createLocation(@RequestBody LocationRequest locationRequest) {
        try {
            crowdsensingService.createLocation(locationRequest);
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse("New location created",null));
    }

    @PostMapping("/upload")
    public ResponseEntity<BaseResponse> uploadCsv(@RequestParam("file") MultipartFile file) {
        try {
            crowdsensingService.uploadCsv(file);
        } catch (CustomException | IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse(null, new ErrorResponse(e)));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse("File was uploaded to the database",null));
    }

    @GetMapping("/publicspaces/{id}")
    public ResponseEntity<BaseResponse> findPublicSpace(@PathVariable long id) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(new BaseResponse(crowdsensingService.findPublicSpace(id), null));
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BaseResponse(null, new ErrorResponse(e)));
        }
    }
}

