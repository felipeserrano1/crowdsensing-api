package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.Path;
import com.example.apicrowdsensing.services.CrowdsensingService;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;


@RestController
public class CrowdsensingController {
    private CrowdsensingService crowdsensingService;

    @Autowired
    public CrowdsensingController(CrowdsensingService crowdsensingService) {
        this.crowdsensingService = crowdsensingService;
    }

    @GetMapping("/paths")
    public Object getAllPaths() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File("src/main/resources/paths.json");
            objectMapper.registerModule(new JavaTimeModule());
            Path path = objectMapper.readValue(file, Path.class);
            return crowdsensingService.getPeople(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
