package com.tikzy.event.config;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.ShowTimeRepository;
import com.tikzy.event.repository.TicketTypeRepository;
import com.tikzy.ticket.entity.ShowTimeTicketInventory;
import com.tikzy.ticket.repository.ShowTimeTicketInventoryRepository;
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
import java.time.LocalDateTime;
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
    private final ShowTimeRepository showTimeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final ShowTimeTicketInventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public EventDemoSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CategoryRepository categoryRepository,
            EventRepository eventRepository,
            ShowTimeRepository showTimeRepository,
            TicketTypeRepository ticketTypeRepository,
            ShowTimeTicketInventoryRepository inventoryRepository,
            PasswordEncoder passwordEncoder,
            @Value("${tikzy.seed.organizer.email}") String email,
            @Value("${tikzy.seed.organizer.password}") String password,
            @Value("${tikzy.seed.organizer.full-name}") String fullName) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.showTimeRepository = showTimeRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.inventoryRepository = inventoryRepository;
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
        int createdEvents = 0;
        int createdShowTimes = 0;
        int createdTicketTypes = 0;
        int createdInventories = 0;
        for (SeedEvent seed : seedEvents()) {
            Event event = eventRepository
                    .findByOrganizerIdAndTitle(organizer.getId(), seed.title())
                    .orElse(null);
            if (event == null) {
                Category category = categoryRepository.findBySlug(seed.categorySlug())
                        .filter(item -> item.getStatus() == CategoryStatus.PUBLISHED)
                        .orElse(null);
                if (category == null) {
                    log.warn("Bỏ qua seed sự kiện '{}' vì không có danh mục published '{}'",
                            seed.title(),
                            seed.categorySlug());
                    continue;
                }
                event = eventRepository.save(Event.builder()
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
                createdEvents++;
            }
            createdShowTimes += seedShowTimes(event, seed.showTimes());
            createdTicketTypes += seedTicketTypes(event, seed.ticketTypes());
            createdInventories += seedInventories(event);
        }
        log.info(
                "Seeded {} demo events, {} demo show times, {} demo ticket types and {} demo inventories for {}",
                createdEvents,
                createdShowTimes,
                createdTicketTypes,
                createdInventories,
                normalizedEmail);
    }

    private int seedShowTimes(Event event, List<SeedShowTime> showTimes) {
        int created = 0;
        for (SeedShowTime slot : showTimes) {
            if (showTimeRepository.existsByEventIdAndStartTimeAndEndTime(
                    event.getId(),
                    slot.startTime(),
                    slot.endTime())) {
                continue;
            }
            showTimeRepository.save(ShowTime.builder()
                    .event(event)
                    .startTime(slot.startTime())
                    .endTime(slot.endTime())
                    .isActive(true)
                    .build());
            created++;
        }
        return created;
    }

    private int seedTicketTypes(Event event, List<SeedTicketType> ticketTypes) {
        int created = 0;
        for (SeedTicketType type : ticketTypes) {
            if (ticketTypeRepository.existsByEventIdAndNameIgnoreCase(event.getId(), type.name())) {
                continue;
            }
            ticketTypeRepository.save(TicketType.builder()
                    .event(event)
                    .name(type.name())
                    .price(type.price())
                    .maxPerOrder(type.maxPerOrder())
                    .isActive(type.isActive())
                    .build());
            created++;
        }
        return created;
    }

    private int seedInventories(Event event) {
        List<ShowTime> showTimes = showTimeRepository.findAllByEventIdOrderByStartTimeAsc(event.getId());
        List<TicketType> ticketTypes = ticketTypeRepository.findAllByEventIdOrderByCreatedAtAsc(event.getId());
        int created = 0;
        for (ShowTime showTime : showTimes) {
            for (TicketType ticketType : ticketTypes) {
                if (inventoryRepository.existsByShowTimeIdAndTicketTypeId(showTime.getId(), ticketType.getId())) {
                    continue;
                }
                inventoryRepository.save(ShowTimeTicketInventory.builder()
                        .showTime(showTime)
                        .ticketType(ticketType)
                        .totalQuantity(seedQuantity(ticketType.getName()))
                        .reservedQuantity(0)
                        .soldQuantity(0)
                        .build());
                created++;
            }
        }
        return created;
    }

    private int seedQuantity(String ticketTypeName) {
        return switch (ticketTypeName) {
            case "Early Bird" -> 80;
            case "GA" -> 200;
            case "VIP" -> 40;
            case "VVIP" -> 12;
            case "Standard" -> 150;
            case "5km" -> 300;
            case "10km" -> 200;
            case "Regular" -> 120;
            case "Student" -> 60;
            case "Trẻ em" -> 100;
            case "Người lớn" -> 200;
            case "Gia đình" -> 40;
            default -> 100;
        };
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
                        null,
                        List.of(
                                showTime(2026, 10, 15, 19, 0, 22, 0),
                                showTime(2026, 10, 16, 19, 0, 22, 0),
                                showTime(2026, 10, 17, 20, 0, 23, 0)),
                        List.of(
                                ticketType("Early Bird", "350000.00", 6, true),
                                ticketType("GA", "550000.00", 10, true),
                                ticketType("VIP", "1500000.00", 4, true))),
                new SeedEvent(
                        "san-khau",
                        "Kịch Hamlet",
                        "Vở kịch cổ điển được dàn dựng lại theo phong cách hiện đại.",
                        "Nhà hát Thành phố",
                        "7 Công Trường Lam Sơn, Quận 1, TP.HCM",
                        RefundPolicy.ALLOW_REFUND,
                        7,
                        new BigDecimal("10.00"),
                        List.of(
                                showTime(2026, 11, 1, 19, 30, 22, 0),
                                showTime(2026, 11, 2, 15, 0, 17, 30),
                                showTime(2026, 11, 2, 19, 30, 22, 0)),
                        List.of(
                                ticketType("Standard", "400000.00", 8, true),
                                ticketType("VIP", "800000.00", 4, true),
                                ticketType("VVIP", "1800000.00", 2, true))),
                new SeedEvent(
                        "the-thao",
                        "Giải chạy đêm Sài Gòn",
                        "Giải chạy 5km và 10km xuyên trung tâm thành phố.",
                        "Phố đi bộ Nguyễn Huệ",
                        "Nguyễn Huệ, Quận 1, TP.HCM",
                        RefundPolicy.NO_REFUND,
                        null,
                        null,
                        List.of(
                                showTime(2026, 11, 15, 18, 0, 21, 0),
                                showTime(2026, 11, 16, 5, 0, 8, 0)),
                        List.of(
                                ticketType("5km", "250000.00", 2, true),
                                ticketType("10km", "350000.00", 2, true))),
                new SeedEvent(
                        "hoi-thao",
                        "Hội thảo AI 2026",
                        "Chia sẻ ứng dụng AI trong vận hành sự kiện và bán vé.",
                        "GEM Center",
                        "8 Nguyễn Bỉnh Khiêm, Quận 1, TP.HCM",
                        RefundPolicy.ALLOW_REFUND,
                        3,
                        new BigDecimal("5.00"),
                        List.of(
                                showTime(2026, 10, 20, 8, 30, 12, 0),
                                showTime(2026, 10, 20, 13, 30, 17, 0),
                                showTime(2026, 10, 21, 9, 0, 12, 0)),
                        List.of(
                                ticketType("Regular", "500000.00", 5, true),
                                ticketType("Student", "200000.00", 2, true),
                                ticketType("VIP", "1500000.00", 3, true))),
                new SeedEvent(
                        "le-hoi",
                        "Lễ hội Trung thu",
                        "Lễ hội gia đình với múa lân, gian hàng và biểu diễn đèn lồng.",
                        "Phố ông đồ",
                        "Đường sách Nguyễn Văn Bình, Quận 1, TP.HCM",
                        RefundPolicy.NO_REFUND,
                        null,
                        null,
                        List.of(
                                showTime(2026, 10, 6, 17, 0, 21, 0),
                                showTime(2026, 10, 7, 17, 0, 21, 0)),
                        List.of(
                                ticketType("Trẻ em", "50000.00", 6, true),
                                ticketType("Người lớn", "150000.00", 10, true),
                                ticketType("Gia đình", "350000.00", 4, false))));
    }

    private SeedShowTime showTime(
            int year,
            int month,
            int day,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute) {
        return new SeedShowTime(
                LocalDateTime.of(year, month, day, startHour, startMinute),
                LocalDateTime.of(year, month, day, endHour, endMinute));
    }

    private SeedTicketType ticketType(String name, String price, int maxPerOrder, boolean isActive) {
        return new SeedTicketType(name, new BigDecimal(price), maxPerOrder, isActive);
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
            BigDecimal refundFeePercentage,
            List<SeedShowTime> showTimes,
            List<SeedTicketType> ticketTypes) {
    }

    private record SeedShowTime(LocalDateTime startTime, LocalDateTime endTime) {
    }

    private record SeedTicketType(String name, BigDecimal price, int maxPerOrder, boolean isActive) {
    }
}
