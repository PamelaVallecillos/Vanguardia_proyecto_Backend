package com.example.dat.dependent.entity;

import com.example.dat.appointment.entity.Appointment;
import com.example.dat.enums.BloodGroup;
import com.example.dat.enums.Genotype;
import com.example.dat.enums.converter.BloodGroupConverter;
import com.example.dat.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "dependents")
public class Dependent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_number", unique = true, nullable = false, length = 5)
    private String expedienteNumber;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 50)
    private String gender; // MASCULINO, FEMENINO, OTRO

    @Column(name = "relationship", length = 50)
    private String relationship; // HIJO/HIJA, PADRE/MADRE, CONYUGE, HERMANO/HERMANA, OTRO

    @Column(name = "profile_photo", length = 500)
    private String profilePhoto; // File path: uploads/dependents/photo.jpg

    // Medical Fields
    @Lob
    private String knownAllergies;

    @Convert(converter = BloodGroupConverter.class)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    private Genotype genotype;

    // Relationship: Dependent belongs to a Patient (titular)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Dependent can have their own appointments
    @OneToMany(mappedBy = "dependent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointments;
}
