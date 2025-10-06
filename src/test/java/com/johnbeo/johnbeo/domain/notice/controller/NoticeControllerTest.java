package com.johnbeo.johnbeo.domain.notice.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.johnbeo.johnbeo.domain.notice.dto.NoticeResponse;
import com.johnbeo.johnbeo.domain.notice.service.NoticeService;
import com.johnbeo.johnbeo.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = NoticeController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoticeService noticeService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @TestConfiguration
    static class MockSecurityConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return Mockito.mock(JwtTokenProvider.class);
        }
    }

    @Test
    void getNoticesReturnsList() throws Exception {
        when(noticeService.getActiveNotices(anyInt())).thenReturn(List.of(
            new NoticeResponse(1L, "공지", "내용", 2, Instant.parse("2025-10-05T10:00:00Z"), null)
        ));

        mockMvc.perform(get("/api/notices").param("limit", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("공지"));
    }

    @Test
    void invalidLimitFallsBackToDefault() throws Exception {
        when(noticeService.getActiveNotices(Mockito.anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/notices").param("limit", "0"))
            .andExpect(status().isOk());
    }
}
