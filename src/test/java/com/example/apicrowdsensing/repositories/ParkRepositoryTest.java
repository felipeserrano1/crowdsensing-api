package com.example.apicrowdsensing.repositories;

import com.example.apicrowdsensing.models.Park;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;


//@SpringBootTest
@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class ParkRepositoryTest {

    @Autowired
    private ParkRepository parkRepository;

    @BeforeEach
    void setUp() {
        openMocks(this);

    }

    @Test
    public void ParkRepository_SaveAll_ReturnSavedPark() {

        // Crea una instancia de Park

        //Act
        //Park savedPark = parkRepository.save(park);
        Park park = new Park();
        park.setId(1L);
        park.setName("Parque Central");
        park.setCity("Ciudad");
        park.setType("Publico");
        park.setPoints(Arrays.asList("point1", "point2"));

        Park savedPark = parkRepository.save(park);
        //Assert

        assertThat(savedPark).isNotNull();
        assertThat(savedPark.getId()).isEqualTo(1L);

//        Assertions.assertThat(savedPark).isNotNull();
//        Assertions.assertThat(savedPark.getId()).isGreaterThan(0);

    }

}