package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.*;
import com.example.apicrowdsensing.services.CrowdsensingService;
import com.example.apicrowdsensing.views.BaseResponse;
import com.example.apicrowdsensing.views.ErrorResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CrowdsensingController {
    private CrowdsensingService crowdsensingService;

    @Autowired
    public CrowdsensingController(CrowdsensingService crowdsensingService) {
        this.crowdsensingService = crowdsensingService;
    }

    @GetMapping("/query/city")
    public BaseResponse getQuery(@RequestParam("city") String city, @RequestParam("tag") String tag) throws Exception {
        String response;
        try {
            response = crowdsensingService.getQuery(city, tag);
        } catch (Exception e) {
            return new BaseResponse(null, new ErrorResponse(e.getMessage()));
        }
        return new BaseResponse(response, null);
    }

    @GetMapping("/markers")
    public ResponseEntity<String> getMarkers(@RequestParam("initialDate") LocalDate initialDate, @RequestParam("finalDate") LocalDate finalDate, @RequestParam("tag") String tag) throws IOException {
        ResponseEntity<String> response;
        try {
            response = crowdsensingService.getMarkers(initialDate, finalDate, tag);
        } catch (IOException e) {
            return ResponseEntity.ofNullable(e.getMessage());
        }
        return response;
    }

    @GetMapping("/parks")
    public BaseResponse getParks() {
        List<Park> parkList = null;
        try {
            parkList = crowdsensingService.getParks();
        } catch (Exception e) {
            return new BaseResponse(null, new ErrorResponse(e.getMessage()));
        }
        return new BaseResponse(parkList, null);
    }

    @GetMapping("/visitas")
    public BaseResponse getVisitas() {
        var visitasList = new ArrayList<Visitas>();
        try {
            visitasList = crowdsensingService.getVisitas();
        } catch (Exception e) {
            return new BaseResponse(null, new ErrorResponse(e.getMessage()));
        }
        return new BaseResponse(visitasList, null);
    }

    @DeleteMapping("/delete/park")
    public BaseResponse deletePark(@RequestParam("name") String name) {
        String response;
        try {
            response = crowdsensingService.deletePark(name);
        } catch (Exception e) {
            return new BaseResponse(null, new ErrorResponse(e.getMessage()));
        }
        return new BaseResponse(response, null);
    }

    @PutMapping("/update/park")
    public BaseResponse updateParkName(@RequestParam("name") String name, @RequestParam("newName") String newName) {
        String response;
        try {
            response = crowdsensingService.updateParkName(name, newName);
        } catch (Exception e) {
            return new BaseResponse(null, new ErrorResponse(e.getMessage()));
        }
        return new BaseResponse(response, null);
    }

    @PostMapping("/create/location")
    public BaseResponse createLocation(@RequestBody LocationRequest locationRequest) {
        String response;
        try {
            response = crowdsensingService.createLocation(locationRequest);
        } catch (Exception e) {
            return new BaseResponse(null, new ErrorResponse(e.getMessage()));
        }
        return new BaseResponse(response, null);
    }

    @PostMapping("/upload")
    public BaseResponse uploadCsv(@RequestParam("file") MultipartFile file) {
        String response;
        try {
            response = crowdsensingService.uploadCsv(file);
        } catch (Exception e) {
            return new BaseResponse(null, new ErrorResponse(e.getMessage()));
        }
        return new BaseResponse(response, null);
    }
}
