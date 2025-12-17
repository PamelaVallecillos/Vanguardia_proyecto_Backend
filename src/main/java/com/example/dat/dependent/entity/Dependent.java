package com.example.dat.dependent.entity;

import java.time.LocalDate;
import java.util.List;

import com.example.dat.appointment.entity.Appointment;
import com.example.dat.enums.BloodGroup;
import com.example.dat.enums.Genotype;
import com.example.dat.patient.entity.Patient;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

    @Enumerated(EnumType.STRING)
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

