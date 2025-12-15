package com.example.dat.doctor.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dat.doctor.dto.DoctorDTO;
import com.example.dat.doctor.service.DoctorService;
import com.example.dat.enums.Specialization;
import com.example.dat.res.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/doctors")
@Slf4j
public class DoctorController {

    private final DoctorService doctorService;


    @GetMapping("/me")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<Response<DoctorDTO>> getDoctorProfile() {
        return ResponseEntity.ok(doctorService.getDoctorProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<Response<?>> updateDoctorProfile(@RequestBody DoctorDTO doctorDTO) {
        log.info("=== CONTROLLER: Request recibido ===");
        log.info("Todos los campos del DTO: {}", doctorDTO);
        return ResponseEntity.ok(doctorService.updateDoctorProfile(doctorDTO));
    }


    @GetMapping
    public ResponseEntity<Response<List<DoctorDTO>>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<Response<DoctorDTO>> getDoctorById(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getDoctorById(doctorId));
    }

    @GetMapping("/filter")
    public ResponseEntity<Response<List<DoctorDTO>>> searchDoctorsBySpecialization(
            @RequestParam(required = true) Specialization specialization
    ) {
        return ResponseEntity.ok(doctorService.searchDoctorsBySpecialization(specialization));
    }

    @GetMapping("/specializations")
    public ResponseEntity<Response<List<Specialization>>> getAllSpecializationEnums() {
        return ResponseEntity.ok(doctorService.getAllSpecializationEnums());
    }


}
