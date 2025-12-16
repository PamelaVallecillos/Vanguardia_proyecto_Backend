package com.example.dat.appointment.service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.example.dat.appointment.dto.AppointmentDTO;
import com.example.dat.appointment.entity.Appointment;
import com.example.dat.appointment.repo.AppointmentRepo;
import com.example.dat.doctor.entity.Doctor;
import com.example.dat.doctor.entity.Schedule;
import com.example.dat.doctor.repo.DoctorRepo;
import com.example.dat.doctor.repo.ScheduleRepo;
import com.example.dat.enums.AppointmentStatus;
import com.example.dat.exceptions.BadRequestException;
import com.example.dat.exceptions.NotFoundException;
import com.example.dat.notification.dto.NotificationDTO;
import com.example.dat.notification.service.NotificationService;
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
public class AppointmentServiceImpl implements AppointmentService {

        private final AppointmentRepo appointmentRepo;
        private final PatientRepo patientRepo;
        private final DoctorRepo doctorRepo;
        private final ScheduleRepo scheduleRepo;
        private final com.example.dat.dependent.repo.DependentRepo dependentRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;


    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy 'at' hh:mm a");


    @Override
    public Response<AppointmentDTO> bookAppointment(AppointmentDTO appointmentDTO) {

        User currentUser = userService.getCurrentUser();

                // DEBUG: log incoming startTime and server timezone to troubleshoot timezone shifts
                log.info("Incoming appointment startTime (raw DTO): {}", appointmentDTO.getStartTime());
                log.info("Server default ZoneId: {}", java.time.ZoneId.systemDefault());
                log.info("Server current Instant: {}", java.time.Instant.now());

        // 1. Get the patient initiating the booking (titular)
        Patient patient = patientRepo.findByUser(currentUser)
                .orElseThrow(() -> new NotFoundException("Patient profile required for booking."));

        // If booking on behalf of a dependent, we'll load it below and use its data for validations
        com.example.dat.dependent.entity.Dependent dependent = null;



        // --- START: VALIDATION LOGIC ---
        // Define the proposed time slot and compute end time from doctor's configured duration
        LocalDateTime startTime = appointmentDTO.getStartTime();

        // 2.a If dependentId provided, load dependent and validate ownership
        if (appointmentDTO.getDependentId() != null) {
            dependent = dependentRepo.findById(appointmentDTO.getDependentId())
                    .orElseThrow(() -> new NotFoundException("Dependiente no encontrado."));

            // Ensure the dependent belongs to the patient's user
            if (!dependent.getPatient().getUser().getId().equals(currentUser.getId())) {
                throw new BadRequestException("You can only book appointments for your own dependents.");
            }
        }

        // 3. Get the target doctor
        Doctor doctor = doctorRepo.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado."));

        int doctorMinutes = (doctor.getTiempoDeConsulta() != null && doctor.getTiempoDeConsulta() > 0) ? doctor.getTiempoDeConsulta() : 60;
        LocalDateTime endTime = startTime.plusMinutes(doctorMinutes);

                // 3.a Determine subject (patient or dependent) data for validations
                String subjectGender = null;
                LocalDate subjectDob = null;
                if (dependent != null) {
                        subjectGender = dependent.getGender();
                        subjectDob = dependent.getDateOfBirth();
                } else {
                        // As titular, use patient data
                        subjectDob = patient.getDateOfBirth();
                        subjectGender = patient.getGender();
                }

                // 3.b Gender restriction check
                if (doctor.getRestriccionGenero() != null && !doctor.getRestriccionGenero().isBlank()
                                && !doctor.getRestriccionGenero().equalsIgnoreCase("TODOS")) {
                        String docRestr = doctor.getRestriccionGenero().trim();
                        if (subjectGender == null || subjectGender.isBlank()) {
                                throw new BadRequestException("El género del paciente no está especificado; no se puede reservar con la restricción del doctor: " + docRestr);
                        }
                        String subj = subjectGender.trim();
                        if (!subj.equalsIgnoreCase(docRestr)) {
                                throw new BadRequestException("El doctor solo acepta pacientes con género: " + docRestr);
                        }
                }

                // 3.c Age restriction check
                if (subjectDob != null) {
                        int age = Period.between(subjectDob, LocalDate.now()).getYears();
                        if (doctor.getEdadMinima() != null && age < doctor.getEdadMinima()) {
                                throw new BadRequestException("El paciente no cumple con la edad mínima requerida por este doctor.");
                        }
                        if (doctor.getEdadMaxima() != null && age > doctor.getEdadMaxima()) {
                                throw new BadRequestException("El paciente excede la edad máxima permitida por este doctor.");
                        }
                } else {
                        // If doctor has age restrictions but subject DOB is missing, block booking
                        if (doctor.getEdadMinima() != null || doctor.getEdadMaxima() != null) {
                                throw new BadRequestException("La fecha de nacimiento es obligatoria debido a las restricciones de edad del doctor.");
                        }
                }

                // 4. Basic validation: booking must be at least 1 hour in advance
                if (startTime.isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new BadRequestException("Las citas deben reservarse con al menos 1 hora de anticipación.");
                }

        //This code snippet logic used to enforce a mandatory one-hour break (or buffer) for the doctor before a new appointment.
                // 5. Check schedule availability for the doctor on the appointment day
                java.time.DayOfWeek dow = startTime.getDayOfWeek();
                List<Schedule> schedules = scheduleRepo.findByDoctorId(doctor.getId());

                boolean withinSchedule = false;
                for (Schedule sch : schedules) {
                        if (!Boolean.TRUE.equals(sch.getIsActive())) continue;
                        if (!sch.getDayOfWeek().equalsIgnoreCase(dow.name())) continue;

                        java.time.LocalTime apptStart = startTime.toLocalTime();
                        java.time.LocalTime apptEnd = endTime.toLocalTime();

                        if ((apptStart.equals(sch.getStartTime()) || apptStart.isAfter(sch.getStartTime()))
                                        && (apptEnd.equals(sch.getEndTime()) || apptEnd.isBefore(sch.getEndTime()))) {

                                // Check lunch intersection
                                if (sch.getLunchStart() != null && sch.getLunchEnd() != null) {
                                        if (!(apptEnd.isBefore(sch.getLunchStart()) || apptStart.isAfter(sch.getLunchEnd()))) {
                                                // intersects lunch, not allowed
                                                continue;
                                        }
                                }

                                withinSchedule = true;
                                break;
                        }
                }

                if (!withinSchedule) {
                        throw new BadRequestException("El doctor no está trabajando en el día/hora solicitados.");
        }

                // 6. Conflict detection with existing appointments (overlap)
                List<Appointment> conflicts = appointmentRepo.findConflictingAppointments(
                                doctor.getId(),
                                startTime,
                                endTime
                );

                if (!conflicts.isEmpty()) {
                        throw new BadRequestException("El doctor no está disponible a la hora solicitada. Por favor, revisa su horario.");
                }


        // 4a. Generate a unique, random string for the room name.
        //    (Your existing code is good for this)
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String uniqueRoomName = "dat-" + uuid.substring(0, 10);


        // 4b. Use the public Jitsi Meet domain with your unique room name
        String meetingLink = "https://meet.jit.si/" + uniqueRoomName;

        log.info("Generated Jitsi meeting link: {}", meetingLink);


        // 5. Build and Save Appointment
        // Build appointment entity, attach dependent if present
        Appointment.AppointmentBuilder builder = Appointment.builder()
                .startTime(startTime)
                .endTime(endTime)
                .meetingLink(meetingLink)
                .initialSymptoms(appointmentDTO.getInitialSymptoms())
                .purposeOfConsultation(appointmentDTO.getPurposeOfConsultation())
                .status(AppointmentStatus.SCHEDULED)
                .doctor(doctor)
                .patient(patient);

        if (dependent != null) {
            builder.dependent(dependent);
        }

        Appointment appointment = builder.build();

        Appointment savedAppointment = appointmentRepo.save(appointment);

        log.info("[BOOK] Saved appointment startTime (entity): {} | endTime: {}", savedAppointment.getStartTime(), savedAppointment.getEndTime());

        sendAppointmentConfirmation(savedAppointment);

        return Response.<AppointmentDTO>builder()
                .statusCode(200)
                .message("Appointment booked successfully.")
                .build();


    }


