package com.example.dat.dependent.dto;

import com.example.dat.enums.BloodGroup;
import com.example.dat.enums.Genotype;
import com.example.dat.patient.dto.PatientDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DependentDTO {

    private Long id;

    private String expedienteNumber;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    private String gender;
    private String relationship;
    private String profilePhoto;

    private String knownAllergies;

    private BloodGroup bloodGroup;

    private Genotype genotype;

    private PatientDTO patient; // Reference to titular patient
}
