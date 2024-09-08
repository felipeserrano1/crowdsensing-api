package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.*;
import com.example.apicrowdsensing.repositories.ParkRepository;
import com.example.apicrowdsensing.repositories.ViajeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.*;

class CrowdsensingServiceTest {
    @InjectMocks
    private CrowdsensingService crowdsensingService;
    @Mock
    private ParkRepository parkRepository;
    @Mock
    private ViajeRepository viajeRepository;
    @Mock
    private MultipartFile multipartFile;
    @Mock
    private HttpResponse<String> mockResponse;
    @Mock
    private Unirest mockUnirest;
    @Mock
    private ObjectMapper objectMapper;
    private Park park1, park2;
    private Park lastPark;
    private LocationRequest locationRequest;
    private LocalDate initialDate;
    private LocalDate finalDate;
    private String tag;
    private Path path;
    private List<Park> parks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        openMocks(this);
        park1 = new Park();
        park1.setId(1L);
        park1.setName("Test 1");
        park1.setCity("City");
        park1.setType("Public");
        park1.setPoints(Arrays.asList("40.0; -3.0", "point2"));

        park2 = new Park();
        park2.setId(2L);
        park2.setName("Test 2");
        park2.setCity("City");
        park2.setType("Public");;
        park2.setPoints(Arrays.asList("point3", "point4"));


        lastPark = new Park();
        lastPark.setId(1L);
        lastPark.setName("Parque Antiguo");

        locationRequest = new LocationRequest();
        locationRequest.setName("Nuevo Parque");
        locationRequest.setCity("Nueva Ciudad");
        locationRequest.setType("Publico");

        List<List<String>> positions = new ArrayList<>();
        positions.add(Arrays.asList("34.0", "-58.0"));
        positions.add(Arrays.asList("34.1", "-58.1"));
        locationRequest.setPositions(positions);

        initialDate = LocalDate.of(2024, 8, 1);
        finalDate = LocalDate.of(2024, 8, 31);
        tag = "someTag";

