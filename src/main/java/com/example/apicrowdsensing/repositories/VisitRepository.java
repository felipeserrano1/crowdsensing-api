package com.example.apicrowdsensing.repositories;

import com.example.apicrowdsensing.models.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Integer> {
    List<Visit> findByStartTimeBetween(long start, long end);
}