    @Override
    public Response<List<AppointmentDTO>> getMyAppointments() {

        User user = userService.getCurrentUser();

        Long userId = user.getId();

        List<Appointment> appointments;

        // Check for "DOCTOR" role
        boolean isDoctor = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("DOCTOR"));

        if (isDoctor) {
            // 1. Check for Doctor profile existence (required to throw the correct exception)
            doctorRepo.findByUser(user)
                    .orElseThrow(() -> new NotFoundException("Doctor profile not found."));

            // 2. Efficiently fetch appointments of the Doctor
            appointments = appointmentRepo.findByDoctor_User_IdOrderByIdDesc(userId);

        } else {

            // 1. Check for Patient profile existence
            patientRepo.findByUser(user)
                    .orElseThrow(() -> new NotFoundException("Patient profile not found."));

            // 2. Efficiently fetch appointments using the User ID to navigate Patient relationship
            appointments = appointmentRepo.findByPatient_User_IdOrderByIdDesc(userId);
        }
                // Debug: log start times returned from DB before mapping
                for (Appointment a : appointments) {
                        log.info("[LIST] Appointment id={} startTime(entity)={} | endTime(entity)={}", a.getId(), a.getStartTime(), a.getEndTime());
                }

                // Convert the list of entities to DTOs in a single step
                List<AppointmentDTO> appointmentDTOList = appointments.stream()
                                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                                .toList();

