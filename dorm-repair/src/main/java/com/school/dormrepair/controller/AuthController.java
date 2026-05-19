package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.User;
import com.school.dormrepair.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<String> login(
            @RequestParam String username,
            @RequestParam String password
    ) {
        return userService.login(username, password);
    }

    @GetMapping("/info")
    public Result<User> info(@RequestAttribute("userId") Long userId) {
        return userService.getUserInfo(userId);
    }

    @PostMapping("/register")
    public Result<String> register(
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long dormId
    ) {
        return userService.register(name, username, password, phone, dormId);
    }

    @PostMapping("/change-password")
    public Result<String> changePassword(
            @RequestAttribute("userId") Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword
    ) {
        return userService.changePassword(userId, oldPassword, newPassword);
    }
}
