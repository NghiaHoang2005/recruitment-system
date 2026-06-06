package com.recruitment.backend.controllers;

import com.recruitment.backend.config.CustomJwtDecoder;
import com.recruitment.backend.config.SecurityConfig;
import com.recruitment.backend.domain.dtos.AdminDashboardResponse;
import com.recruitment.backend.services.AdminAnalyticsService;
import com.recruitment.backend.services.AdminAuditLogService;
import com.recruitment.backend.services.AdminCompanyService;
import com.recruitment.backend.services.AdminDashboardService;
import com.recruitment.backend.services.AdminJobService;
import com.recruitment.backend.services.AdminSettingsService;
import com.recruitment.backend.services.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomJwtDecoder customJwtDecoder;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AdminCompanyService adminCompanyService;

    @MockBean
    private AdminJobService adminJobService;

    @MockBean
    private AdminAnalyticsService adminAnalyticsService;

    @MockBean
    private AdminAuditLogService adminAuditLogService;

    @MockBean
    private AdminSettingsService adminSettingsService;

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminDashboard() throws Exception {
        when(adminDashboardService.getDashboard()).thenReturn(AdminDashboardResponse.builder().build());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());
    }
}
