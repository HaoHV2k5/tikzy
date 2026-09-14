package com.tikzy.event.controller;

import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.common.config.JwtAuthenticationFilter;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.config.RestAccessDeniedHandler;
import com.tikzy.common.config.RestAuthenticationEntryPoint;
import com.tikzy.common.config.SecurityConfig;
import com.tikzy.event.dto.request.SearchEventsRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.dto.response.PublicEventDetailResponse;
import com.tikzy.event.dto.response.PublicShowTimeResponse;
import com.tikzy.event.dto.response.PublicTicketOfferResponse;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicEventController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    void getPublished_asAnonymous_returnsEvents() throws Exception {
        EventResponse response = EventResponse.builder()
                .id(UUID.randomUUID())
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.PUBLISHED)
                .build();
        when(eventService.getPublished(isNull(), any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách sự kiện thành công"))
                .andExpect(jsonPath("$.data[0].title").value("Hòa nhạc mùa hè"))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));

        verify(eventService).getPublished(isNull(), any());
    }

    @Test
    void getPublished_withCategory_filtersByCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();
        EventResponse response = EventResponse.builder()
                .id(UUID.randomUUID())
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.PUBLISHED)
                .build();
        when(eventService.getPublished(eq(categoryId), any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/events").param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Hòa nhạc mùa hè"));

        verify(eventService).getPublished(eq(categoryId), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getPublished_asCustomer_returnsEvents() throws Exception {
        when(eventService.getPublished(isNull(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(eventService).getPublished(isNull(), any());
    }

    @Test
    void search_asAnonymous_filtersByKeywordTimeLocationAndPrice() throws Exception {
        EventResponse response = EventResponse.builder()
                .id(UUID.randomUUID())
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.PUBLISHED)
                .build();
        when(eventService.searchPublished(any(), any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/events/search")
                        .param("keyword", "hòa nhạc")
                        .param("location", "Hà Nội")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("minPrice", "100000")
                        .param("maxPrice", "2000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tìm kiếm sự kiện thành công"))
                .andExpect(jsonPath("$.data[0].title").value("Hòa nhạc mùa hè"))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));

        ArgumentCaptor<SearchEventsRequest> captor = ArgumentCaptor.forClass(SearchEventsRequest.class);
        verify(eventService).searchPublished(captor.capture(), any());
        SearchEventsRequest request = captor.getValue();
        assertEquals("hòa nhạc", request.getKeyword());
        assertEquals("Hà Nội", request.getLocation());
        assertEquals(LocalDate.of(2026, 10, 1), request.getFrom());
        assertEquals(LocalDate.of(2026, 10, 31), request.getTo());
        assertEquals(new BigDecimal("100000"), request.getMinPrice());
        assertEquals(new BigDecimal("2000000"), request.getMaxPrice());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void search_asCustomer_returnsEvents() throws Exception {
        when(eventService.searchPublished(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/events/search").param("keyword", "nhạc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(eventService).searchPublished(any(), any());
    }

    @Test
    void getPublishedById_asAnonymous_returnsEventDetail() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID showTimeId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        PublicEventDetailResponse response = PublicEventDetailResponse.builder()
                .id(eventId)
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.PUBLISHED)
                .showTimes(List.of(PublicShowTimeResponse.builder()
                        .id(showTimeId)
                        .startTime(LocalDateTime.of(2026, 10, 15, 19, 0))
                        .endTime(LocalDateTime.of(2026, 10, 15, 22, 0))
                        .ticketOffers(List.of(PublicTicketOfferResponse.builder()
                                .ticketTypeId(ticketTypeId)
                                .name("VIP")
                                .price(new BigDecimal("1500000"))
                                .maxPerOrder(4)
                                .availableQuantity(70)
                                .build()))
                        .build()))
                .build();
        when(eventService.getPublishedById(eventId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin sự kiện thành công"))
                .andExpect(jsonPath("$.data.id").value(eventId.toString()))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.showTimes[0].id").value(showTimeId.toString()))
                .andExpect(jsonPath("$.data.showTimes[0].ticketOffers[0].ticketTypeId")
                        .value(ticketTypeId.toString()))
                .andExpect(jsonPath("$.data.showTimes[0].ticketOffers[0].availableQuantity").value(70));

        verify(eventService).getPublishedById(eventId);
    }
}
