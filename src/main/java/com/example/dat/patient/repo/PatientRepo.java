package com.example.dat.patient.repo;

import com.example.dat.patient.entity.Patient;
import com.example.dat.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepo extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUser(User user);

    Optional<Patient> findByExpedienteNumber(String expedienteNumber);

    boolean existsByExpedienteNumber(String expedienteNumber);

    List<Patient> findByUserId(Long userId);

    List<Patient> findByExpedienteNumberIsNull();
}
