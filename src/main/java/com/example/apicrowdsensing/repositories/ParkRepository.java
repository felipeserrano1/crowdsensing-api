package com.example.apicrowdsensing.repositories;

import com.example.apicrowdsensing.models.Park;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkRepository extends JpaRepository<Park, Integer> {
    List<Park> findAllByCity(String city);
    List<Park> findAllByCityAndDeletedIsFalse(String city);

    Park findByName(String name);
}