                // Debug: log DTO times after mapping
                for (AppointmentDTO dto : appointmentDTOList) {
                        log.info("[LIST] DTO id={} startTime(dto)={} | endTime(dto)={}", dto.getId(), dto.getStartTime(), dto.getEndTime());
                }

        return Response.<List<AppointmentDTO>>builder()
                .statusCode(200)
                .message("Appointments retrieved successfully.")
                .data(appointmentDTOList)
                .build();

    }

    @Override
    public Response<AppointmentDTO> cancelAppointment(Long appointmentId) {

        User user = userService.getCurrentUser();

        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));


        // Add security check: only the patient or doctor involved can cancel
        boolean isOwner = appointment.getPatient().getUser().getId().equals(user.getId()) ||
                appointment.getDoctor().getUser().getId().equals(user.getId());

        if (!isOwner) {
            throw new BadRequestException("No tienes permiso para cancelar esta cita.");
        }

        // Update status
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment savedAppointment = appointmentRepo.save(appointment);

        // NOTE: Notification should be sent to the other party (patient/doctor)
        sendAppointmentCancellation(savedAppointment, user);

        return Response.<AppointmentDTO>builder()
                .statusCode(200)
                .message("Appointment cancelled successfully.")
                .build();

    }

    @Override
    public Response<?> completeAppointment(Long appointmentId) {

        // Get the current user (must be the Doctor)
        User currentUser = userService.getCurrentUser();

        // 1. Fetch the appointment
        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with ID: " + appointmentId));

        // Security Check 1: Ensure the current user is the Doctor assigned to this appointment
        if (!appointment.getDoctor().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Solo el doctor asignado puede marcar esta cita como completada.");
        }

        // 2. Update status and end time
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setEndTime(LocalDateTime.now());

        Appointment updatedAppointment = appointmentRepo.save(appointment);

        modelMapper.map(updatedAppointment, AppointmentDTO.class);

        return Response.builder()
                .statusCode(200)
                .message("Appointment successfully marked as completed. You may now proceed to create the consultation notes.")
                .build();

    }

    private void sendAppointmentCancellation(Appointment appointment, User cancelingUser){

        User patientUser = appointment.getPatient().getUser();
        User doctorUser = appointment.getDoctor().getUser();

        // Safety check to ensure the cancellingUser is involved
        boolean isOwner = patientUser.getId().equals(cancelingUser.getId()) || doctorUser.getId().equals(cancelingUser.getId());
        if (!isOwner) {
            log.error("Cancellation initiated by user not associated with appointment. User ID: {}", cancelingUser.getId());
            return;
        }

        String formattedTime = appointment.getStartTime().format(FORMATTER);
        String cancellingPartyName = cancelingUser.getName();


        // --- Common Variables for the Template ---
        Map<String, Object> baseVars = new HashMap<>();
        baseVars.put("cancellingPartyName", cancellingPartyName);
        baseVars.put("appointmentTime", formattedTime);
        baseVars.put("doctorName", appointment.getDoctor().getLastName());
        baseVars.put("patientFullName", patientUser.getName());

        // --- 1. Dispatch Email to Doctor ---
        Map<String, Object> doctorVars = new HashMap<>(baseVars);
        doctorVars.put("recipientName", doctorUser.getName());

        NotificationDTO doctorNotification = NotificationDTO.builder()
                .recipient(doctorUser.getEmail())
                .subject("DAT Health: Appointment Cancellation")
                .templateName("appointment-cancellation")
                .templateVariables(doctorVars)
                .build();

        notificationService.sendEmail(doctorNotification, doctorUser);
        log.info("Dispatched cancellation email to Doctor: {}", doctorUser.getEmail());


        // --- 2. Dispatch Email to Patient ---
        Map<String, Object> patientVars = new HashMap<>(baseVars);
        patientVars.put("recipientName", patientUser.getName());

        NotificationDTO patientNotification = NotificationDTO.builder()
                .recipient(patientUser.getEmail())
                .subject("DAT Health: Appointment CANCELED (ID: " + appointment.getId() + ")")
                .templateName("appointment-cancellation")
                .templateVariables(patientVars)
                .build();

        notificationService.sendEmail(patientNotification, patientUser);
        log.info("Dispatched cancellation email to Patient: {}", patientUser.getEmail());

    }


    private void sendAppointmentConfirmation(Appointment appointment) {

        // --- 1. Prepare Patient Notification ---
        User patientUser = appointment.getPatient().getUser();
        String formattedTime = appointment.getStartTime().format(FORMATTER);


        Map<String, Object> patientVars = new HashMap<>();
        patientVars.put("patientName", patientUser.getName());
        patientVars.put("doctorName", appointment.getDoctor().getUser().getName());
        patientVars.put("appointmentTime", formattedTime);
        patientVars.put("isVirtual", true);
        patientVars.put("meetingLink", appointment.getMeetingLink());
        patientVars.put("purposeOfConsultation", appointment.getPurposeOfConsultation());

        NotificationDTO patientNotification = NotificationDTO.builder()
                .recipient(patientUser.getEmail())
                .subject("DAT Health: Your Appointment is Confirmed")
                .templateName("patient-appointment")
                .templateVariables(patientVars)
                .build();


        // Dispatch patient email using the low-level service
        notificationService.sendEmail(patientNotification, patientUser);
        log.info("Dispatched confirmation email for patient: {}", patientUser.getEmail());


        // --- 2. Prepare Doctor Notification ---
        User doctorUser = appointment.getDoctor().getUser();

        Map<String, Object> doctorVars = new HashMap<>();
        doctorVars.put("doctorName", doctorUser.getName());
        doctorVars.put("patientFullName", patientUser.getName());
        doctorVars.put("appointmentTime", formattedTime);
        doctorVars.put("isVirtual", true);
        doctorVars.put("meetingLink", appointment.getMeetingLink());
        doctorVars.put("initialSymptoms", appointment.getInitialSymptoms());
        doctorVars.put("purposeOfConsultation", appointment.getPurposeOfConsultation());

        NotificationDTO doctorNotification = NotificationDTO.builder()
                .recipient(doctorUser.getEmail())
                .subject("DAT Health: New Appointment Booked")
                .templateName("doctor-appointment")
                .templateVariables(doctorVars)
                .build();


        // Dispatch doctor email using the low-level service
        notificationService.sendEmail(doctorNotification, doctorUser);
        log.info("Dispatched new appointment email for doctor: {}", doctorUser.getEmail());
    }
}


















