package com.example.springboot.controller;


import com.example.springboot.entity.Result;
import com.example.springboot.entity.User;
import com.example.springboot.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.util.Map;

@Controller
public class AuthController {
    private UserService userService;
    private AuthenticationManager authenticationManager;

    @Inject
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }


    @GetMapping("/auth")
    @ResponseBody
    public Object auth() {
        //通过登录状态拿到用户名，用户名是通过Cookie维持的
//        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = userService.getUserByUsername(authentication==null?null:authentication.getName());
        if (loggedInUser == null) {
            return Result.success("用户没有登录", false);
        } else {
            return Result.success(null, true, loggedInUser);
        }
    }

    @RequestMapping("/auth/logout")
    @ResponseBody
    public Object logout() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userService.getUserByUsername(userName);
        if (loggedInUser == null) {
            return Result.failure("用户没有登录");
        } else {
            SecurityContextHolder.clearContext();
            return Result.success("注销成功", false);
        }
    }

    @PostMapping("/auth/register")
    @ResponseBody
    public Result register(@RequestBody Map<String, String> userNameAndPassword) {
        String username = userNameAndPassword.get("username");
        String password = userNameAndPassword.get("password");
        if (username == null || password == null) {
            return Result.failure("username/password==null");
        }
        if (username.length() < 1 || username.length() > 15) {
            return Result.failure("invalid username");
        }
        if (password.length() < 1 || password.length() > 15) {
            return Result.failure("invalid password");
        }
        try {
            userService.save(username, password);
        } catch (DuplicateKeyException e) {
            e.printStackTrace();
            return Result.failure("user already exists");
        }
        return Result.success("success!", false);
    }

    @PostMapping("/auth/login")
    @ResponseBody
    public Result login(@RequestBody Map<String, Object> userNameAndPassword) {
        //将用户发起登录时所填的用户名和密码拿出来
        String username = userNameAndPassword.get("username").toString();
        String password = userNameAndPassword.get("password").toString();

        //从数据库中取出该用户名对应的真正的密码
        UserDetails userDetails = null;
        try {
            userDetails = userService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            return Result.failure("用户不存在");
        }
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, password);

        //将用户所填密码和数据库中密码进行比对，比对的结果在这里处理
        try {
            authenticationManager.authenticate(token);
            //把用户信息保存在一个地方
            //Cookie
            SecurityContextHolder.getContext().setAuthentication(token);
            return Result.success("登录成功", true, userService.getUserByUsername(username));

        } catch (BadCredentialsException e) {
            return Result.failure("密码不正确");
        }
    }

}
