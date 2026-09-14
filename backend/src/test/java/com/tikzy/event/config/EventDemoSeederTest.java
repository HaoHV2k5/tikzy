package com.tikzy.event.config;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.ShowTimeRepository;
import com.tikzy.event.repository.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDemoSeederTest {

    private static final String EMAIL = "organizer@tikzy.local";
    private static final String PASSWORD = "Organizer@123456";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ShowTimeRepository showTimeRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Role organizerRole;
    private User organizer;
    private EventDemoSeeder seeder;

    @BeforeEach
    void setUp() {
        organizerRole = Role.builder().code("ROLE_ORGANIZER").name("Ban tổ chức").build();
        organizerRole.setId(UUID.randomUUID());
        organizer = User.builder()
                .role(organizerRole)
                .email(EMAIL)
                .passwordHash("hash")
                .fullName("Ban tổ chức Tikzy")
                .isActive(true)
                .build();
        organizer.setId(UUID.randomUUID());
        seeder = new EventDemoSeeder(
                userRepository,
                roleRepository,
                categoryRepository,
                eventRepository,
                showTimeRepository,
                ticketTypeRepository,
                passwordEncoder,
                " " + EMAIL.toUpperCase() + " ",
                PASSWORD,
                " Ban tổ chức Tikzy ");
    }

    @Test
    void run_createsOrganizerAndFiveDraftEvents() {
        when(roleRepository.findByCode("ROLE_ORGANIZER")).thenReturn(Optional.of(organizerRole));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(organizer);
        when(eventRepository.findByOrganizerIdAndTitle(any(), any())).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(UUID.randomUUID());
            }
            return event;
        });
        when(showTimeRepository.existsByEventIdAndStartTimeAndEndTime(any(), any(), any())).thenReturn(false);
        when(ticketTypeRepository.existsByEventIdAndNameIgnoreCase(any(), any())).thenReturn(false);
        stubPublishedCategories();

        seeder.run(null);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(5)).save(eventCaptor.capture());
        List<Event> saved = eventCaptor.getAllValues();
        assertEquals(5, saved.size());
        assertTrue(saved.stream().allMatch(event -> event.getStatus() == EventStatus.DRAFT));
        assertEquals("Hòa nhạc mùa hè", saved.get(0).getTitle());
        assertEquals("Kịch Hamlet", saved.get(1).getTitle());
        assertEquals("Giải chạy đêm Sài Gòn", saved.get(2).getTitle());
        assertEquals("Hội thảo AI 2026", saved.get(3).getTitle());
        assertEquals("Lễ hội Trung thu", saved.get(4).getTitle());

        ArgumentCaptor<ShowTime> showTimeCaptor = ArgumentCaptor.forClass(ShowTime.class);
        verify(showTimeRepository, times(13)).save(showTimeCaptor.capture());
        List<ShowTime> showTimes = showTimeCaptor.getAllValues();
        assertEquals(13, showTimes.size());
        assertTrue(showTimes.stream().allMatch(ShowTime::getIsActive));
        assertEquals(3, showTimes.stream()
                .filter(showTime -> "Hòa nhạc mùa hè".equals(showTime.getEvent().getTitle()))
                .count());

        ArgumentCaptor<TicketType> ticketTypeCaptor = ArgumentCaptor.forClass(TicketType.class);
        verify(ticketTypeRepository, times(14)).save(ticketTypeCaptor.capture());
        List<TicketType> ticketTypes = ticketTypeCaptor.getAllValues();
        assertEquals(14, ticketTypes.size());
        assertEquals(3, ticketTypes.stream()
                .filter(ticketType -> "Hòa nhạc mùa hè".equals(ticketType.getEvent().getTitle()))
                .count());
        assertTrue(ticketTypes.stream().anyMatch(ticketType -> "VIP".equals(ticketType.getName())));
        assertTrue(ticketTypes.stream().anyMatch(ticketType -> !ticketType.getIsActive()));
    }

    @Test
    void run_skipsExistingEventsButSeedsMissingShowTimes() {
        Event existing = Event.builder()
                .organizer(organizer)
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.DRAFT)
                .build();
        existing.setId(UUID.randomUUID());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(organizer));
        when(eventRepository.findByOrganizerIdAndTitle(any(), any())).thenReturn(Optional.of(existing));
        when(showTimeRepository.existsByEventIdAndStartTimeAndEndTime(any(), any(), any())).thenReturn(false);
        when(ticketTypeRepository.existsByEventIdAndNameIgnoreCase(any(), any())).thenReturn(false);

        seeder.run(null);

        verify(userRepository, never()).save(any(User.class));
        verify(eventRepository, never()).save(any(Event.class));
        verify(showTimeRepository, times(13)).save(any(ShowTime.class));
        verify(ticketTypeRepository, times(14)).save(any(TicketType.class));
    }

    @Test
    void run_blankPassword_throwsWhenAccountMissing() {
        EventDemoSeeder invalidSeeder = new EventDemoSeeder(
                userRepository,
                roleRepository,
                categoryRepository,
                eventRepository,
                showTimeRepository,
                ticketTypeRepository,
                passwordEncoder,
                EMAIL,
                " ",
                "Ban tổ chức Tikzy");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> invalidSeeder.run(null));
    }

    private void stubPublishedCategories() {
        when(categoryRepository.findBySlug("am-nhac"))
                .thenReturn(Optional.of(publishedCategory("Âm nhạc", "am-nhac")));
        when(categoryRepository.findBySlug("san-khau"))
                .thenReturn(Optional.of(publishedCategory("Sân khấu", "san-khau")));
        when(categoryRepository.findBySlug("the-thao"))
                .thenReturn(Optional.of(publishedCategory("Thể thao", "the-thao")));
        when(categoryRepository.findBySlug("hoi-thao"))
                .thenReturn(Optional.of(publishedCategory("Hội thảo", "hoi-thao")));
        when(categoryRepository.findBySlug("le-hoi"))
                .thenReturn(Optional.of(publishedCategory("Lễ hội", "le-hoi")));
    }

    private Category publishedCategory(String name, String slug) {
        Category category = Category.builder()
                .name(name)
                .normalizedName(name.toLowerCase())
                .slug(slug)
                .status(CategoryStatus.PUBLISHED)
                .sortOrder(0)
                .build();
        category.setId(UUID.randomUUID());
        return category;
    }
}
