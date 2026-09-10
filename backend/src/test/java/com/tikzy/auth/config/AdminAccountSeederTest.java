package com.tikzy.auth.config;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountSeederTest {

    private static final String EMAIL = "admin@tikzy.local";
    private static final String PASSWORD = "Admin@123456";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Role adminRole;
    private AdminAccountSeeder seeder;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().code("ROLE_ADMIN").name("Quản trị viên").build();
        adminRole.setId(UUID.randomUUID());
        seeder = new AdminAccountSeeder(
                userRepository,
                roleRepository,
                passwordEncoder,
                " " + EMAIL.toUpperCase() + " ",
                PASSWORD,
                " Tikzy Administrator ");
    }

    @Test
    void run_createsAdminWhenMissing() {
        when(roleRepository.findByCode("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(EMAIL, saved.getEmail());
        assertEquals(adminRole, saved.getRole());
        assertEquals("Tikzy Administrator", saved.getFullName());
        assertTrue(saved.getIsActive());
        assertFalse(saved.getIsLocked());
        assertEquals(0, saved.getFailedLoginAttempts());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPasswordHash()));
    }

    @Test
    void run_resetsExistingAccountToAdmin() {
        Role customerRole = Role.builder().code("ROLE_CUSTOMER").name("Khách hàng").build();
        customerRole.setId(UUID.randomUUID());
        User existing = User.builder()
                .role(customerRole)
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode("old-password"))
                .fullName("Old Name")
                .isActive(false)
                .isLocked(true)
                .failedLoginAttempts(5)
                .lockedAt(LocalDateTime.now())
                .build();
        existing.setId(UUID.randomUUID());

        when(roleRepository.findByCode("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        seeder.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(existing.getId(), saved.getId());
        assertEquals(adminRole, saved.getRole());
        assertEquals("Tikzy Administrator", saved.getFullName());
        assertTrue(saved.getIsActive());
        assertFalse(saved.getIsLocked());
        assertEquals(0, saved.getFailedLoginAttempts());
        assertNull(saved.getLockedAt());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPasswordHash()));
    }

    @Test
    void run_missingAdminRole_throws() {
        when(roleRepository.findByCode("ROLE_ADMIN")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> seeder.run(null));

        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void run_blankPassword_throws() {
        AdminAccountSeeder invalidSeeder = new AdminAccountSeeder(
                userRepository,
                roleRepository,
                passwordEncoder,
                EMAIL,
                " ",
                "Tikzy Administrator");

        assertThrows(IllegalStateException.class, () -> invalidSeeder.run(null));
    }
}
