package com.example.dat.doctor.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.dat.doctor.dto.DoctorDTO;
import com.example.dat.doctor.entity.Doctor;
import com.example.dat.doctor.repo.DoctorRepo;
import com.example.dat.enums.Specialization;
import com.example.dat.exceptions.NotFoundException;
import com.example.dat.res.Response;
import com.example.dat.users.entity.User;
import com.example.dat.users.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorServiceImpl implements DoctorService{


    private final DoctorRepo doctorRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;


    @Override
    public Response<DoctorDTO> getDoctorProfile() {

        User user = userService.getCurrentUser();

        Doctor doctor = doctorRepo.findByUser(user)
                .orElseThrow(() -> new NotFoundException("No se encontró perfil del Doctor."));

        return Response.<DoctorDTO>builder()
                .statusCode(200)
                .message("El registro del Doctor ha sido obtenido correctamente.")
                .data(modelMapper.map(doctor, DoctorDTO.class))
                .build();
    }

    @Override
    public Response<?> updateDoctorProfile(DoctorDTO doctorDTO) {

        User currentUser = userService.getCurrentUser();

        Doctor doctor = doctorRepo.findByUser(currentUser)
                .orElseThrow(() -> new NotFoundException("No se encontró perfil del Doctor."));

        // Basic fields (firstName, lastName)
        if (StringUtils.hasText(doctorDTO.getFirstName())) {
            doctor.setFirstName(doctorDTO.getFirstName());
        }
        if (StringUtils.hasText(doctorDTO.getLastName())) {
            doctor.setLastName(doctorDTO.getLastName());
        }

        Optional.ofNullable(doctorDTO.getSpecialization()).ifPresent(doctor::setSpecialization);

        doctorRepo.save(doctor);
        log.info("Perfil del Doctor actualizado con éxito.");

        return Response.builder()
                .statusCode(200)
                .message("Perfil del Doctor actualizado con éxito.")
                .build();

    }

    @Override
    public Response<List<DoctorDTO>> getAllDoctors() {

        List<Doctor> doctors = doctorRepo.findAll();

        List<DoctorDTO> doctorDTOS = doctors.stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .toList();

        return Response.<List<DoctorDTO>>builder()
                .statusCode(200)
                .message("Todos los registros de los doctores han sido obtenidos correctamente.")
                .data(doctorDTOS)
                .build();

    }

    @Override
    public Response<DoctorDTO> getDoctorById(Long doctorId) {

        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado."));

        return Response.<DoctorDTO>builder()
                .statusCode(200)
                .message("El registro del Doctor ha sido obtenido correctamente.")
                .data(modelMapper.map(doctor, DoctorDTO.class))
                .build();
    }

    @Override
    public Response<List<DoctorDTO>> searchDoctorsBySpecialization(Specialization specialization) {

        List<Doctor> doctors = doctorRepo.findBySpecialization(specialization);

        List<DoctorDTO> doctorDTOs = doctors.stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .toList();


        String message = doctors.isEmpty() ?
                "No se encontraron doctores para la especialización: " + specialization.name() :
                "Doctores obtenidos correctamente para la especialización: " + specialization.name();

        return Response.<List<DoctorDTO>>builder()
                .statusCode(200)
                .message(message)
                .data(doctorDTOs)
                .build();

    }

    @Override
    public Response<List<Specialization>> getAllSpecializationEnums() {

        List<Specialization> specializations = Arrays.asList(Specialization.values());

        return Response.<List<Specialization>>builder()
                .statusCode(200)
                .message("Especializaciones obtenidas correctamente.")
                .data(specializations)
                .build();
    }
}
