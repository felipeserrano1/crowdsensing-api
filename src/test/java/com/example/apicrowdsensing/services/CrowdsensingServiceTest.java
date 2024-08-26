package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.Park;
import com.example.apicrowdsensing.repositories.ParkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class CrowdsensingServiceTest {
    @Mock
    private ParkRepository parkRepository;

    @InjectMocks
    private CrowdsensingService crowdsensingService;

    private Park park;

    @BeforeEach
    void setUp() {
        openMocks(this);
        var park = new Park();
        park.setId(1);
        park.setName("Parque Central");
        park.setCity("Tandil");
        park.setType("Park");
    }

    @Test
    void getParks() {
        when(parkRepository.findAll()).thenReturn(Arrays.asList(park));
        assertNotNull(crowdsensingService.getParks());
    }
}