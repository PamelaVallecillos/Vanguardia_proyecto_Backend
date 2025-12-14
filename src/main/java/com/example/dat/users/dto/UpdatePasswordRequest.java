package com.example.dat.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "Antigua Contraseña es requerida")
    private String oldPassword;

    @NotBlank(message = "Nueva Contraseña es requerida")
    private String newPassword;
}