        path = Paths.get("src", "main", "resources", "response.json");
        parks.add(park1);
    }
    @Test
    void getParks_ShouldReturnListOfParks() {
        when(parkRepository.findAll()).thenReturn(Arrays.asList(park1, park2));

        List<Park> parks = crowdsensingService.getParks();

        assertNotNull(parks);
        assertEquals(2, parks.size());
        assertEquals("Test 1", parks.get(0).getName());
        assertEquals("Test 2", parks.get(1).getName());

        verify(parkRepository, times(1)).findAll();
    }
    @Test
    void getParks_ShouldReturnEmptyList() {
        when(parkRepository.findAll()).thenReturn(Arrays.asList());

        List<Park> parks = crowdsensingService.getParks();

        assertNotNull(parks);
        assertTrue(parks.isEmpty());

        verify(parkRepository, times(1)).findAll();
    }
    @Test
    void createLocation_ShouldCreateNewParkWithIncrementedId() {
        when(parkRepository.findTopByOrderByIdDesc()).thenReturn(lastPark);

        String response = crowdsensingService.createLocation(locationRequest);

        assertEquals("New location created!", response);

        verify(parkRepository, times(1)).save(any(Park.class));

        ArgumentCaptor<Park> parkCaptor = ArgumentCaptor.forClass(Park.class);
        verify(parkRepository).save(parkCaptor.capture());
        Park savedPark = parkCaptor.getValue();

        assertEquals(2L, savedPark.getId());  // ID incrementado
        assertEquals("Nuevo Parque", savedPark.getName());
        assertEquals("Nueva Ciudad", savedPark.getCity());
        assertEquals("Publico", savedPark.getType());
        assertEquals(Arrays.asList("34.0; -58.0", "34.1; -58.1"), savedPark.getPoints());
        assertTrue(savedPark.isCreated());
    }
    @Test
    void createLocation_ShouldNotHandleEmptyPositions() {
        locationRequest.setPositions(new ArrayList<>());

        when(parkRepository.findTopByOrderByIdDesc()).thenReturn(lastPark);

        String response = crowdsensingService.createLocation(locationRequest);

        verify(parkRepository, never()).save(any(Park.class));

        assertEquals("The list of points is empty.", response);
    }
    @Test
    void deletePark_ShouldMarkParkAsDeletedAndSave() {
        when(parkRepository.findByName("Test 1")).thenReturn(park1);

        String response = crowdsensingService.deletePark("Test 1");

        assertTrue(park1.isDeleted());

        verify(parkRepository, times(1)).save(park1);

        assertEquals("Public space deleted!", response);
    }
    @Test
    void deletePark_ShouldReturnErrorMessage_WhenParkNotFound() {
        when(parkRepository.findByName("Parque Inexistente")).thenThrow(new RuntimeException("Park not found"));

        String response = crowdsensingService.deletePark("Parque Inexistente");

        verify(parkRepository, never()).save(any(Park.class));

        assertEquals("Park not found", response);
    }
    @Test
    void deletePark_ShouldHandleExceptionAndReturnErrorMessage() {
        when(parkRepository.findByName("Test 1")).thenThrow(new RuntimeException("Unexpected error"));

        String response = crowdsensingService.deletePark("Test 1");

        verify(parkRepository, never()).save(any(Park.class));

        assertEquals("Unexpected error", response);
    }
    @Test
    void updateParkName_ShouldUpdateParkNameAndSave() {
        when(parkRepository.findByName("Test 1")).thenReturn(park1);

        String response = crowdsensingService.updateParkName("Test 1", "Test 2");

        verify(parkRepository, times(1)).save(any(Park.class));

        ArgumentCaptor<Park> parkArgumentCaptor = ArgumentCaptor.forClass(Park.class);
        verify(parkRepository).save(parkArgumentCaptor.capture());
        Park savedPark = parkArgumentCaptor.getValue();

        assertEquals(savedPark.getName(), "Test 2");
        assertEquals("Public space's name updated!", response);
    }
    @Test
    void updateParkName_ShouldReturnErrorMessage_WhenParkNotFound() {
        when(parkRepository.findByName("Parque Inexistente")).thenThrow(new RuntimeException("Park not found"));

        String response = crowdsensingService.updateParkName("Nonexistant Park", "New Name");

        verify(parkRepository, never()).save(any(Park.class));

        assertEquals("Park not found", response);
    }
    @Test
    void uploadCsv_ShouldReturnEmptyFileMessage_WhenFileIsEmpty() {
        when(multipartFile.isEmpty()).thenReturn(true);

        String result = crowdsensingService.uploadCsv(multipartFile);
        assertEquals("El archivo está vacío", result);

        verify(viajeRepository, never()).saveAll(anyList());
    }
    @Test
    void uploadCsv_ShouldProcessFileAndSaveData_WhenFileIsValid() throws Exception {
        String csvContent = "id,user_id,center,center,start_time,end_time\n" +
                "1,101,(45.123,-93.123),1456789.123,1234567890,1234567891\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = crowdsensingService.uploadCsv(multipartFile);

        assertEquals("Archivo procesado y datos guardados en la base de datos.", result);

        verify(viajeRepository, times(1)).saveAll(anyList());
    }
    @Test
    void uploadCsv_ShouldReturnErrorMessage_WhenExceptionOccurs() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenThrow(new RuntimeException("Test exception"));

        String result = crowdsensingService.uploadCsv(multipartFile);

        assertTrue(result.startsWith("Error al procesar el archivo"));
        assertEquals("Error al procesar el archivo: Test exception", result);

        verify(viajeRepository, never()).saveAll(anyList());
    }
    @Test
    void pointInsidePoligon_ShouldReturnTrue_WhenPointIsInsidePolygon() {
        List<String> polygon = Arrays.asList(
                "0.0; 0.0",   // punto inferior izquierdo
                "0.0; 10.0",  // punto superior izquierdo
                "10.0; 10.0", // punto superior derecho
                "10.0; 0.0"   // punto inferior derecho
        );

        Point pointInside = new Point(5.0, 5.0);

        assertTrue(crowdsensingService.pointInsidePoligon(pointInside, polygon));
    }

    @Test
    void pointInsidePoligon_ShouldReturnFalse_WhenPointIsOutsidePolygon() {
        List<String> polygon = Arrays.asList(
                "0.0; 0.0",
                "0.0; 10.0",
                "10.0; 10.0",
                "10.0; 0.0"
        );

        Point pointOutside = new Point(15.0, 5.0);

        assertFalse(crowdsensingService.pointInsidePoligon(pointOutside, polygon));
    }
    @Test
    void pointInsidePoligon_ShouldReturnTrue_WhenPointIsOnEdgeOfPolygon() {
        List<String> polygon = Arrays.asList(
                "0.0; 0.0",
                "0.0; 10.0",
                "10.0; 10.0",
                "10.0; 0.0"
        );

        Point pointOnEdge = new Point(0.0, 5.0);

        assertTrue(crowdsensingService.pointInsidePoligon(pointOnEdge, polygon));
    }
    @Test
    void pointInsidePoligon_ShouldReturnFalse_WhenPolygonIsEmpty() {
        List<String> emptyPolygon = Arrays.asList();

        Point point = new Point(1.0, 1.0);

        assertFalse(crowdsensingService.pointInsidePoligon(point, emptyPolygon));
    }
    @Test
    void parseBoundsFromJsonResponse_ValidResponse() throws Exception {
        String jsonResponse = "{ \"elements\": [{ \"bounds\": { \"minlat\": -34.6, \"minlon\": -58.4, \"maxlat\": -34.5, \"maxlon\": -58.3 } }] }";

        Method method = CrowdsensingService.class.getDeclaredMethod("parseBoundsFromJsonResponse", String.class);
        method.setAccessible(true);

        JSONObject result = (JSONObject) method.invoke(crowdsensingService, jsonResponse);

        assertNotNull(result);
        assertEquals(-34.6, result.getDouble("minlat"));
        assertEquals(-58.4, result.getDouble("minlon"));
        assertEquals(-34.5, result.getDouble("maxlat"));
        assertEquals(-58.3, result.getDouble("maxlon"));
    }
    @Test
    void parseBoundsFromJsonResponse_EmptyResponse() throws Exception {
        String jsonResponse = "{ \"elements\": [] }";

        Method method = CrowdsensingService.class.getDeclaredMethod("parseBoundsFromJsonResponse", String.class);
        method.setAccessible(true);

        JSONObject result = (JSONObject) method.invoke(crowdsensingService, jsonResponse);

        assertNull(result);
    }
    @Test
    void getTraficByPark_ShouldReturnCorrectTrafficData() {
        Park park = new Park();
        park.setPoints(Arrays.asList("lat1; lon1", "lat2; lon2", "lat3; lon3"));

        Visitas visita1 = new Visitas();
        visita1.setCenter("(12.34, 56.78)");
        visita1.setStartTime(Instant.now().minusMillis(10000).toEpochMilli());

        Visitas visita2 = new Visitas();
        visita2.setCenter("(12.34, 56.78)");
        visita2.setStartTime(Instant.now().minusMillis(5000).toEpochMilli());

        List<Visitas> visitas = Arrays.asList(visita1, visita2);

        CrowdsensingService spyService = spy(crowdsensingService);
        doReturn(true).when(spyService).pointInsidePoligon(any(Point.class), anyList());
        ArrayList<DateTraffic> result = spyService.getTraficByPark(park, visitas);

        assertEquals(1, result.size(), "Should have one entry in DateTraffic list");
        assertEquals(2, result.get(0).getTraffic(), "Should have 2 visits counted");
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(visita1.getStartTime()), ZoneId.systemDefault()), result.get(0).getDate());
    }
    @Test
    void getTraficByPark_ShouldReturnEmptyList_WhenNoVisitsInsidePark() {
        Park park = new Park();
        park.setPoints(Arrays.asList("lat1; lon1", "lat2; lon2", "lat3; lon3"));

        Visitas visita1 = new Visitas();
        visita1.setCenter("(12.34, 56.78)");
        visita1.setStartTime(Instant.now().minusMillis(10000).toEpochMilli());

        List<Visitas> visitas = Arrays.asList(visita1);

        CrowdsensingService spyService = spy(crowdsensingService);
        doReturn(false).when(spyService).pointInsidePoligon(any(Point.class), anyList());
        ArrayList<DateTraffic> result = spyService.getTraficByPark(park, visitas);

        assertEquals(0, result.size(), "Should return an empty list if no visits are inside the park");
    }
    @Test
    void getMarker_ShouldReturnValidResponse() throws IOException {
        JsonNode jsonNodeMock = mock(JsonNode.class);
        when(objectMapper.readTree(new File(path.toString()))).thenReturn(jsonNodeMock);
        when(jsonNodeMock.get("elements")).thenReturn(mock(JsonNode.class));

        List<Visitas> visitasMock = mock(List.class);
        when(viajeRepository.findByStartTimeBetween(anyLong(), anyLong())).thenReturn(visitasMock);

        List<Park> parksCreatedMock = mock(List.class);
        when(parkRepository.findAllByCityAndCreatedIsTrueAndTypeAndDeletedIsFalse(anyString(), anyString())).thenReturn(parksCreatedMock);

        ResponseEntity<String> response = crowdsensingService.getMarkers(LocalDate.now(), LocalDate.now().plusDays(1), "tag");

        assertNotNull(response);
        verify(viajeRepository, times(1)).findByStartTimeBetween(anyLong(), anyLong());
        verify(parkRepository, times(1)).findAllByCityAndCreatedIsTrueAndTypeAndDeletedIsFalse(anyString(), anyString());
        verify(objectMapper, times(1)).readTree(any(File.class));
    }
    @Test
    void getMarkers_ShouldThrowException_WhenJsonParsingFails() throws IOException {
        when(objectMapper.readTree(any(File.class))).thenThrow(IOException.class);

        assertThrows(IOException.class, () -> {
            crowdsensingService.getMarkers(LocalDate.now(), LocalDate.now().plusDays(1), "tag");
        });

        verify(objectMapper, times(1)).readTree(any(File.class));
        verifyNoInteractions(viajeRepository); // No debería interactuar con el repositorio
        verifyNoInteractions(parkRepository);  // No debería interactuar con el repositorio
    }
    @Test
    void filterElements_ShouldReturnValidResponse_WhenElementsArrayContainsData() throws JsonProcessingException {
        JsonNode elementNode = mock(JsonNode.class);
        JsonNode tagsNode = mock(JsonNode.class);

        when(elementNode.get("tags")).thenReturn(tagsNode);
        when(elementNode.get("id")).thenReturn(new IntNode(1));
        when(elementNode.get("lat")).thenReturn(new DoubleNode(40.0));
        when(elementNode.get("lon")).thenReturn(new DoubleNode(-3.0));

        ArrayNode elementsArrayNode = mock(ArrayNode.class);
        when(elementsArrayNode.elements()).thenReturn(Collections.singletonList(elementNode).iterator());

        List<Visitas> visitasMock = new ArrayList<>();
        List<Park> parksCreatedMock = new ArrayList<>();
        parksCreatedMock.add(park1);

        CrowdsensingService crowdsensingServiceSpy = spy(crowdsensingService);
        ResponseEntity<String> response = crowdsensingService.filterElements(elementsArrayNode, visitasMock, parksCreatedMock);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    @Test
    void filterElements_ShouldReturnEmptyResponse_WhenElementsArrayIsNull() throws JsonProcessingException {
        List<Visitas> visitasMock = new ArrayList<>();
        List<Park> parksCreatedMock = new ArrayList<>();

        ResponseEntity<String> response = crowdsensingService.filterElements(null, visitasMock, parksCreatedMock);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }
    @Test
    void loadParks_ShouldAddParks_WhenElementsAreInRange() {
        JsonNode elementsArrayMock = mock(JsonNode.class);
        JsonNode elementMock = mock(JsonNode.class);
        JsonNode geometryMock = mock(JsonNode.class);
        JsonNode tagsMock = mock(JsonNode.class);
        JsonNode geometryElementMock = mock(JsonNode.class);

        when(elementsArrayMock.isArray()).thenReturn(true);
        when(elementsArrayMock.elements()).thenReturn(mock(Iterator.class));

        Iterator<JsonNode> elementsIterator = mock(Iterator.class);
        when(elementsIterator.hasNext()).thenReturn(true, false);
        when(elementsIterator.next()).thenReturn(elementMock);
        when(elementsArrayMock.elements()).thenReturn(elementsIterator);

        when(elementMock.has("geometry")).thenReturn(true);
        when(elementMock.get("geometry")).thenReturn(geometryMock);
        when(elementMock.get("tags")).thenReturn(tagsMock);
        when(elementMock.get("id")).thenReturn(mock(JsonNode.class));
        when(elementMock.get("id").asLong()).thenReturn(123L);

        when(tagsMock.get("name")).thenReturn(mock(JsonNode.class));
        when(tagsMock.get("name").asText()).thenReturn("Test Park");

        Iterator<JsonNode> geometryIterator = mock(Iterator.class);
        when(geometryIterator.hasNext()).thenReturn(true, false);
        when(geometryIterator.next()).thenReturn(geometryElementMock);
        when(geometryMock.elements()).thenReturn(geometryIterator);

        when(geometryElementMock.get("lat")).thenReturn(mock(JsonNode.class));
        when(geometryElementMock.get("lat").asDouble()).thenReturn(10.0);

        when(geometryElementMock.get("lon")).thenReturn(mock(JsonNode.class));
        when(geometryElementMock.get("lon").asDouble()).thenReturn(20.0);

        Double minlat = 5.0, maxlat = 15.0, minlon = 15.0, maxlon = 25.0;

        crowdsensingService.loadParks(elementsArrayMock, "park", minlat, maxlat, minlon, maxlon);

        verify(parkRepository, times(1)).save(any(Park.class));
        verify(elementsArrayMock, times(1)).elements();
    }
    @Test
    void loadParks_ShouldNotAddParks_WhenElementsAreOutOfRange() {
        JsonNode elementsArrayMock = mock(JsonNode.class);
        JsonNode elementMock = mock(JsonNode.class);
        JsonNode geometryMock = mock(JsonNode.class);
        JsonNode tagsMock = mock(JsonNode.class);
        JsonNode geometryElementMock = mock(JsonNode.class);

        when(elementsArrayMock.isArray()).thenReturn(true);
        when(elementsArrayMock.elements()).thenReturn(mock(Iterator.class));

        Iterator<JsonNode> elementsIterator = mock(Iterator.class);
        when(elementsIterator.hasNext()).thenReturn(true, false);
        when(elementsIterator.next()).thenReturn(elementMock);
        when(elementsArrayMock.elements()).thenReturn(elementsIterator);

        when(elementMock.has("geometry")).thenReturn(true);
        when(elementMock.get("geometry")).thenReturn(geometryMock);
        when(elementMock.get("tags")).thenReturn(tagsMock);
        when(elementMock.get("id")).thenReturn(mock(JsonNode.class));
        when(elementMock.get("id").asLong()).thenReturn(123L);

        when(tagsMock.get("name")).thenReturn(mock(JsonNode.class));
        when(tagsMock.get("name").asText()).thenReturn("Test Park");

        Iterator<JsonNode> geometryIterator = mock(Iterator.class);
        when(geometryIterator.hasNext()).thenReturn(true, false);
        when(geometryIterator.next()).thenReturn(geometryElementMock);
        when(geometryMock.elements()).thenReturn(geometryIterator);

        when(geometryElementMock.get("lat")).thenReturn(mock(JsonNode.class));
        when(geometryElementMock.get("lat").asDouble()).thenReturn(50.0);

        when(geometryElementMock.get("lon")).thenReturn(mock(JsonNode.class));
        when(geometryElementMock.get("lon").asDouble()).thenReturn(60.0);

        Double minlat = 5.0, maxlat = 15.0, minlon = 15.0, maxlon = 25.0;

        crowdsensingService.loadParks(elementsArrayMock, "park", minlat, maxlat, minlon, maxlon);

        verify(parkRepository, times(0)).save(any(Park.class)); // Should not save
    }

//    @Test
//    void getBoundsCity_ShouldReturnBounds_WhenCityIsAyacucho() throws Exception {
//        // Mocking the HTTP response
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(mockResponse.getStatus()).thenReturn(200);
//        when(mockResponse.getBody()).thenReturn("{\"elements\":[]}");
//
//        // Mocking Unirest call
//        mockStatic(Unirest.class);
//        when(Unirest.get(anyString())).thenReturn(mockResponse);
//
//        // Calling the method with "Ayacucho"
//        JSONObject result = crowdsensingService.getBoundsCity("Ayacucho", "Argentina");
//
//        // Verifications
//        assertNotNull(result);
//        verifyStatic(Unirest.class, times(1));
//
//        Unirest.get(contains("[out:json];area['name'='Buenos Aires']->.searchArea;relation(area.searchArea)['name'='Ayacucho']['boundary'='administrative'];out geom;"));
//    }
//    @Test
//    void getQuery_ShouldProcessResponseCorrectly() throws Exception {
//        // Mocking the Unirest HTTP response
//        HttpResponse<String> responseMock = mock(HttpResponse.class);
//        when(responseMock.getBody()).thenReturn("{\"elements\": []}");
//        //when(Unirest.post(anyString()).header(anyString(), anyString()).body(anyString()).asString()).thenReturn(responseMock);
//
//        // Mocking ObjectMapper behavior
//        JsonNode mockJsonNode = mock(JsonNode.class);
//        when(objectMapper.readTree(any(File.class))).thenReturn(mockJsonNode);
//        when(mockJsonNode.get("elements")).thenReturn(mock(JsonNode.class));
//
//        // Mocking getBoundsCity method
//        JSONObject mockBounds = new JSONObject();
//        mockBounds.put("minlat", 10.0);
//        mockBounds.put("minlon", 20.0);
//        mockBounds.put("maxlat", 30.0);
//        mockBounds.put("maxlon", 40.0);
//
//        CrowdsensingService spyService = spy(crowdsensingService);
//        doReturn(mockBounds).when(spyService).getBoundsCity(anyString(), anyString());
//
//        // Mocking loadParks method
//        doNothing().when(spyService).loadParks(any(JsonNode.class), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
//
//        // Call the method under test
//        String result = spyService.getQuery("TestCity", "TestTag");
//
//        // Verify loadParks is called with the correct arguments
//        ArgumentCaptor<Double> latCaptor = ArgumentCaptor.forClass(Double.class);
//        ArgumentCaptor<Double> lonCaptor = ArgumentCaptor.forClass(Double.class);
//        verify(spyService).loadParks(any(JsonNode.class), anyString(), latCaptor.capture(), latCaptor.capture(), lonCaptor.capture(), lonCaptor.capture());
////
////        // Validate the captured values (minlat, maxlat, minlon, maxlon)
//        assertEquals(10.0, latCaptor.getAllValues().get(0));
//        assertEquals(30.0, latCaptor.getAllValues().get(1));
//        assertEquals(20.0, lonCaptor.getAllValues().get(0));
//        assertEquals(40.0, lonCaptor.getAllValues().get(1));
//
//        // Validate the response
//        assertEquals("Overpass queried!", result);
//    }
//    @Test
//    void getQuery_ShouldSaveParks_WhenResponseIsValid() throws Exception {
//        // Simulamos la respuesta de getBoundsCity
//        JSONObject mockBounds = new JSONObject();
//        mockBounds.put("minlat", -34.6);
//        mockBounds.put("minlon", -58.4);
//        mockBounds.put("maxlat", -34.5);
//        mockBounds.put("maxlon", -58.3);
//
//        // Simulamos el comportamiento de getBoundsCity
//        when(crowdsensingService.getBoundsCity(anyString(), anyString())).thenReturn(mockBounds);
//
//        // Simulamos la respuesta de la API de Overpass
//        when(mockResponse.getBody()).thenReturn("{\"elements\": [{\"id\": 12345, \"tags\": {\"name\": \"Parque Test\"}, \"geometry\": [{\"lat\": -34.55, \"lon\": -58.35}]}]}");
//        when(Unirest.post(anyString()).header(anyString(), anyString()).body(anyString()).asString()).thenReturn(mockResponse);
//
//        // Simulamos la conversión del JSON
//        JsonNode mockJsonNode = new ObjectMapper().readTree(new File("src/test/resources/mock_response.json"));
//        when(objectMapper.readTree(any(File.class))).thenReturn(mockJsonNode);
//
//        // Llamamos al método
//        String result = crowdsensingService.getQuery("City", "park");
//
//        // Verificamos que el parque haya sido guardado
//        verify(parkRepository, times(1)).save(any(Park.class));
//

//        // Verificamos que el mensaje de éxito se devolvió

//        assertEquals("Overpass queried!", result);

//    }

//    @Test
//    void getBoundsCity_Success() throws Exception {
//        // Arrange
//        String city = "Buenos Aires";
//        String country = "Argentina";
//        String jsonResponse = "{ \"bounds\": { \"minlat\": -34.6, \"minlon\": -58.4, \"maxlat\": -34.5, \"maxlon\": -58.3 } }";
//
//        // Mockear la respuesta de la API
//        when(mockResponse.getStatus()).thenReturn(200);
//        when(mockResponse.getBody()).thenReturn(jsonResponse);
//        //when(Unirest.get(anyString())).thenReturn(mockResponse);
//        //when(mockUnirest.toString()).thenReturn(mockResponse);
//
//        // Mockear el método parseBoundsFromJsonResponse si es necesario
//        // Puedes usar un espía (spy) si el método es parte de la clase en cuestión.
//
//        // Act
//        JSONObject result = crowdsensingService.getBoundsCity(city, country);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(-34.6, result.getDouble("minlat"));
//        assertEquals(-58.4, result.getDouble("minlon"));
//        assertEquals(-34.5, result.getDouble("maxlat"));
//        assertEquals(-58.3, result.getDouble("maxlon"));
//    }
//
//
//    @Test
//    void testGetBoundsCity() throws Exception {
//
//        // Usar Mockito para mockear Unirest
//        try (MockedStatic<Unirest> mockedUnirest = Mockito.mockStatic(Unirest.class)) {
//            // Configurar el comportamiento de Unirest.get() para devolver una respuesta simulada
//            HttpResponse<String> mockResponse = mock(HttpResponse.class);
//            when(mockResponse.getStatus()).thenReturn(200);
//            when(mockResponse.getBody()).thenReturn("{\"elements\": [{\"bounds\": {\"minlat\": -34.6, \"minlon\": -58.4, \"maxlat\": -34.5, \"maxlon\": -58.3}}]}");
//            mockedUnirest.when(() -> Unirest.get(anyString())).thenReturn(mockResponse);
//
//            // Actuar
//            JSONObject result = crowdsensingService.getBoundsCity("Buenos Aires", "Argentina");
//
//            // Assert
//            assertNotNull(result);
//            assertEquals(-34.6, result.getJSONObject("bounds").getDouble("minlat"));
//            assertEquals(-58.4, result.getJSONObject("bounds").getDouble("minlon"));
//            assertEquals(-34.5, result.getJSONObject("bounds").getDouble("maxlat"));
//            assertEquals(-58.3, result.getJSONObject("bounds").getDouble("maxlon"));
//        }
//    }



}