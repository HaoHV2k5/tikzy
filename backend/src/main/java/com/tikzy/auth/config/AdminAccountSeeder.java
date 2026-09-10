package com.tikzy.auth.config;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "tikzy.seed.admin", name = "enabled", havingValue = "true")
public class AdminAccountSeeder implements ApplicationRunner {

    private static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminAccountSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${tikzy.seed.admin.email}") String email,
            @Value("${tikzy.seed.admin.password}") String password,
            @Value("${tikzy.seed.admin.full-name}") String fullName) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new IllegalStateException("tikzy.seed.admin.email must be configured");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("tikzy.seed.admin.password must be configured");
        }

        Role adminRole = roleRepository.findByCode(ADMIN_ROLE_CODE)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            user = User.builder()
                    .role(adminRole)
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(password))
                    .fullName(resolveFullName())
                    .isActive(true)
                    .isLocked(false)
                    .failedLoginAttempts(0)
                    .build();
            userRepository.save(user);
            log.info("Seeded local admin account {}", normalizedEmail);
            return;
        }

        user.setRole(adminRole);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(resolveFullName());
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);
        log.info("Reset local admin account {}", normalizedEmail);
    }

    private String resolveFullName() {
        if (!StringUtils.hasText(fullName)) {
            return "Tikzy Administrator";
        }
        return fullName.trim();
    }

    private String normalizeEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
