package com.vetri.smartcampus.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginRouteReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("Auth/Login"));
    }

    @Test
    void loginHtmlRouteReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(view().name("Auth/Login"));
    }

    @Test
    void loginRouteRedirectsLoggedInTeacher() throws Exception {
        mockMvc.perform(get("/login").sessionAttr("teacherId", 10L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher-dashboard"));
    }
}
