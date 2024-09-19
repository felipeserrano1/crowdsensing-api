package com.example.apicrowdsensing.services;

import com.example.apicrowdsensing.models.*;
import com.example.apicrowdsensing.repositories.PublicSpaceRepository;
import com.example.apicrowdsensing.repositories.VisitRepository;
import com.example.apicrowdsensing.utils.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class CrowdsensingServiceTest {
    @InjectMocks
    private CrowdsensingService crowdsensingService;
    @Mock
    private PublicSpaceRepository publicSpaceRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private MultipartFile multipartFile;
    @Mock
    private Utils utils;
    private PublicSpace publicSpace1, publicSpace2;
    private final List<PublicSpace> publicSpaces = new ArrayList<>();

    @BeforeEach
    void setUp() {
        openMocks(this);
        publicSpace1 = new PublicSpace();
        publicSpace1.setId(1L);
        publicSpace1.setName("test1");
        publicSpace1.setCity("city");
        publicSpace1.setType("park");
        publicSpace1.setPoints(Arrays.asList("40.0; -3.0", "point2"));

        publicSpace2 = new PublicSpace();
        publicSpace2.setId(2L);
        publicSpace2.setName("test2");
        publicSpace2.setCity("city");
        publicSpace2.setType("park");
        publicSpace2.setPoints(Arrays.asList("point3", "point4"));


        PublicSpace lastPublicSpace = new PublicSpace();
        lastPublicSpace.setId(1L);
        lastPublicSpace.setName("name");

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName("name");
        locationRequest.setCity("city");
        locationRequest.setType("type");

        List<List<String>> positions = new ArrayList<>();
        positions.add(Arrays.asList("34.0", "-58.0"));
        positions.add(Arrays.asList("34.1", "-58.1"));
        locationRequest.setPositions(positions);

        publicSpaces.add(publicSpace1);
    }
    @Test
    void getPublicSpaces_ShouldReturnListOfParks() throws CustomException {
        when(publicSpaceRepository.findAll()).thenReturn(Arrays.asList(publicSpace1, publicSpace2));

        List<PublicSpace> publicSpaces = crowdsensingService.getPublicSpaces();

        assertNotNull(publicSpaces);
        assertEquals(2, publicSpaces.size());
        assertEquals("test1", publicSpaces.get(0).getName());
        assertEquals("test2", publicSpaces.get(1).getName());

        verify(publicSpaceRepository, times(1)).findAll();
    }
    @Test
    void getPublicSpaces_ShouldReturnEmptyList() {
        when(publicSpaceRepository.findAll()).thenReturn(List.of());
        assertThrows(CustomException.class, () -> crowdsensingService.getPublicSpaces());
    }
    @Test
    void createLocation_ShouldSavePublicSpace_WhenRequestIsValid() throws CustomException {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setType("park");
        locationRequest.setName("test");
        locationRequest.setCity("city");

        List<List<String>> positions = new ArrayList<>();
        positions.add(Arrays.asList("40.785091", "-73.968285"));
        locationRequest.setPositions(positions);

        PublicSpace newPublicSpace = new PublicSpace();
        when(publicSpaceRepository.save(any(PublicSpace.class))).thenReturn(newPublicSpace);

        crowdsensingService.createLocation(locationRequest);

        verify(publicSpaceRepository, times(1)).save(any(PublicSpace.class));
    }
    @Test
    void createLocation_ShouldThrowException_WhenPositionsListIsEmpty() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setType("park");
        locationRequest.setName("test");
        locationRequest.setCity("city");

        List<List<String>> positions = new ArrayList<>();
        positions.add(new ArrayList<>());
        locationRequest.setPositions(positions);

        CustomException exception = assertThrows(CustomException.class, () -> crowdsensingService.createLocation(locationRequest));

        assertEquals("List of points is empty", exception.getMessage());
        verify(publicSpaceRepository, never()).save(any(PublicSpace.class));
    }
    @Test
    void deletePark_ShouldMarkParkAsDeletedAndSave() throws CustomException {
        when(publicSpaceRepository.findById(1L)).thenReturn(publicSpace1);

        crowdsensingService.softDeletePublicSpace(1L);

        assertTrue(publicSpace1.isDeleted());

        verify(publicSpaceRepository, times(1)).save(publicSpace1);
    }
    @Test
    void deletePark_ShouldReturnErrorMessage_WhenParkNotFound() {
        when(publicSpaceRepository.findById(100L)).thenReturn(null);
        assertThrows(CustomException.class, () -> crowdsensingService.softDeletePublicSpace(100L));

        verify(publicSpaceRepository, never()).save(any(PublicSpace.class));
    }
    @Test
    void updateParkName_ShouldUpdateParkNameAndSave() throws CustomException {
        when(publicSpaceRepository.findById(1L)).thenReturn(publicSpace1);

        crowdsensingService.updatePublicSpaceName(1L, "new name");

        verify(publicSpaceRepository, times(1)).save(any(PublicSpace.class));

        ArgumentCaptor<PublicSpace> parkArgumentCaptor = ArgumentCaptor.forClass(PublicSpace.class);
        verify(publicSpaceRepository).save(parkArgumentCaptor.capture());
        PublicSpace savedPublicSpace = parkArgumentCaptor.getValue();

        assertEquals(savedPublicSpace.getName(), "new name");
    }
    @Test
    void updateParkName_ShouldReturnErrorMessage_WhenParkNotFound() {
        when(publicSpaceRepository.findById(100L)).thenReturn(null);

        assertThrows(CustomException.class, () -> crowdsensingService.updatePublicSpaceName(100L, "new name"));

        verify(publicSpaceRepository, never()).save(any(PublicSpace.class));

    }
    @Test
    void uploadCsv_ShouldProcessFileAndSaveData_WhenFileIsValid() throws Exception {
        String csvContent = """
                id,user_id,center,center,start_time,end_time
                1,101,(45.123,-93.123),1456789.123,1234567890,1234567891
                """;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        crowdsensingService.uploadCsv(multipartFile);

        verify(visitRepository, times(1)).saveAll(anyList());
    }
    @Test
    void uploadCsv_ShouldReturnEmptyFileMessage_WhenFileIsEmpty() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(CustomException.class, () -> crowdsensingService.uploadCsv(multipartFile));

        verify(visitRepository, never()).saveAll(anyList());
    }
    @Test
    void uploadCsv_ShouldReturnErrorMessage_WhenExceptionOccurs() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenThrow(new IOException());

        assertThrows(IOException.class, () -> crowdsensingService.uploadCsv(multipartFile));

        verify(visitRepository, never()).saveAll(anyList());
    }
    @Test
    public void testGetTrafficByPublicSpace() {
        PublicSpace publicSpace = mock(PublicSpace.class);
        List<String> points = Arrays.asList("1; 1", "2; 2");
        when(publicSpace.getPoints()).thenReturn(points);

        Visit visit1 = mock(Visit.class);
        when(visit1.getCenter()).thenReturn("(1.5,1.5)");
        when(visit1.getStartTime()).thenReturn(Instant.now().toEpochMilli());

        Visit visit2 = mock(Visit.class);
        when(visit2.getCenter()).thenReturn("(3.0,3.0)");
        when(visit2.getStartTime()).thenReturn(Instant.now().toEpochMilli());

        List<Visit> visits = Arrays.asList(visit1, visit2);

        when(utils.pointInsidePoligon(any(Point.class), eq(points))).thenReturn(true).thenReturn(false);

        ArrayList<DateTraffic> result = crowdsensingService.getTrafficByPublicSpace(publicSpace, visits);

        assertEquals(1, result.size());
        DateTraffic dateTraffic = result.get(0);
        assertEquals(LocalDate.now(), dateTraffic.getDate());
        assertEquals(1, dateTraffic.getTraffic());
    }
    @Test
    public void testGetMarkers_Success() throws IOException, CustomException {
        List<Visit> mockVisits = List.of(new Visit());
        when(visitRepository.findByStartTimeBetween(anyLong(), anyLong())).thenReturn(mockVisits);

        List<PublicSpace> mockPublicSpaces = List.of(new PublicSpace());
        when(publicSpaceRepository.findAllByCityAndDeletedIsFalseAndType(anyString(), anyString())).thenReturn(mockPublicSpaces);

        ResponseEntity<String> mockResponse = ResponseEntity.ok("{\"key\":\"value\"}");
        CrowdsensingService crowdsensingService1 = spy(crowdsensingService);
        doReturn(mockResponse).when(crowdsensingService1).filterElements(mockVisits, mockPublicSpaces);

        LocalDate initialDate = LocalDate.now().minusDays(7);
        LocalDate finalDate = LocalDate.now();

        ResponseEntity<String> response = crowdsensingService1.getMarkers(initialDate, finalDate, "park", "city");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"key\":\"value\"}", response.getBody());
    }
    @Test
    public void testGetMarkers_NoPublicSpacesFound() {
        List<Visit> mockVisits = List.of(new Visit());
        when(visitRepository.findByStartTimeBetween(anyLong(), anyLong())).thenReturn(mockVisits);

        when(publicSpaceRepository.findAllByCityAndDeletedIsFalseAndType(anyString(), anyString())).thenReturn(new ArrayList<>());

        LocalDate initialDate = LocalDate.now().minusDays(7);
        LocalDate finalDate = LocalDate.now();

        CustomException exception = assertThrows(CustomException.class, () -> crowdsensingService.getMarkers(initialDate, finalDate, "park", "city"));

        assertEquals("No public spaces with that specification", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    @Test
    public void testGetMarkers_NoVisitsFound() {
        when(visitRepository.findByStartTimeBetween(anyLong(), anyLong())).thenReturn(new ArrayList<>());

        LocalDate initialDate = LocalDate.now().minusDays(7);
        LocalDate finalDate = LocalDate.now();

        CustomException exception = assertThrows(CustomException.class, () -> crowdsensingService.getMarkers(initialDate, finalDate, "park", "city"));

        assertEquals("No visits in that range of time", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    @Test
    public void testFilterElements_Success() throws JsonProcessingException {
        PublicSpace mockPublicSpace = mock(PublicSpace.class);
        when(mockPublicSpace.getPoints()).thenReturn(Arrays.asList("10.0; 20.0", "30.0; 40.0"));
        when(mockPublicSpace.getName()).thenReturn("park");
        when(mockPublicSpace.getId()).thenReturn(1L);

        List<Visit> mockVisits = new ArrayList<>();

        ArrayList<DateTraffic> mockTraffic = new ArrayList<>();
        mockTraffic.add(new DateTraffic(LocalDate.now()));
        CrowdsensingService crowdsensingService1 = spy(crowdsensingService);
        doReturn(mockTraffic).when(crowdsensingService1).getTrafficByPublicSpace(any(PublicSpace.class), anyList());

        List<PublicSpace> publicSpaces = List.of(mockPublicSpace);
        ResponseEntity<String> response = crowdsensingService1.filterElements(mockVisits, publicSpaces);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("application/json", response.getHeaders().getContentType().toString());

        String json = response.getBody();
        assertNotNull(json);

        assertTrue(json.contains("\"name\":\"park\""));
    }
    @Test
    public void testFilterElements_EmptyPublicSpaces() throws JsonProcessingException {
        List<PublicSpace> mockPublicSpaces = new ArrayList<>();
        List<Visit> mockVisits = new ArrayList<>();

        CrowdsensingService crowdsensingService1 = spy(crowdsensingService);
        ResponseEntity<String> response = crowdsensingService1.filterElements(mockVisits, mockPublicSpaces);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("application/json", response.getHeaders().getContentType().toString());
        assertEquals("[]", response.getBody());
    }
}