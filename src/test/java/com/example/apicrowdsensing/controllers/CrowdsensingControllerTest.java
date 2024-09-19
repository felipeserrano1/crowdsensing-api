package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.LocationRequest;
import com.example.apicrowdsensing.models.PublicSpace;
import com.example.apicrowdsensing.models.Visit;
import com.example.apicrowdsensing.repositories.PublicSpaceRepository;
import com.example.apicrowdsensing.repositories.VisitRepository;
import com.example.apicrowdsensing.services.Utils;
import com.example.apicrowdsensing.views.responses.BaseResponse;
import com.example.apicrowdsensing.views.responses.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CrowdsensingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PublicSpaceRepository publicSpaceRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private Utils utils;

    @Test
    public void testGetOverpassQuery() throws Exception {
        WireMockServer wireMockServer = new WireMockServer(8089); // Puerto para mockear
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/interpreter"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                            "elements": [
                                {
                                    "type": "node",
                                    "id": 123456,
                                    "lat": 50.123456,
                                    "lon": 8.123456,
                                    "tags": {
                                        "name": "Test Park",
                                        "leisure": "park"
                                    }
                                }
                            ]
                        }
                    """)));

        MvcResult result = mockMvc.perform(get("/query/overpass")
                        .param("city", "test")
                        .param("tag", "test"))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        assertTrue(jsonResponse.contains("Overpass queried!"));

        wireMockServer.stop();
    }
    @Test
    public void testCreateLocationSuccess() throws Exception {
        var publicSpace = new PublicSpace();
        publicSpace.setName("test");
        publicSpace.setCity("city");
        publicSpace.setType("park");
        publicSpace.setCreated(true);
        publicSpace.setDeleted(false);
        publicSpace.setPoints(List.of("40.7128; -74.0060"));
        publicSpaceRepository.save(publicSpace);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName("Test Location");
        locationRequest.setType("Test");
        List<List<String>> positions = new ArrayList<>();
        ArrayList<String> coords = new ArrayList<>();
        String lat = "40.7128";
        String lon = "-74.0060";
        coords.add(lat);
        coords.add(lon);
        positions.add(coords);
        locationRequest.setPositions(positions);

        MvcResult result = mockMvc.perform(post("/create/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(utils.asJsonString(locationRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        Exception errorResponse = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getErrorResponse().getException()).isNull();
        assertThat(baseResponse.getPayload()).isNotNull();
        assertEquals(baseResponse.getPayload(), "New location created");
        assertThat(publicSpaceRepository.findByName("Test Location")).isNotNull();
    }
    @Test
    public void testCreateLocationException() throws Exception {
        var publicSpace = new PublicSpace();
        publicSpace.setName("test");
        publicSpace.setCity("city");
        publicSpace.setType("park");
        publicSpace.setCreated(true);
        publicSpace.setDeleted(false);
        publicSpace.setPoints(List.of("40.7128; -74.0060"));
        publicSpaceRepository.save(publicSpace);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName("Test Location");
        locationRequest.setType("Test");
        locationRequest.setCity("City");
        List<List<String>> positions = new ArrayList<>();
        List<String> coords = new ArrayList<>();
        positions.add(coords);
        locationRequest.setPositions(positions);

        MvcResult result = mockMvc.perform(post("/create/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(utils.asJsonString(locationRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String error = JsonPath.read(response, "$.errorResponse.exception.message");
        assertEquals(error, "List of points is empty");
        assertThat(publicSpaceRepository.findByName("Test Location")).isNull();
    }
    @Test
    public void testGetPublicSpacesSuccess() throws Exception {
        var publicSpace = new PublicSpace();
        publicSpace.setName("test");
        publicSpace.setCity("city");
        publicSpace.setType("park");
        publicSpace.setCreated(true);
        publicSpace.setDeleted(false);
        publicSpace.setPoints(List.of("40.7128; -74.0060"));
        publicSpaceRepository.save(publicSpace);


        MvcResult result = mockMvc.perform(get("/publicspaces")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<Map<String, Object>> payload = JsonPath.read(jsonResponse, "$.payload");
        Exception errorResponse = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getErrorResponse().getException()).isNull();
        assertThat(payload).isNotEmpty();
    }
    @Test
    public void testGetPublicSpacesEmpty() throws Exception {
        MvcResult result = mockMvc.perform(get("/publicspaces")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String error = JsonPath.read(jsonResponse, "$.errorResponse.exception.message");

        assertEquals(error, "No public spaces available");
    }
    @Test
    public void testGetVisitsSuccess() throws Exception {
        var visit = new Visit();
        visit.setId(1);
        visit.setUser_id(154);
        visit.setCenter("p1, p2");
        visit.setStartTime(1617640000000L);
        visit.setEnd_time(1617640000000L);
        visitRepository.save(visit);

        MvcResult result = mockMvc.perform(get("/visits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<Map<String, Object>> payload = JsonPath.read(jsonResponse, "$.payload");
        Exception errorResponse = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(jsonResponse, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getErrorResponse().getException()).isNull();
        assertThat(payload).isNotNull();
    }
    @Test
    public void testGetVisitsEmpty() throws Exception {
        MvcResult result = mockMvc.perform(get("/visits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String error = JsonPath.read(jsonResponse, "$.errorResponse.exception.message");

        assertEquals(error, "No visits available");
    }
    @Test
    public void testSoftDeletePublicSpaceSuccess() throws Exception {
        var ps = new PublicSpace();
        ps.setId(1L);
        ps.setName("test");
        ps.setCity("city");
        ps.setType("park");
        ps.setCreated(true);
        ps.setDeleted(false);
        ps.setPoints(List.of("40.7128; -74.0060"));
        publicSpaceRepository.save(ps);

        MvcResult result = mockMvc.perform(put("/delete/publicspace/{id}", 11)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();


        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");

        assertEquals(payload , "Public space deleted successfully");
    }
    @Test
    public void testSoftDeletePublicSpaceException() throws Exception {
        publicSpaceRepository.deleteAll();
        var ps = new PublicSpace();
//        ps.setId(1L);
        ps.setName("Test 1");
        ps.setCity("City");
        ps.setType("park");
        ps.setCreated(true);
        ps.setDeleted(false);
        ps.setPoints(List.of("40.7128; -74.0060"));
        publicSpaceRepository.save(ps);

        mockMvc.perform(put("/delete/publicspace/{id}", 199L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertFalse(publicSpaceRepository.findByName("Test 1").isDeleted());
    }
    @Test
    public void testUpdateParkNameSuccess() throws Exception {
        var ps = new PublicSpace();
        ps.setId(1L);
        ps.setName("test");
        ps.setCity("city");
        ps.setType("park");
        ps.setCreated(true);
        ps.setDeleted(false);
        ps.setPoints(List.of("40.7128; -74.0060"));
        var ps2 = new PublicSpace();
        ps2.setId(2L);
        ps2.setName("test");
        ps2.setCity("city");
        ps2.setType("park");
        ps2.setCreated(true);
        ps2.setDeleted(false);
        ps2.setPoints(List.of("40.7128; -74.0060"));
        publicSpaceRepository.save(ps);
        publicSpaceRepository.save(ps2);

        MvcResult result = mockMvc.perform(put("/publicspaces/name/{id}", 4)
                        .param("newName", "New Name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        Exception errorMessage = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorMessage));

        assertThat(baseResponse.getErrorResponse()).isNotNull();
        assertThat(baseResponse.getPayloadAsString(payload)).isEqualTo("Public space's name updated");

        PublicSpace updatedPublicSpace = publicSpaceRepository.findById(4L);
        assertThat(updatedPublicSpace).isNotNull();
        assertThat(updatedPublicSpace.getName()).isEqualTo("New Name");
    }
    @Test
    public void testUpdateParkNameException() throws Exception {
        var ps = new PublicSpace();
        ps.setId(1L);
        ps.setName("Test 1");
        ps.setCity("City");
        ps.setType("park");
        ps.setCreated(true);
        ps.setDeleted(false);
        ps.setPoints(List.of("40.7128; -74.0060"));
        publicSpaceRepository.save(ps);

        MvcResult result = mockMvc.perform(put("/publicspaces/name/{id}", 100L)
                        .param("newName", "NewName")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String error = JsonPath.read(jsonResponse, "$.errorResponse.exception.message");

        assertEquals(error, "PublicSpace not found");
    }
    @Test
    public void testUploadCsvSuccess() throws Exception {
        String csvContent = """
                id,user_id,coordinate,start_time,end_time
                1,100,"(40.7128,-74.0060)",1609459200000,1609462800000
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes());

        MvcResult result = mockMvc.perform(multipart("/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        Exception errorResponse = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getErrorResponse().getException()).isNull();
        assertThat(baseResponse.getPayload()).isEqualTo("File was uploaded to the database");

        List<Visit> visitList = visitRepository.findAll();
        assertThat(visitList).hasSize(1);
        assertThat(visitList.get(0).getId()).isEqualTo(1);
        assertThat(visitList.get(0).getUser_id()).isEqualTo(100);
        assertThat(visitList.get(0).getCenter()).isEqualTo("(40.7128,-74.0060)");
    }
    @Test
    public void testUploadCsvEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        MvcResult result = mockMvc.perform(multipart("/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String error = JsonPath.read(jsonResponse, "$.errorResponse.exception.message");

        assertEquals(error, "File is empty");

        List<Visit> visitList = visitRepository.findAll();
        assertThat(visitList).isEmpty();
    }
    @Test
    public void testUploadCsvMalformed() throws Exception {
        String csvContent = """
                id,user_id,coordinate,start_time,end_time
                1,100,"(40.7128,-74.0060)"
                """; // Falta start_time y end_time

        MockMultipartFile file = new MockMultipartFile(
                "file", "malformed.csv", "text/csv", csvContent.getBytes());


        mockMvc.perform(multipart("/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorResponse.exception.message")
                        .value("Malformed CSV: Insufficient columns on line 1"));

        List<Visit> visitList = visitRepository.findAll();
        assertThat(visitList).isEmpty();
    }
    @Test
    public void testGetMarkersSuccess() throws Exception {
        var ps = new PublicSpace();
        ps.setName("test");
        ps.setCity("city");
        ps.setType("park");
        ps.setCreated(true);
        ps.setDeleted(false);
        ps.setPoints(Arrays.asList(
                "-37.3266645; -59.1290909",
                "-37.3261356; -59.127699",
                "-37.3262184; -59.1276492",
                "-37.3271408; -59.1270942",
                "-37.3272499; -59.1270286",
                "-37.3277621; -59.1284428",
                "-37.3266645; -59.1290909"
        ));
        publicSpaceRepository.save(ps);

        Visit visit = new Visit();
        visit.setId(1);
        visit.setUser_id(100);
        visit.setCenter("(-37.326952, -59.128071)");
        visit.setStartTime(1577847600000L); // 01-01-2021
        visit.setEnd_time(1640919600000L); // 02-01-2021
        visitRepository.save(visit);


        LocalDate initialDate = LocalDate.of(2020, 1, 1);
        LocalDate finalDate = LocalDate.of(2021, 12, 31);
        String tag = "park";

        MvcResult result = mockMvc.perform(get("/markers")
                        .param("initialDate", String.valueOf(initialDate))
                        .param("finalDate", String.valueOf(finalDate))
                        .param("tag", tag)
                        .param("city", "city")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        for (JsonNode marker : rootNode) {
            JsonNode trafficArray = marker.get("traffic");
            for (JsonNode traffic : trafficArray) {
                int trafficValue = traffic.get("traffic").asInt();
                assertEquals(trafficValue, 1);
            }
        }
        assertTrue(jsonResponse.contains("geocode"));
    }
    @Test
    public void testGetMarkersEmpty() throws Exception {
        var ps = new PublicSpace();
        ps.setName("test");
        ps.setCity("city");
        ps.setType("park");
        ps.setCreated(true);
        ps.setDeleted(false);
        ps.setPoints(Arrays.asList(
                "-37.3266645; -59.1290909",
                "-37.3261356; -59.127699",
                "-37.3262184; -59.1276492",
                "-37.3271408; -59.1270942",
                "-37.3272499; -59.1270286",
                "-37.3277621; -59.1284428",
                "-37.3266645; -59.1290909"
        ));
        publicSpaceRepository.save(ps);

        Visit visit = new Visit();
        visit.setId(1);
        visit.setUser_id(100);
        visit.setCenter("(-37.326952, -59.128071)");
        visit.setStartTime(1577847600000L); // 01-01-2021
        visit.setEnd_time(1640919600000L); // 02-01-2021
        visitRepository.save(visit);


        LocalDate initialDate = LocalDate.of(2020, 1, 1);
        LocalDate finalDate = LocalDate.of(2021, 12, 31);
        String tag = "bar";

        MvcResult result = mockMvc.perform(get("/markers")
                        .param("initialDate", String.valueOf(initialDate))
                        .param("finalDate", String.valueOf(finalDate))
                        .param("tag", tag)
                        .param("city", "city")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertEquals(response, "No public spaces with that specification");
    }
}