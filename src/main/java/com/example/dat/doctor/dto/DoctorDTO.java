package com.example.dat.doctor.dto;

//tengo 2 imports de demas import com.example.dat.enums.Specialization; Y import com.example.dat.users.dto.UserDTO;
import com.example.dat.enums.Specialization;
import com.example.dat.users.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorDTO {


    private Long id;

    private String firstName;
    private String lastName;
    private String gender;
    private String phone;

    private Specialization specialization;

    private String additionalSpecializations;

    private String licenseNumber;

    private String restriccionGenero;
    private Integer edadMinima;
    private Integer edadMaxima;
    private Integer tiempoDeConsulta;

    private UserDTO user;


}
