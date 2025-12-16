package com.example.dat.patient.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "expediente_sequence")
public class ExpedienteSequence {

    @Id
    private Long id = 1L; // Solo un registro en la tabla
    
    private Integer lastNumber = 0; // Último número de expediente generado
}
