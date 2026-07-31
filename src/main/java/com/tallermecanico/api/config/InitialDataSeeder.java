package com.tallermecanico.api.config;

import com.tallermecanico.api.user.Role;
import com.tallermecanico.api.user.RoleName;
import com.tallermecanico.api.user.RoleRepository;
import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.user.SystemUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class InitialDataSeeder {

    @Bean
    CommandLineRunner seedInitialData(
            RoleRepository roleRepository,
            SystemUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.enabled:false}") boolean bootstrapEnabled
    ) {
        return args -> seed(roleRepository, userRepository, passwordEncoder, bootstrapEnabled);
    }

    @Transactional
    void seed(
            RoleRepository roleRepository,
            SystemUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            boolean bootstrapEnabled
    ) {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));
        Role employeeRole = roleRepository.findByName(RoleName.EMPLEADO)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.EMPLEADO)));

        if (!bootstrapEnabled) {
            return;
        }

        if (userRepository.findByUsernameIgnoreCase("admin").isEmpty()) {
            userRepository.save(new SystemUser("admin", "Administrador del taller", passwordEncoder.encode("Admin123!"), adminRole));
        }
        if (userRepository.findByUsernameIgnoreCase("empleado").isEmpty()) {
            userRepository.save(new SystemUser("empleado", "Empleado de demostración", passwordEncoder.encode("Empleado123!"), employeeRole));
        }
    }
}
