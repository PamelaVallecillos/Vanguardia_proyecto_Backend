package com.example.dat.dependent.controller;

import com.example.dat.dependent.dto.DependentDTO;
import com.example.dat.dependent.service.DependentService;
import com.example.dat.res.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dependents")
@RequiredArgsConstructor
public class DependentController {

    private final DependentService dependentService;

    @PostMapping("/register/{patientId}")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<Response<DependentDTO>> registerDependent(
            @PathVariable Long patientId,
            @RequestBody DependentDTO dependentDTO) {
        
        Response<DependentDTO> response = dependentService.registerDependent(patientId, dependentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<Response<List<DependentDTO>>> getDependentsByPatient(@PathVariable Long patientId) {
        Response<List<DependentDTO>> response = dependentService.getDependentsByPatient(patientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{dependentId}")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR')")
    public ResponseEntity<Response<DependentDTO>> getDependentById(@PathVariable Long dependentId) {
        Response<DependentDTO> response = dependentService.getDependentById(dependentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-dependents")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<Response<List<DependentDTO>>> getMyDependents() {
        Response<List<DependentDTO>> response = dependentService.getMyDependents();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{dependentId}/upload-photo")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<Response<DependentDTO>> uploadProfilePhoto(
            @PathVariable Long dependentId,
            @RequestParam("photo") MultipartFile photo) {
        
        Response<DependentDTO> response = dependentService.uploadProfilePhoto(dependentId, photo);
        return ResponseEntity.ok(response);
    }
}
