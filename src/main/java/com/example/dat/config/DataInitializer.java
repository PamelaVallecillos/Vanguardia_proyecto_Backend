package com.example.dat.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.dat.role.entity.Role;
import com.example.dat.role.repo.RoleRepo;
import com.example.dat.users.entity.User;
import com.example.dat.users.repo.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepo roleRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.create-admin:false}")
    private boolean createAdmin;

    @Value("${app.seed.admin-email:admin@example.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // Ensure roles exist
        List<String> roleNames = List.of("PATIENT", "DOCTOR", "ADMIN");
        List<Role> created = new ArrayList<>();
        for (String name : roleNames) {
            Optional<Role> maybe = roleRepo.findByName(name);
            if (maybe.isEmpty()) {
                Role r = Role.builder().name(name).build();
                Role saved = roleRepo.save(r);
                created.add(saved);
                log.info("Created role: {}", name);
            }
        }

        if (createAdmin) {
            // create admin user if not exists
            if (userRepo.findByEmail(adminEmail).isEmpty()) {
                Optional<Role> adminRole = roleRepo.findByName("ADMIN");
                List<Role> roles = new ArrayList<>();
                adminRole.ifPresent(roles::add);

                User admin = User.builder()
                        .name("Administrator")
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .roles(roles)
                        .build();

                userRepo.save(admin);
                log.info("Admin user created: {}", adminEmail);
            } else {
                log.info("Admin user already exists: {}", adminEmail);
            }
        }

        if (created.isEmpty()) {
            log.info("No new roles were created; roles already exist.");
        }
    }
}
