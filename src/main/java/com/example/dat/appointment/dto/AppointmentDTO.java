package com.example.dat.appointment.dto;

//no tiene los imports 1,2,3,6,7
import com.example.dat.doctor.dto.DoctorDTO;
import com.example.dat.enums.AppointmentStatus;
import com.example.dat.patient.dto.PatientDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointmentDTO {

    private Long id;

    @NotNull(message = "Doctor ID is required for booking.")
    private Long doctorId;

    // Optional: book on behalf of a dependent
    private Long dependentId;

    private String purposeOfConsultation;

    private String initialSymptoms;

    @NotNull(message = "Start time is required for the appointment.")
    @Future(message = "Appointment must be scheduled for a future date and time.")
    private LocalDateTime startTime;

    private LocalDateTime endTime;
    private String meetingLink; // Unique link for the video/tele consultation

    private AppointmentStatus status;

    private DoctorDTO doctor;
    private PatientDTO patient;
}
