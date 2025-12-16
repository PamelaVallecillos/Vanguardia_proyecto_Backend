package com.example.dat.patient.service;

import com.example.dat.enums.BloodGroup;
import com.example.dat.enums.Genotype;
import com.example.dat.exceptions.BadRequestException;
import com.example.dat.exceptions.NotFoundException;
import com.example.dat.notification.service.NotificationService;
import com.example.dat.patient.dto.PatientDTO;
import com.example.dat.patient.entity.ExpedienteSequence;
import com.example.dat.patient.entity.Patient;
import com.example.dat.patient.repo.ExpedienteSequenceRepo;
import com.example.dat.patient.repo.PatientRepo;
import com.example.dat.res.Response;
import com.example.dat.users.entity.User;
import com.example.dat.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService{

    private final PatientRepo patientRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final ExpedienteSequenceRepo expedienteSequenceRepo;
    private final NotificationService notificationService;


    @Override
    public Response<PatientDTO> getPatientProfile() {

        User user = userService.getCurrentUser();

        Patient patient = patientRepo.findByUser(user)
                .orElseThrow(()-> new NotFoundException("Patient Not Found"));

        return Response.<PatientDTO>builder()
                .statusCode(200)
                .message("Patient profile retrieved successfully.")
                .data(modelMapper.map(patient, PatientDTO.class))
                .build();
    }

    @Override
    public Response<?> updatePatientProfile(PatientDTO patientDTO) {


        User currentUser = userService.getCurrentUser();

        Patient patient = patientRepo.findByUser(currentUser)
                .orElseThrow(() -> new NotFoundException("Patient profile not found."));


        // Basic fields (firstName, lastName,)
        if (StringUtils.hasText(patientDTO.getFirstName())) {
            patient.setFirstName(patientDTO.getFirstName());
        }
        if (StringUtils.hasText(patientDTO.getLastName())) {
            patient.setLastName(patientDTO.getLastName());
        }
        if (StringUtils.hasText(patientDTO.getPhone())) {
            patient.setPhone(patientDTO.getPhone());
        }

        // Gender
        if (StringUtils.hasText(patientDTO.getGender())) {
            patient.setGender(patientDTO.getGender());
        }

        // LocalDate field
        Optional.ofNullable(patientDTO.getDateOfBirth()).ifPresent(patient::setDateOfBirth);

        // Medical fields (knownAllergies, bloodGroup, genotype)
        if (StringUtils.hasText(patientDTO.getKnownAllergies())) {
            patient.setKnownAllergies(patientDTO.getKnownAllergies());
        }

        // Enum fields (BloodGroup, Genotype)
        Optional.ofNullable(patientDTO.getBloodGroup()).ifPresent(patient::setBloodGroup);
        Optional.ofNullable(patientDTO.getGenotype()).ifPresent(patient::setGenotype);

        patientRepo.save(patient);

        return Response.builder()
                .statusCode(200)
                .message("Patient profile updated successfully.")
                .build();


    }

    @Override
    public Response<PatientDTO> getPatientById(Long patientId) {

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found with ID: " + patientId));

        PatientDTO patientDTO = modelMapper.map(patient, PatientDTO.class);

        return Response.<PatientDTO>builder()
                .statusCode(200)
                .message("Patient retrieved successfully.")
                .data(patientDTO)
                .build();
    }

    @Override
    public Response<List<BloodGroup>> getAllBloodGroupEnums() {

        List<BloodGroup> bloodGroups = Arrays.asList(BloodGroup.values());

        return Response.<List<BloodGroup>>builder()
                .statusCode(200)
                .message("BloodGroups retrieved successfully")
                .data(bloodGroups)
                .build();
    }

    @Override
    public Response<List<Genotype>> getAllGenotypeEnums() {

        List<Genotype> genotypes = Arrays.asList(Genotype.values());

        return Response.<List<Genotype>>builder()
                .statusCode(200)
                .message("Genotypes retrieved successfully")
                .data(genotypes)
                .build();
    }

    @Override
    @Transactional
    public Response<PatientDTO> registerPatientProfile(PatientDTO patientDTO) {

        User currentUser = userService.getCurrentUser();

        // Generar expediente automáticamente
        String expedienteNumber = generateExpedienteNumber();

        Patient patient = Patient.builder()
                .expedienteNumber(expedienteNumber)
                .firstName(patientDTO.getFirstName())
                .lastName(patientDTO.getLastName())
                .dateOfBirth(patientDTO.getDateOfBirth())
                .phone(patientDTO.getPhone())
            .gender(patientDTO.getGender())
                .bloodGroup(patientDTO.getBloodGroup())
                .genotype(patientDTO.getGenotype())
                .knownAllergies(patientDTO.getKnownAllergies())
                .user(currentUser)
                .build();

        Patient savedPatient = patientRepo.save(patient);

        // Enviar notificación por email con el número de expediente
        String patientFullName = savedPatient.getFirstName() + " " + savedPatient.getLastName();
        notificationService.sendExpedienteNotification(
                currentUser.getEmail(),
                currentUser.getName(),
                expedienteNumber,
                patientFullName
        );

        log.info("Paciente registrado: {} {} con expediente: {}", 
            savedPatient.getFirstName(), savedPatient.getLastName(), expedienteNumber);

        PatientDTO responseDTO = modelMapper.map(savedPatient, PatientDTO.class);

        return Response.<PatientDTO>builder()
                .statusCode(201)
                .message("Paciente registrado exitosamente. Expediente: " + expedienteNumber)
                .data(responseDTO)
                .build();
    }

    @Override
    public Response<List<PatientDTO>> getMyPatients() {

        User currentUser = userService.getCurrentUser();

        List<Patient> patients = patientRepo.findByUserId(currentUser.getId());

        List<PatientDTO> patientDTOs = patients.stream()
                .map(patient -> modelMapper.map(patient, PatientDTO.class))
                .toList();

        String message = patients.isEmpty() 
            ? "No hay pacientes registrados para este usuario."
            : "Pacientes obtenidos correctamente.";

        return Response.<List<PatientDTO>>builder()
                .statusCode(200)
                .message(message)
                .data(patientDTOs)
                .build();
    }

    @Transactional
    private String generateExpedienteNumber() {
        
        ExpedienteSequence sequence = expedienteSequenceRepo.findById(1L)
                .orElseGet(() -> {
                    ExpedienteSequence newSequence = ExpedienteSequence.builder()
                            .id(1L)
                            .lastNumber(0)
                            .build();
                    return expedienteSequenceRepo.save(newSequence);
                });

        int nextNumber = sequence.getLastNumber() + 1;

        // Validar que no exceda 99999
        if (nextNumber > 99999) {
            throw new BadRequestException("Se ha alcanzado el límite de expedientes (99999)");
        }

        sequence.setLastNumber(nextNumber);
        expedienteSequenceRepo.save(sequence);

        // Formatear con ceros a la izquierda: 00001, 00002, etc.
        return String.format("%05d", nextNumber);
    }
}
