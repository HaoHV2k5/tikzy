package com.tikzy.event.config;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.Event;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.EventRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "tikzy.seed.events", name = "enabled", havingValue = "true")
public class EventDemoSeeder implements ApplicationRunner {

    private static final String ORGANIZER_ROLE_CODE = "ROLE_ORGANIZER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public EventDemoSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CategoryRepository categoryRepository,
            EventRepository eventRepository,
            PasswordEncoder passwordEncoder,
            @Value("${tikzy.seed.organizer.email}") String email,
            @Value("${tikzy.seed.organizer.password}") String password,
            @Value("${tikzy.seed.organizer.full-name}") String fullName) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
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
            throw new IllegalStateException("tikzy.seed.organizer.email must be configured");
        }

        User organizer = resolveOrganizer(normalizedEmail);
        int created = 0;
        for (SeedEvent seed : seedEvents()) {
            if (eventRepository.existsByOrganizerIdAndTitle(organizer.getId(), seed.title())) {
                continue;
            }
            Category category = categoryRepository.findBySlug(seed.categorySlug())
                    .filter(item -> item.getStatus() == CategoryStatus.PUBLISHED)
                    .orElse(null);
            if (category == null) {
                log.warn("Bỏ qua seed sự kiện '{}' vì không có danh mục published '{}'",
                        seed.title(),
                        seed.categorySlug());
                continue;
            }
            eventRepository.save(Event.builder()
                    .organizer(organizer)
                    .category(category)
                    .title(seed.title())
                    .description(seed.description())
                    .venueName(seed.venueName())
                    .venueAddress(seed.venueAddress())
                    .status(EventStatus.DRAFT)
                    .refundPolicy(seed.refundPolicy())
                    .refundDeadlineDays(seed.refundDeadlineDays())
                    .refundFeePercentage(seed.refundFeePercentage())
                    .build());
            created++;
        }
        log.info("Seeded {} demo events for {}", created, normalizedEmail);
    }

    private User resolveOrganizer(String normalizedEmail) {
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user != null) {
            if (user.getRole() == null
                    || !ORGANIZER_ROLE_CODE.equals(user.getRole().getCode())) {
                user.setRole(findOrganizerRole());
                user = userRepository.save(user);
                log.info("Promoted {} to organizer for event seed", normalizedEmail);
            }
            return user;
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("tikzy.seed.organizer.password must be configured");
        }
        user = User.builder()
                .role(findOrganizerRole())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(resolveFullName())
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();
        user = userRepository.save(user);
        log.info("Seeded local organizer account {}", normalizedEmail);
        return user;
    }

    private Role findOrganizerRole() {
        return roleRepository.findByCode(ORGANIZER_ROLE_CODE)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private List<SeedEvent> seedEvents() {
        return List.of(
                new SeedEvent(
                        "am-nhac",
                        "Hòa nhạc mùa hè",
                        "Đêm nhạc acoustic ngoài trời với các nghệ sĩ độc lập.",
                        "Công viên Tao Đàn",
                        "Trần Cao Vân, Quận 1, TP.HCM",
                        RefundPolicy.NO_REFUND,
                        null,
                        null),
                new SeedEvent(
                        "san-khau",
                        "Kịch Hamlet",
                        "Vở kịch cổ điển được dàn dựng lại theo phong cách hiện đại.",
                        "Nhà hát Thành phố",
                        "7 Công Trường Lam Sơn, Quận 1, TP.HCM",
                        RefundPolicy.ALLOW_REFUND,
                        7,
                        new BigDecimal("10.00")),
                new SeedEvent(
                        "the-thao",
                        "Giải chạy đêm Sài Gòn",
                        "Giải chạy 5km và 10km xuyên trung tâm thành phố.",
                        "Phố đi bộ Nguyễn Huệ",
                        "Nguyễn Huệ, Quận 1, TP.HCM",
                        RefundPolicy.NO_REFUND,
                        null,
                        null),
                new SeedEvent(
                        "hoi-thao",
                        "Hội thảo AI 2026",
                        "Chia sẻ ứng dụng AI trong vận hành sự kiện và bán vé.",
                        "GEM Center",
                        "8 Nguyễn Bỉnh Khiêm, Quận 1, TP.HCM",
                        RefundPolicy.ALLOW_REFUND,
                        3,
                        new BigDecimal("5.00")),
                new SeedEvent(
                        "le-hoi",
                        "Lễ hội Trung thu",
                        "Lễ hội gia đình với múa lân, gian hàng và biểu diễn đèn lồng.",
                        "Phố ông đồ",
                        "Đường sách Nguyễn Văn Bình, Quận 1, TP.HCM",
                        RefundPolicy.NO_REFUND,
                        null,
                        null));
    }

    private String resolveFullName() {
        if (!StringUtils.hasText(fullName)) {
            return "Ban tổ chức Tikzy";
        }
        return fullName.trim();
    }

    private String normalizeEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record SeedEvent(
            String categorySlug,
            String title,
            String description,
            String venueName,
            String venueAddress,
            RefundPolicy refundPolicy,
            Integer refundDeadlineDays,
            BigDecimal refundFeePercentage) {
    }
}
