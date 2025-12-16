package com.example.dat.dependent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.dat.dependent.dto.DependentDTO;
import com.example.dat.dependent.entity.Dependent;
import com.example.dat.dependent.repo.DependentRepo;
import com.example.dat.exceptions.BadRequestException;
import com.example.dat.exceptions.NotFoundException;
import com.example.dat.notification.service.NotificationService;
import com.example.dat.patient.entity.ExpedienteSequence;
import com.example.dat.patient.entity.Patient;
import com.example.dat.patient.repo.ExpedienteSequenceRepo;
import com.example.dat.patient.repo.PatientRepo;
import com.example.dat.res.Response;
import com.example.dat.users.entity.User;
import com.example.dat.users.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependentServiceImpl implements DependentService {

    private final DependentRepo dependentRepo;
    private final PatientRepo patientRepo;
    private final ExpedienteSequenceRepo expedienteSequenceRepo;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Value("${app.upload.dir:uploads}")
    private String baseUploadDir;

    @Override
    @Transactional
    public Response<DependentDTO> registerDependent(Long patientId, DependentDTO dependentDTO) {
        
        // Verify patient exists and belongs to current user
        Patient patient = patientRepo.findById(patientId)
            .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));
        
        User currentUser = userService.getCurrentUser();
        if (!patient.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Solo puedes registrar dependientes para tu propio perfil de paciente");
        }

        // Generate expediente number
        String expedienteNumber = generateExpedienteNumber();

        // Map DTO to entity
        Dependent dependent = modelMapper.map(dependentDTO, Dependent.class);
        dependent.setExpedienteNumber(expedienteNumber);
        dependent.setPatient(patient);

        // Save dependent
        Dependent savedDependent = dependentRepo.save(dependent);

        log.info("Dependiente registrado: {} {} con expediente: {}", 
                savedDependent.getFirstName(), savedDependent.getLastName(), expedienteNumber);

        // Send notification email
        String dependentFullName = savedDependent.getFirstName() + " " + savedDependent.getLastName();
        notificationService.sendExpedienteNotification(
                currentUser.getEmail(),
                currentUser.getName(),
                expedienteNumber,
                dependentFullName
        );

        DependentDTO responseDTO = modelMapper.map(savedDependent, DependentDTO.class);

        return Response.<DependentDTO>builder()
                .statusCode(201)
                .message("Dependiente registrado exitosamente")
                .data(responseDTO)
                .build();
    }

    @Override
    public Response<List<DependentDTO>> getDependentsByPatient(Long patientId) {
        
        // Verify patient exists
        Patient patient = patientRepo.findById(patientId)
            .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));

        User currentUser = userService.getCurrentUser();
        if (!patient.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Solo puedes ver tus propios dependientes");
        }

        List<Dependent> dependents = dependentRepo.findByPatientId(patientId);
        
        List<DependentDTO> dependentDTOs = dependents.stream()
                .map(dep -> modelMapper.map(dep, DependentDTO.class))
                .collect(Collectors.toList());

        return Response.<List<DependentDTO>>builder()
                .statusCode(200)
                .message("Dependientes obtenidos correctamente")
                .data(dependentDTOs)
                .build();
    }

    @Override
    public Response<DependentDTO> getDependentById(Long dependentId) {
        
        Dependent dependent = dependentRepo.findById(dependentId)
            .orElseThrow(() -> new NotFoundException("Dependiente no encontrado"));

        User currentUser = userService.getCurrentUser();
        if (!dependent.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Solo puedes ver tus propios dependientes");
        }

        DependentDTO dependentDTO = modelMapper.map(dependent, DependentDTO.class);

        return Response.<DependentDTO>builder()
                .statusCode(200)
                .message("Dependiente obtenido correctamente")
                .data(dependentDTO)
                .build();
    }

    @Override
    @Transactional
    public Response<DependentDTO> uploadProfilePhoto(Long dependentId, MultipartFile photo) {
        
        Dependent dependent = dependentRepo.findById(dependentId)
            .orElseThrow(() -> new NotFoundException("Dependiente no encontrado"));

        User currentUser = userService.getCurrentUser();
        if (!dependent.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Solo puedes subir fotos para tus propios dependientes");
        }

        try {
            // Create upload directory
            String uploadDir = baseUploadDir + "/dependents";
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Validate file
            if (photo.isEmpty()) {
                throw new BadRequestException("El archivo de la foto está vacío");
            }

            // Validate file size (5MB max for photos)
            if (photo.getSize() > 5 * 1024 * 1024) {
                throw new BadRequestException("La foto excede el tamaño máximo de 5MB");
            }

            // Validate file type
            String contentType = photo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BadRequestException("El archivo debe ser una imagen (JPG, PNG, GIF)");
            }

            // Generate unique filename
            String originalFilename = photo.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Delete old photo if exists
            if (dependent.getProfilePhoto() != null) {
                try {
                    String oldPhotoPath = dependent.getProfilePhoto().replace("/dependents/", "");
                    Path oldFile = uploadPath.resolve(oldPhotoPath);
                    Files.deleteIfExists(oldFile);
                } catch (IOException e) {
                    log.warn("Could not delete old photo: {}", e.getMessage());
                }
            }

            // Save new photo
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(photo.getInputStream(), filePath);

            // Update dependent record
            dependent.setProfilePhoto("/dependents/" + uniqueFilename);
            Dependent savedDependent = dependentRepo.save(dependent);

            log.info("Foto de perfil actualizada para dependiente ID: {}", dependentId);

            DependentDTO responseDTO = modelMapper.map(savedDependent, DependentDTO.class);

                return Response.<DependentDTO>builder()
                    .statusCode(200)
                    .message("Foto de perfil subida correctamente")
                    .data(responseDTO)
                    .build();

        } catch (IOException e) {
            log.error("Error al subir la foto: ", e);
            throw new BadRequestException("Error al subir la foto: " + e.getMessage());
        }
    }

    @Override
    public Response<List<DependentDTO>> getMyDependents() {
        
        User currentUser = userService.getCurrentUser();
        
        // Get all patients for this user
        List<Patient> patients = patientRepo.findByUserId(currentUser.getId());
        
        // Get all dependents for all these patients
        List<Dependent> allDependents = patients.stream()
                .flatMap(patient -> dependentRepo.findByPatientId(patient.getId()).stream())
                .collect(Collectors.toList());

        List<DependentDTO> dependentDTOs = allDependents.stream()
                .map(dep -> modelMapper.map(dep, DependentDTO.class))
                .collect(Collectors.toList());

        return Response.<List<DependentDTO>>builder()
                .statusCode(200)
                .message("Dependientes obtenidos correctamente")
                .data(dependentDTOs)
                .build();
    }

    @Transactional
    private String generateExpedienteNumber() {
        
        // Get or create sequence
        ExpedienteSequence sequence = expedienteSequenceRepo.findById(1L)
                .orElseGet(() -> {
                    ExpedienteSequence newSequence = ExpedienteSequence.builder()
                            .id(1L)
                            .lastNumber(0)
                            .build();
                    return expedienteSequenceRepo.save(newSequence);
                });

        // Increment and format
        int nextNumber = sequence.getLastNumber() + 1;
        
        if (nextNumber > 99999) {
            throw new BadRequestException("Se alcanzó el límite de expedientes (99999)");
        }

        String expedienteNumber = String.format("%05d", nextNumber);

        // Update sequence
        sequence.setLastNumber(nextNumber);
        expedienteSequenceRepo.save(sequence);

        return expedienteNumber;
    }
}
