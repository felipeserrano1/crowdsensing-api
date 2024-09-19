package com.example.apicrowdsensing.overpass;

import com.example.apicrowdsensing.models.PublicSpace;
import com.example.apicrowdsensing.repositories.PublicSpaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OverpassAPITest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PublicSpaceRepository publicSpaceRepository;

    @Test
    public void testGetOverpassQuerySuccesful() throws Exception {
        mockMvc.perform(get("/query/overpass")
                        .param("city", "Ayacucho")
                        .param("tag", "park")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        List<PublicSpace> publicSpaces = publicSpaceRepository.findAll();

        assertThat(publicSpaces).isNotEmpty();
        assertThat(publicSpaces).anyMatch(p -> p.getName().equals("Plaza"));
    }
}
