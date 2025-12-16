package com.example.dat.appointment.controller;


import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dat.appointment.dto.AppointmentDTO;
import com.example.dat.appointment.service.AppointmentService;
import com.example.dat.res.Response;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<Response<AppointmentDTO>> bookAppointment(@RequestBody Map<String, Object> body) {

        // Build AppointmentDTO from raw body and normalize startTime parsing offsets
        AppointmentDTO dto = new AppointmentDTO();

        if (body.containsKey("doctorId") && body.get("doctorId") != null) {
            dto.setDoctorId(Long.valueOf(body.get("doctorId").toString()));
        }
        if (body.containsKey("dependentId") && body.get("dependentId") != null) {
            dto.setDependentId(Long.valueOf(body.get("dependentId").toString()));
        }
        if (body.containsKey("purposeOfConsultation") && body.get("purposeOfConsultation") != null) {
            dto.setPurposeOfConsultation(body.get("purposeOfConsultation").toString());
        }
        if (body.containsKey("initialSymptoms") && body.get("initialSymptoms") != null) {
            dto.setInitialSymptoms(body.get("initialSymptoms").toString());
        }

        // Parse startTime robustly
        if (body.containsKey("startTime") && body.get("startTime") != null) {
            String startStr = body.get("startTime").toString();
            try {
                if (startStr.endsWith("Z") || startStr.contains("+") || (startStr.lastIndexOf('-') > 9)) {
                    java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(startStr);
                    dto.setStartTime(odt.toLocalDateTime());
                } else {
                    dto.setStartTime(java.time.LocalDateTime.parse(startStr, java.time.format.DateTimeFormatter.ISO_DATE_TIME));
                }
            } catch (Exception e) {
                // fallback: try generic parse
                dto.setStartTime(java.time.LocalDateTime.parse(startStr));
            }
        }

        return ResponseEntity.ok(appointmentService.bookAppointment(dto));
    }

    // Debug endpoint to log raw request body for troubleshooting timezone issues
    @PostMapping("/debug")
    public ResponseEntity<String> debugRawBooking(@RequestBody String raw) {
        log.info("[DEBUG-RAW-BOOKING] Request body: {}", raw);
        return ResponseEntity.ok("logged");
    }

    @GetMapping
    public  ResponseEntity<Response<List<AppointmentDTO>>> getMyAppointments(){
        return ResponseEntity.ok(appointmentService.getMyAppointments());
    }

    @PutMapping("/cancel/{appointmentId}")
    public  ResponseEntity<Response<AppointmentDTO>> cancelAppointment(@PathVariable Long appointmentId){
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId));
    }

    @PutMapping("/complete/{appointmentId}")
    @PreAuthorize(("hasAuthority('DOCTOR')"))
    public  ResponseEntity<Response<?>> completeAppointment(@PathVariable Long appointmentId){
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId));
    }

}











