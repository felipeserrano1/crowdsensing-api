package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.PublicSpace;
import com.example.apicrowdsensing.repositories.PublicSpaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

class OverpassServiceTest {
    @InjectMocks
    private OverpassService overpassService;

    @Mock
    private PublicSpaceRepository publicSpaceRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Utils utils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testGetBoundsCity_Success() throws Exception {
        WireMockServer wireMockServer = new WireMockServer(8089);  // Iniciar el servidor WireMock en el puerto 8080
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        String jsonResponse = """
                {
                    "osm_id": 123,
                    "bounds": {
                        "minlat": -34.0,
                        "minlon": -58.5,
                        "maxlat": -34.1,
                        "maxlon": -58.4
                    }
                }
                """;
        wireMockServer.stubFor(get(urlMatching("/api/interpreter\\?data=.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));


        JSONObject result = overpassService.getBoundsCity("Buenos Aires", "Argentina");

//        assertNotNull(result);
//        assertEquals(123, result.getInt("osm_id"));
//        assertEquals(-34.0, result.getJSONObject("bounds").getDouble("minlat"));
//        assertEquals(-58.5, result.getJSONObject("bounds").getDouble("minlon"));

        // Verificar que la solicitud fue hecha correctamente
        //WireMock.verify(getRequestedFor(urlMatching("/api/interpreter")));

        wireMockServer.stop();
    }

    @Test
    public void testSavePublicSpaces_Success() throws Exception {
        String jsonString = """
                [
                  {
                    "id": 123456,
                    "geometry": [
                      { "lat": -34.603722, "lon": -58.381592 },
                      { "lat": -34.604722, "lon": -58.382592 }
                    ],
                    "tags": {
                      "name": "test"
                    }
                  }
                ]
                """;

        JsonNode elementsArray = objectMapper.readTree(jsonString);

        when(publicSpaceRepository.findByOverpassId(123456L)).thenReturn(null);

        overpassService.savePublicSpaces(elementsArray, "tag", -35.0, -34.0, -59.0, -57.0, "city");

        verify(publicSpaceRepository, times(1)).save(any(PublicSpace.class));
    }

    @Test
    public void testSavePublicSpaces_AlreadyExists() throws Exception {
        String jsonString = """
                [
                  {
                    "id": 123456,
                    "geometry": [
                      { "lat": -34.603722, "lon": -58.381592 },
                      { "lat": -34.604722, "lon": -58.382592 }
                    ],
                    "tags": {
                      "name": "test"
                    }
                  }
                ]
                """;

        JsonNode elementsArray = objectMapper.readTree(jsonString);

        PublicSpace existingSpace = new PublicSpace();
        existingSpace.setOverpassId(123456L);
        when(publicSpaceRepository.findByOverpassId(123456L)).thenReturn(existingSpace);

        overpassService.savePublicSpaces(elementsArray, "tag", -35.0, -34.0, -59.0, -57.0, "city");

        verify(publicSpaceRepository, never()).save(any(PublicSpace.class));
    }

    @Test
    public void testSavePublicSpaces_OutsideBounds() throws Exception {
        String jsonString = """
                [
                  {
                    "id": 123456,
                    "geometry": [
                      { "lat": -33.603722, "lon": -57.381592 },
                      { "lat": -33.604722, "lon": -57.382592 }
                    ],
                    "tags": {
                      "name": "test"
                    }
                  }
                ]
                """;

        JsonNode elementsArray = objectMapper.readTree(jsonString);

        when(publicSpaceRepository.findByOverpassId(123456L)).thenReturn(null);

        overpassService.savePublicSpaces(elementsArray, "tag", -35.0, -34.0, -59.0, -58.0, "city");

        verify(publicSpaceRepository, never()).save(any(PublicSpace.class));
    }

    @Test
    public void testSavePublicSpaces_NoGeometry() throws Exception {
        String jsonString = """
                [
                  {
                    "id": 123456,
                    "tags": {
                      "name": "test"
                    }
                  }
                ]
                """;

        JsonNode elementsArray = objectMapper.readTree(jsonString);

        overpassService.savePublicSpaces(elementsArray, "tag", -35.0, -34.0, -59.0, -58.0, "city");

        verify(publicSpaceRepository, never()).save(any(PublicSpace.class));
    }
}