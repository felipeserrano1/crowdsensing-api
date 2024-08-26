package com.example.apicrowdsensing.repositories;

import com.example.apicrowdsensing.models.Park;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkRepository extends JpaRepository<Park, Integer> {
    List<Park> findAllByCity(String city);
    List<Park> findAllByCityAndDeletedIsFalse(String city);

    List<Park> findAllByCityAndDeletedIsFalseAndType(String city, String type);


    Park findByName(String name);
    Park findTopByOrderByIdDesc();

    List<Park> findAllByCityAndCreatedIsTrueAndTypeAndDeletedIsFalse(String city, String type);
    List<Park> findAllByCityAndDeletedIsFalseAndTypeAndCreatedIsFalse(String city, String type);
}
