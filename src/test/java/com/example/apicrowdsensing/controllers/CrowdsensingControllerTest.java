package com.example.apicrowdsensing.controllers;

import com.example.apicrowdsensing.models.LocationRequest;
import com.example.apicrowdsensing.models.Park;
import com.example.apicrowdsensing.models.Visitas;
import com.example.apicrowdsensing.repositories.ParkRepository;
import com.example.apicrowdsensing.repositories.ViajeRepository;
import com.example.apicrowdsensing.views.BaseResponse;
import com.example.apicrowdsensing.views.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CrowdsensingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ParkRepository parkRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    static MockRestServiceServer mockServer;

    @BeforeEach
    public void setup() {}
    @AfterEach
    public void tearDown() {
        parkRepository.deleteAll();
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void testGetQuerySuccesful() throws Exception {
//        mockServer.expect(requestTo("https://overpass-api.de/api/interpreter"))
//                .andExpect(method(HttpMethod.POST))
//                .andRespond(withSuccess());

        mockServer = MockRestServiceServer.createServer(restTemplate);

        Path path = Paths.get("src", "main", "resources", "response.json");
        String jsonResponse = new String(Files.readAllBytes(path));
        mockServer.expect(requestTo("https://overpass-api.de/api/interpreter"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/query/city")
                        .param("city", "Ayacucho")
                        .param("tag", "park")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        List<Park> parks = parkRepository.findAll();
        System.out.println(parkRepository.findAll());

        assertThat(parks).isNotEmpty();
        assertThat(parks).anyMatch(p -> p.getName().equals("Plaza"));
    }
    @Test
    public void testGetQueryUnsuccesful() throws Exception {
        mockServer.expect(requestTo("https://overpass-api.de/api/interpreter"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        MvcResult result = mockMvc.perform(get("/query/city")
                        .param("city", "UnknownCity")
                        .param("tag", "park")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorMessage = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorMessage));

        assertEquals(baseResponse.getPayload(), "City or limits not found");

    }
    @Test
    public void testCreateLocationSuccess() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList("40.7128; -74.0060"));
        parkRepository.save(park1);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName("Test Location");
        locationRequest.setType("Test");
        List<List<String>> positions = new ArrayList<>();
        List<String> coords = new ArrayList();
        String lat = "40.7128";
        String lon = "-74.0060";
        coords.add(lat);
        coords.add(lon);
        positions.add(coords);
        locationRequest.setPositions(positions);

        MvcResult result = mockMvc.perform(post("/create/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(locationRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorResponse = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isNull();
        assertThat(baseResponse.getPayload()).isNotNull();
        assertEquals(baseResponse.getPayload(), "New location created!");
        assertThat(parkRepository.findByName("Test Location")).isNotNull();
    }
    @Test
    public void testCreateLocationException() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList("40.7128; -74.0060"));
        parkRepository.save(park1);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName("Test Location");
        locationRequest.setType("Test");
        List<List<String>> positions = new ArrayList<>();
        List<String> coords = new ArrayList();
        positions.add(coords);
        locationRequest.setPositions(positions);

        MvcResult result = mockMvc.perform(post("/create/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(locationRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorResponse = JsonPath.read(jsonResponse, "$.errorResponse.errorMessage");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getPayload()).isNull();
        assertThat(baseResponse.getErrorResponse()).isNotNull();
        assertEquals(baseResponse.getErrorResponse().getErrorMessage(), "The list of points is empty");
        assertThat(parkRepository.findByName("Test Location")).isNull();
    }
    @Test
    public void testGetParksSuccess() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList("40.7128; -74.0060"));
        parkRepository.save(park1);


        MvcResult result = mockMvc.perform(get("/parks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<Map<String, Object>> payload = JsonPath.read(jsonResponse, "$.payload");
        BaseResponse baseResponse = new BaseResponse(jsonResponse, null);

        assertThat(baseResponse.getErrorResponse()).isNull();
        assertThat(payload).isNotEmpty();
    }
    @Test
    public void testGetParksEmpty() throws Exception {
        MvcResult result = mockMvc.perform(get("/parks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<Map<String, Object>> payload = JsonPath.read(jsonResponse, "$.payload");
        String errorMessage = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorMessage));

        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isNull();
        assertThat(payload).isEmpty();
    }
    @Test
    public void testGetVisitsSuccess() throws Exception {
        var visit = new Visitas();
        visit.setId(1);
        visit.setUser_id(154);
        visit.setCenter("p1, p2");
        visit.setStartTime(1617640000000L);
        visit.setEnd_time(1617640000000L);
        viajeRepository.save(visit);

        MvcResult result = mockMvc.perform(get("/visitas")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<Map<String, Object>> payload = JsonPath.read(jsonResponse, "$.payload");
        BaseResponse baseResponse = new BaseResponse(jsonResponse, null);

        assertThat(baseResponse.getErrorResponse()).isNull();
        assertThat(payload).isNotNull();
    }
    @Test
    public void testGetVisitsEmpty() throws Exception {
        MvcResult result = mockMvc.perform(get("/visitas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<Map<String, Object>> payload = JsonPath.read(jsonResponse, "$.payload");
        String errorMessage = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorMessage));

        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isNull();
        assertThat(baseResponse.getPayloadAsString(payload)).isNull();
    }
    @Test
    public void testDeleteParkSuccess() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList("40.7128; -74.0060"));
        parkRepository.save(park1);

        MvcResult result = mockMvc.perform(delete("/delete/park")
                        .param("name", "Test 1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorMessage = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorMessage));

        assertTrue(parkRepository.findByName("Test 1").isDeleted());
        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isNull();
        assertThat(baseResponse.getPayload()).isEqualTo("Public space deleted successfully");
    }
    @Test
    public void testDeleteParkException() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList("40.7128; -74.0060"));
        parkRepository.save(park1);

        MvcResult result = mockMvc.perform(delete("/delete/park")
                        .param("name", "NonExistentPark")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorMessage = JsonPath.read(jsonResponse, "$.errorResponse.errorMessage");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(jsonResponse));

        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isNotNull();
        assertFalse(parkRepository.findByName("Test 1").isDeleted());
        assertThat(errorMessage).isEqualTo("Park not found");
        assertThat(payload).isNull();
    }
    @Test
    public void testUpdateParkNameSuccess() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList("40.7128; -74.0060"));
        parkRepository.save(park1);

        MvcResult result = mockMvc.perform(put("/update/park")
                        .param("name", "Test 1")
                        .param("newName", "New Name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorMessage = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorMessage));

        assertThat(baseResponse.getErrorResponse()).isNotNull();
        assertThat(baseResponse.getPayloadAsString(payload)).isEqualTo("Public space's name updated!");

        Park updatedPark = parkRepository.findByName("New Name");
        assertThat(updatedPark).isNotNull();
        assertThat(updatedPark.getName()).isEqualTo("New Name");
    }
    @Test
    public void testUpdateParkNameException() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList("40.7128; -74.0060"));
        parkRepository.save(park1);

        MvcResult result = mockMvc.perform(put("/update/park")
                        .param("name", "NonExistentPark")
                        .param("newName", "NewName")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorMessage = JsonPath.read(jsonResponse, "$.errorResponse.errorMessage");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorMessage));

        // Verifica que la respuesta contenga el mensaje de error esperado
        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isEqualTo("Park not found");
    }
    @Test
    public void testUploadCsvSuccess() throws Exception {
        String csvContent = "id,user_id,coordinate,start_time,end_time\n"
                + "1,100,\"(40.7128,-74.0060)\",1609459200000,1609462800000\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes());

        MvcResult result = mockMvc.perform(multipart("/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorResponse = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isNull();
        assertThat(baseResponse.getPayload()).isEqualTo("Archivo procesado y datos guardados en la base de datos.");

        List<Visitas> visitasList = viajeRepository.findAll();
        assertThat(visitasList).hasSize(1);
        assertThat(visitasList.get(0).getId()).isEqualTo(1);
        assertThat(visitasList.get(0).getUser_id()).isEqualTo(100);
        assertThat(visitasList.get(0).getCenter()).isEqualTo("(40.7128,-74.0060)");
    }
    @Test
    public void testUploadCsvEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        MvcResult result = mockMvc.perform(multipart("/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorResponse = JsonPath.read(jsonResponse, "$.errorResponse");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getErrorResponse().getErrorMessage()).isNull();
        assertThat(baseResponse.getPayload()).isEqualTo("El archivo esta vacio");

        List<Visitas> visitasList = viajeRepository.findAll();
        assertThat(visitasList).isEmpty();
    }
    @Test
    public void testUploadCsvMalformed() throws Exception {
        String csvContent = "id,user_id,coordinate,start_time,end_time\n"
                + "1,100,\"(40.7128,-74.0060)\"\n"; // Falta start_time y end_time

        MockMultipartFile file = new MockMultipartFile(
                "file", "malformed.csv", "text/csv", csvContent.getBytes());

        MvcResult result = mockMvc.perform(multipart("/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String payload = JsonPath.read(jsonResponse, "$.payload");
        String errorResponse = JsonPath.read(jsonResponse, "$.errorResponse.errorMessage");
        BaseResponse baseResponse = new BaseResponse(payload, new ErrorResponse(errorResponse));

        assertThat(baseResponse.getPayload()).isNull();
        assertThat(baseResponse.getErrorResponse().getErrorMessage()).contains("Error: Index 4 out of bounds for length 4");

        List<Visitas> visitasList = viajeRepository.findAll();
        assertThat(visitasList).isEmpty();
    }
    @Test
    public void testGetMarkersSuccess() throws Exception {
        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList(
                "-37.3266645; -59.1290909",
                "-37.3261356; -59.127699",
                "-37.3262184; -59.1276492",
                "-37.3271408; -59.1270942",
                "-37.3272499; -59.1270286",
                "-37.3277621; -59.1284428",
                "-37.3266645; -59.1290909"
        ));
        parkRepository.save(park1);

        Visitas visita1 = new Visitas();
        visita1.setId(1);
        visita1.setUser_id(100);
        visita1.setCenter("(-37.326952, -59.128071)");
        visita1.setStartTime(1577847600000L); // 01-01-2021
        visita1.setEnd_time(1640919600000L); // 02-01-2021
        viajeRepository.save(visita1);

//        // Mockear la lectura del archivo JSON
//        String mockJsonContent = """
//        {
//          "version" : 0.6,
//          "generator" : "Overpass API 0.7.62.1 084b4234",
//          "osm3s" : {
//            "timestamp_osm_base" : "2024-09-02T20:01:44Z",
//            "timestamp_areas_base" : "2024-09-02T14:00:16Z",
//            "copyright" : "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
//          },
//          "elements" : [ {
//            "type" : "node",
//            "id" : 10805479623,
//            "lat" : -37.151152,
//            "lon" : -58.4748928,
//            "tags" : {
//              "leisure" : "park",
//              "name" : "Plaza Eva Perón"
//            }
//          }]
//        }
//        """;
//
//        Mockito.when(crowdsensingService.getMarkers(
//                any(LocalDate.class),
//                any(LocalDate.class),
//                Mockito.anyString()
//        )).thenReturn(ResponseEntity.ok(mockJsonContent));

        LocalDate initialDate = LocalDate.of(2020, 1, 1);
        LocalDate finalDate = LocalDate.of(2021, 12, 31);
        String tag = "park";

        MvcResult result = mockMvc.perform(get("/markers")
                        .param("initialDate", String.valueOf(initialDate))
                        .param("finalDate", String.valueOf(finalDate))
                        .param("tag", tag)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        System.out.println(jsonResponse);
        assertTrue(jsonResponse.contains("geocode"));
    }
    @Test
    public void testGetMarkersEmpty() throws Exception {

        var park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("park");
        park1.setCreated(true);
        park1.setDeleted(false);
        park1.setPoints(Arrays.asList(
                "-37.3266645; -59.1290909",
                "-37.3261356; -59.127699",
                "-37.3262184; -59.1276492",
                "-37.3271408; -59.1270942",
                "-37.3272499; -59.1270286",
                "-37.3277621; -59.1284428",
                "-37.3266645; -59.1290909"
        ));
        parkRepository.save(park1);

        Visitas visita1 = new Visitas();
        visita1.setId(1);
        visita1.setUser_id(100);
        visita1.setCenter("(-37.326952, -59.128071)");
        visita1.setStartTime(1577847600000L); // 01-01-2021
        visita1.setEnd_time(1640919600000L); // 02-01-2021
        viajeRepository.save(visita1);


        LocalDate initialDate = LocalDate.of(2020, 1, 1);
        LocalDate finalDate = LocalDate.of(2021, 12, 31);
        String tag = "bar";

        MvcResult result = mockMvc.perform(get("/markers")
                        .param("initialDate", String.valueOf(initialDate))
                        .param("finalDate", String.valueOf(finalDate))
                        .param("tag", tag)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        System.out.println(jsonResponse);
        assertFalse(jsonResponse.contains("geocode"));
    }
}