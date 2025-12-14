package com.example.dat.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email es requerido")
    @Email
    private String email;

    @NotBlank(message = "Contraseña es requerida")
    private String password;
}
