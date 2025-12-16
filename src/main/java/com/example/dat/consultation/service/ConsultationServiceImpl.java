package com.example.dat.consultation.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.dat.appointment.entity.Appointment;
import com.example.dat.appointment.repo.AppointmentRepo;
import com.example.dat.consultation.dto.ConsultationDTO;
import com.example.dat.consultation.dto.ConsultationDocumentDTO;
import com.example.dat.consultation.entity.Consultation;
import com.example.dat.consultation.entity.ConsultationDocument;
import com.example.dat.consultation.repo.ConsultationDocumentRepo;
import com.example.dat.consultation.repo.ConsultationRepo;
import com.example.dat.doctor.entity.Doctor;
import com.example.dat.doctor.repo.DoctorRepo;
import com.example.dat.enums.AppointmentStatus;
import com.example.dat.exceptions.BadRequestException;
import com.example.dat.exceptions.NotFoundException;
import com.example.dat.patient.entity.Patient;
import com.example.dat.patient.repo.PatientRepo;
import com.example.dat.res.Response;
import com.example.dat.users.entity.User;
import com.example.dat.users.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationServiceImpl implements ConsultationService{


    private final ConsultationRepo consultationRepo;
    private final AppointmentRepo appointmentRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final PatientRepo patientRepo;
    private final ConsultationDocumentRepo consultationDocumentRepo;
    private final DoctorRepo doctorRepo;

    @Value("${app.upload.dir:uploads/consultation-documents}")
    private String uploadDir;

    @Override
    public Response<ConsultationDTO> createConsultation(ConsultationDTO consultationDTO) {

        User user = userService.getCurrentUser();
        Long appointmentId = consultationDTO.getAppointmentId();

        Appointment appointment = appointmentRepo.findById(appointmentId)
            .orElseThrow(() -> new NotFoundException("Cita no encontrada."));

        // Security Check 1: Must be the doctor linked to the appointment
        if (!appointment.getDoctor().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No estás autorizado para crear notas para esta consulta.");
        }
        // Complete the appointment
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepo.save(appointment);

        // Check 3: Ensure a consultation doesn't already exist for this appointment
        if (consultationRepo.findByAppointmentId(appointmentId).isPresent()) {
            throw new BadRequestException("Ya existen notas de consulta para esta cita.");
        }

        Consultation consultation = Consultation.builder()
                .consultationDate(LocalDateTime.now())
                .subjectiveNotes(consultationDTO.getSubjectiveNotes())
                .objectiveFindings(consultationDTO.getObjectiveFindings())
                .assessment(consultationDTO.getAssessment())
                .plan(consultationDTO.getPlan())
                .appointment(appointment)
                .documents(new ArrayList<>())
                .build();

        Consultation savedConsultation = consultationRepo.save(consultation);

        return Response.<ConsultationDTO>builder()
                .statusCode(200)
                .message("Notas de consulta guardadas correctamente.")
                .data(ConsultationDTO.builder()
                        .id(savedConsultation.getId())
                        .build())
                .build();

    }
    
    @Override
    public Response<?> uploadConsultationDocuments(Long consultationId, List<MultipartFile> files) {
        
        Consultation consultation = consultationRepo.findById(consultationId)
            .orElseThrow(() -> new NotFoundException("Consulta no encontrada"));
        
        User user = userService.getCurrentUser();
        
        // Security: Only the doctor who created the consultation can upload documents
        if (!consultation.getAppointment().getDoctor().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No estás autorizado para subir documentos para esta consulta.");
        }
        
        List<ConsultationDocumentDTO> uploadedDocs = new ArrayList<>();
        
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                
                // Validate file size (10MB max)
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new BadRequestException("El archivo " + file.getOriginalFilename() + " excede el tamaño máximo de 10MB");
                }
                
                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : "";
                String uniqueFilename = UUID.randomUUID().toString() + extension;
                
                // Save file
                Path filePath = uploadPath.resolve(uniqueFilename);
                Files.copy(file.getInputStream(), filePath);
                
                // Create document record
                ConsultationDocument document = ConsultationDocument.builder()
                        .fileName(originalFilename)
                        .filePath("/consultation-documents/" + uniqueFilename)
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .uploadedAt(LocalDateTime.now())
                        .consultation(consultation)
                        .build();
                
                ConsultationDocument savedDoc = consultationDocumentRepo.save(document);
                
                uploadedDocs.add(modelMapper.map(savedDoc, ConsultationDocumentDTO.class));
                
                log.info("Documento subido: {} para consulta ID: {}", originalFilename, consultationId);
            }
            
                return Response.builder()
                    .statusCode(200)
                    .message("Documentos subidos correctamente")
                    .data(uploadedDocs)
                    .build();
                    
        } catch (IOException e) {
            log.error("Error al subir documentos: ", e);
            throw new BadRequestException("Error al subir documentos: " + e.getMessage());
        }
    }

    @Override
    public Response<ConsultationDTO> getConsultationByAppointmentId(Long appointmentId) {

        User user = userService.getCurrentUser();

        Consultation consultation = consultationRepo.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new NotFoundException("No se encontraron notas de consulta para la cita con ID: " + appointmentId));

        ConsultationDTO dto = convertConsultationToDTO(consultation);

        return Response.<ConsultationDTO>builder()
                .statusCode(200)
                .message("Notas de consulta obtenidas correctamente.")
                .data(dto)
                .build();

    }

    @Override
    public Response<List<ConsultationDTO>> getConsultationHistoryForPatient(Long patientId) {

        User user = userService.getCurrentUser();

        // 1. If patientId is null, retrieve the ID of the current authenticated patient.
        if (patientId == null) {
                Patient currentPatient = patientRepo.findByUser(user)
                    .orElseThrow(() -> new BadRequestException("Perfil de paciente no encontrado para el usuario actual"));
            patientId = currentPatient.getId();
        }

        // Find the patient to ensure they exist (or to perform future security checks)
        patientRepo.findById(patientId)
            .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));


        // Use the repository method to fetch all consultations linked via appointments
        List<Consultation> history = consultationRepo.findByAppointmentPatientIdOrderByConsultationDateDesc(patientId);

        if (history.isEmpty()) {
            return Response.<List<ConsultationDTO>>builder()
                    .statusCode(200)
                    .message("No se encontraron consultas para este paciente.")
                    .data(List.of())
                    .build();
        }

        List<ConsultationDTO> historyDTOs = history.stream()
                .map(this::convertConsultationToDTO)
                .toList();

        return Response.<List<ConsultationDTO>>builder()
                .statusCode(200)
                .message("Historial de consultas obtenido correctamente.")
                .data(historyDTOs)
                .build();

    }
    
    @Override
    public Response<List<ConsultationDTO>> getMyConsultations() {
        
        User user = userService.getCurrentUser();
        
        // Get current doctor
        Doctor doctor = doctorRepo.findByUser(user)
            .orElseThrow(() -> new BadRequestException("Perfil de doctor no encontrado para el usuario actual"));
        
        // Get all consultations for this doctor
        List<Consultation> consultations = consultationRepo.findByAppointmentDoctorIdOrderByConsultationDateDesc(doctor.getId());
        
        if (consultations.isEmpty()) {
                return Response.<List<ConsultationDTO>>builder()
                    .statusCode(200)
                    .message("No se encontraron consultas.")
                    .data(List.of())
                    .build();
        }
        
        List<ConsultationDTO> consultationDTOs = consultations.stream()
                .map(this::convertConsultationToDTO)
                .collect(Collectors.toList());
        
        return Response.<List<ConsultationDTO>>builder()
                .statusCode(200)
            .message("Consultas obtenidas correctamente.")
                .data(consultationDTOs)
                .build();
    }
    
    private ConsultationDTO convertConsultationToDTO(Consultation consultation) {
        ConsultationDTO dto = modelMapper.map(consultation, ConsultationDTO.class);
        
        // Include documents if they exist
        if (consultation.getDocuments() != null && !consultation.getDocuments().isEmpty()) {
            List<ConsultationDocumentDTO> documentDTOs = consultation.getDocuments().stream()
                    .map(doc -> modelMapper.map(doc, ConsultationDocumentDTO.class))
                    .collect(Collectors.toList());
            dto.setDocuments(documentDTOs);
        }
        
        return dto;
    }
}










