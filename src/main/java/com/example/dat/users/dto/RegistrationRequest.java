package com.example.dat.users.dto;
//tengo 3 imports de mas import com.example.dat.enums.Specialization; | import jakarta.validation.constraints.Email; | import jakarta.validation.constraints.NotBlank;
import java.util.List;

import com.example.dat.enums.Specialization;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrationRequest {


    @NotBlank(message = "Nombre es requerido")
    private String name;

    private Specialization specialization; //if users is a doctor specify his specialization

    private String licenseNumber; ////if users is a doctor licence number of the doctor

    @NotBlank(message = "Email es requerido")
    @Email
    private String email;

    private List<String> roles;

    @NotBlank(message = "Contraseña es requerida")
    private String password;
}
