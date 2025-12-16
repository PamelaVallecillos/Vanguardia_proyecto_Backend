package com.example.dat.dependent.repo;

import com.example.dat.dependent.entity.Dependent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependentRepo extends JpaRepository<Dependent, Long> {

    Optional<Dependent> findByExpedienteNumber(String expedienteNumber);

    boolean existsByExpedienteNumber(String expedienteNumber);

    List<Dependent> findByPatientId(Long patientId);

    List<Dependent> findByExpedienteNumberIsNull();
}
