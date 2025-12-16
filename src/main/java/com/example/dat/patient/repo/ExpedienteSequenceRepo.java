package com.example.dat.patient.repo;

import com.example.dat.patient.entity.ExpedienteSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpedienteSequenceRepo extends JpaRepository<ExpedienteSequence, Long> {
}
