package com.example.springboot.controller;

import com.example.springboot.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpSession;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
class AuthControllerTest {
    private MockMvc mvc;
    @Mock
    private UserService userService;
    @Mock
    private AuthenticationManager authenticationManager;
    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    //在每个测试之前，先创建一个实例
    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(userService, authenticationManager)).build();
    }

    @Test
    void returnNotLoginByDefault() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/auth"))
                .andExpect(status().isOk()).andExpect(mvcResult -> Assertions
                .assertTrue(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("用户没有登录")));
    }

    @Test
    void testLogin() throws Exception {
        //检查/auth返回值，处于未登录状态
        mvc.perform(MockMvcRequestBuilders.get("/auth"))
                .andExpect(status().isOk()).andExpect(mvcResult -> Assertions
                .assertTrue(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("用户没有登录")));

        //使用/auth/login登录
        //创建这个map用于得到一个json
        Map<String,String> usernamePassword =new HashMap<>();
        usernamePassword.put("username","MyUser");
        usernamePassword.put("password","MyPassword");
        new ObjectMapper().writeValueAsString(usernamePassword);
        Mockito.when(userService.loadUserByUsername("MyUser")).thenReturn(new User("MyUser",bCryptPasswordEncoder.encode("MyPassword"), Collections.emptyList()));
        Mockito.when(userService.getUserByUsername("MyUser")).thenReturn(new com.example.springboot.entity.User(123,"MyUser",bCryptPasswordEncoder.encode("MyPassword")));
        //再次检查/auth返回值，处于登录状态
        MvcResult response = mvc.perform(MockMvcRequestBuilders.post("/auth/login").contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(new ObjectMapper().writeValueAsString(usernamePassword)))
                .andExpect(status().isOk())
                .andExpect(result -> {
//                        System.out.println(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
                    assertTrue(result.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("登录成功"));
                })
                .andReturn();

//        System.out.println(Arrays.toString(response.getResponse().getCookies()));
        HttpSession session = response.getRequest().getSession();
        mvc.perform(MockMvcRequestBuilders.get("/auth").session((MockHttpSession) session))
                .andExpect(status().isOk()).andExpect(new ResultMatcher() {
            @Override
            public void match(MvcResult mvcResult) throws Exception {
//                System.out.println(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
                Assertions
                        .assertTrue(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("MyUser"));
            }
        });

    }


}