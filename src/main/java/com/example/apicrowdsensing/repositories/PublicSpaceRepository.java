package com.example.apicrowdsensing.repositories;

import com.example.apicrowdsensing.models.PublicSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicSpaceRepository extends JpaRepository<PublicSpace, Long> {
    PublicSpace findByName(String name);
    PublicSpace findById(long id);
    PublicSpace findByOverpassId(Long overpassId);
    List<PublicSpace> findAllByCityAndDeletedIsFalseAndType(String city, String type);
    List<PublicSpace> findAllByCreatedTrue();
}
