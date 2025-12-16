package com.example.dat.config;

import com.example.dat.dependent.entity.Dependent;
import com.example.dat.dependent.repo.DependentRepo;
import com.example.dat.patient.entity.ExpedienteSequence;
import com.example.dat.patient.entity.Patient;
import com.example.dat.patient.repo.ExpedienteSequenceRepo;
import com.example.dat.patient.repo.PatientRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Ejecutar después de DataInitializer
public class ExpedienteMigration implements CommandLineRunner {

    private final PatientRepo patientRepo;
    private final DependentRepo dependentRepo;
    private final ExpedienteSequenceRepo expedienteSequenceRepo;

    @Value("${app.migration.assign-expedientes:true}")
    private boolean assignExpedientes;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        
        if (!assignExpedientes) {
            log.info("Migración de expedientes deshabilitada");
            return;
        }

        log.info("=== Iniciando migración de expedientes ===");

        // Buscar TODOS los pacientes y filtrar los que no tienen expediente válido
        List<Patient> allPatients = patientRepo.findAll();
        List<Patient> patientsWithoutExpediente = allPatients.stream()
                .filter(p -> p.getExpedienteNumber() == null || p.getExpedienteNumber().trim().isEmpty())
                .toList();

        // Buscar TODOS los dependientes y filtrar los que no tienen expediente válido
        List<Dependent> allDependents = dependentRepo.findAll();
        List<Dependent> dependentsWithoutExpediente = allDependents.stream()
                .filter(d -> d.getExpedienteNumber() == null || d.getExpedienteNumber().trim().isEmpty())
                .toList();

        int totalWithoutExpediente = patientsWithoutExpediente.size() + dependentsWithoutExpediente.size();

        if (totalWithoutExpediente == 0) {
            log.info("Todos los pacientes y dependientes ya tienen expediente asignado");
            return;
        }

        log.info("Encontrados {} pacientes sin expediente", patientsWithoutExpediente.size());
        log.info("Encontrados {} dependientes sin expediente", dependentsWithoutExpediente.size());

        // Inicializar secuencia si no existe
        ExpedienteSequence sequence = expedienteSequenceRepo.findById(1L)
                .orElseGet(() -> {
                    ExpedienteSequence newSequence = ExpedienteSequence.builder()
                            .id(1L)
                            .lastNumber(0)
                            .build();
                    return expedienteSequenceRepo.save(newSequence);
                });

        // Asignar expediente a cada paciente
        for (Patient patient : patientsWithoutExpediente) {
            int nextNumber = sequence.getLastNumber() + 1;
            String expedienteNumber = String.format("%05d", nextNumber);

            patient.setExpedienteNumber(expedienteNumber);
            patientRepo.save(patient);

            sequence.setLastNumber(nextNumber);
            expedienteSequenceRepo.save(sequence);

            log.info("Expediente {} asignado a paciente ID: {} - {} {}", 
                    expedienteNumber, patient.getId(), patient.getFirstName(), patient.getLastName());
        }

        // Asignar expediente a cada dependiente
        for (Dependent dependent : dependentsWithoutExpediente) {
            int nextNumber = sequence.getLastNumber() + 1;
            String expedienteNumber = String.format("%05d", nextNumber);

            dependent.setExpedienteNumber(expedienteNumber);
            dependentRepo.save(dependent);

            sequence.setLastNumber(nextNumber);
            expedienteSequenceRepo.save(sequence);

            log.info("Expediente {} asignado a dependiente ID: {} - {} {}", 
                    expedienteNumber, dependent.getId(), dependent.getFirstName(), dependent.getLastName());
        }

        log.info("=== Migración de expedientes completada ===");
        log.info("Total de expedientes asignados: {}", totalWithoutExpediente);
    }
}
