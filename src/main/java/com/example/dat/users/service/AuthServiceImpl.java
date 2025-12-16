package com.example.dat.users.service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dat.doctor.entity.Doctor;
import com.example.dat.doctor.repo.DoctorRepo;
import com.example.dat.exceptions.BadRequestException;
import com.example.dat.exceptions.NotFoundException;
import com.example.dat.notification.dto.NotificationDTO;
import com.example.dat.notification.service.NotificationService;
import com.example.dat.patient.entity.Patient;
import com.example.dat.patient.repo.PatientRepo;
import com.example.dat.res.Response;
import com.example.dat.role.entity.Role;
import com.example.dat.role.repo.RoleRepo;
import com.example.dat.security.JwtService;
import com.example.dat.users.dto.LoginRequest;
import com.example.dat.users.dto.LoginResponse;
import com.example.dat.users.dto.RegistrationRequest;
import com.example.dat.users.dto.ResetPasswordRequest;
import com.example.dat.users.entity.PasswordResetCode;
import com.example.dat.users.entity.User;
import com.example.dat.users.repo.PasswordResetRepo;
import com.example.dat.users.repo.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;

    private final PatientRepo patientRepo;
    private final DoctorRepo doctorRepo;

    private final PasswordResetRepo passwordResetRepo;
    private final CodeGenerator codeGenerator; //



    @Value("${password.reset.link}")
    private String resetLink;

    @Value("${login.link}")
    private String loginLink;



    @Override
    public Response<String> register(RegistrationRequest request) {
        /// 1. Check if user already exists
                if (userRepo.findByEmail(request.getEmail()).isPresent()) {
                        throw new BadRequestException("Ya existe un usuario con este correo electrónico");
        }

        // Determine the roles to assign. Default to PATIENT if none are provided.
        List<String> requestedRoleNames = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles().stream().map(String::toUpperCase).toList()
                : List.of("PATIENT");


        boolean isDoctor = requestedRoleNames.contains("DOCTOR");

                if (isDoctor && (request.getLicenseNumber() == null || request.getLicenseNumber().isBlank())) {
                        throw new BadRequestException("Se requiere número de licencia para registrar un doctor.");
        }

        /// 2. Load and validate roles from the database
        List<Role> roles = requestedRoleNames.stream() //==>
                .map(roleRepo::findByName) //==>
                .flatMap(Optional::stream) //==>
                .toList();


                if (roles.isEmpty()) {
                        throw new NotFoundException("Registro fallido: los roles solicitados no se encontraron en la base de datos.");
        }
        /// 3. Create and save new user entity
        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .roles(roles) 
                .build();

        User savedUser = userRepo.save(newUser);

        log.info("New user registered: {} with {} roles.", savedUser.getEmail(), roles.size());


        /// 4. Process Profile Creation


        for (Role role : roles) {
            String roleName = role.getName();

            switch (roleName) {
                case "PATIENT":
                    createPatientProfile(savedUser);
                    log.info("Patient profile created: {}", savedUser.getEmail());
                    break;

                case "DOCTOR":
                    createDoctorProfile(request, savedUser);
                    log.info("Doctor profile created: {}", savedUser.getEmail());
                    break;

                case "ADMIN":
                    log.info("Admin role assigned to user: {}", savedUser.getEmail());
                    break;

                default:
                    log.warn("Assigned role '{}' has no corresponding profile creation logic.", roleName);
                    break;
            }
        }

        /// 5. Send welcome email out
        sendRegistrationEmail(request, savedUser);

        // 6. Return success response
        return Response.<String>builder()
                .statusCode(200)
                .message("Registro exitoso. Se ha enviado un correo de bienvenida.")
                .data(savedUser.getEmail())
                .build();


    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();


                // For security, do not reveal whether email or password was incorrect.
                // Always return a generic "invalid credentials" error.
                User user = userRepo.findByEmail(email)
                                .orElseThrow(() -> new BadRequestException("Credenciales inválidas. Verifica tu email y contraseña"));

                if (!passwordEncoder.matches(password, user.getPassword())) {
                        throw new BadRequestException("Credenciales inválidas. Verifica tu email y contraseña");
                }

        String token = jwtService.generateToken(user.getEmail());

        LoginResponse loginResponse = LoginResponse.builder()
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .token(token)
                .build();

        return Response.<LoginResponse>builder()
                .statusCode(200)
                .message("Login Successful")
                .data(loginResponse)
                .build();

    }

        @Override
        @Transactional
        public Response<?> forgetPassword(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        passwordResetRepo.deleteByUserId(user.getId());

        String code = codeGenerator.generateUniqueCode();

        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .code(code)
                .expiryDate(calculateExpiryDate())
                .used(false)
                .build();

        passwordResetRepo.save(resetCode);

        //send email reset link out
        NotificationDTO passwordResetEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Password Reset Code")
                .templateName("password-reset")
                .templateVariables(Map.of( // Using Map.of() for concise, immutable map creation
                        "name", user.getName(),
                        "resetLink", resetLink + code
                ))
                .build();

        notificationService.sendEmail(passwordResetEmail, user);

        return Response.builder()
                .statusCode(200)
                .message("Código de restablecimiento de contraseña enviado a tu correo")
                .build();
    }

        @Override
        @Transactional
        public Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest) {

        String code = resetPasswordRequest.getCode();
        String newPassword = resetPasswordRequest.getNewPassword();

        log.info("CODE IS: " + code);
        log.info("NEW PASSWORD IS: " + newPassword);


        // Find and validate code
        PasswordResetCode resetCode = passwordResetRepo.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Código de restablecimiento inválido"));      

        // Check expiration first
        if (resetCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetRepo.delete(resetCode); // Clean up expired code
            throw new BadRequestException("El código de restablecimiento ha expirado");
        }

        //update the password
        User user = resetCode.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Delete the code immediately after successful use
        passwordResetRepo.delete(resetCode);


        // Send password confirmation email
        NotificationDTO passwordResetEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Contraseña actualizada correctamente")
                .templateName("password-update-confirmation")
                .templateVariables(Map.of(
                        "name", user.getName()
                ))
                .build();

        notificationService.sendEmail(passwordResetEmail, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Contraseña actualizada correctamente")
                .build();

    }



    private void createPatientProfile( User user){

        Patient patient = Patient.builder()
                .user(user)
                .build();
        patientRepo.save(patient);
        log.info("Patient profile created");

    }

    private void createDoctorProfile(RegistrationRequest request, User user){

        Doctor doctor = Doctor.builder()
                .specialization(request.getSpecialization())
                .licenseNumber(request.getLicenseNumber())
                .user(user)
                .build();

        doctorRepo.save(doctor);

        log.info("Doctor profile created");
    }

    private void sendRegistrationEmail(RegistrationRequest request, User user){
        NotificationDTO welcomeEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("¡Bienvenido a DAT Health!")
                .templateName("welcome")
                .message("Gracias por registrarte. Tu cuenta está lista.")
                .templateVariables(Map.of(
                        "name", request.getName(),
                        "loginLink", loginLink
                ))
                .build();

        notificationService.sendEmail(welcomeEmail, user);
    }


    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(5);
    }

}
