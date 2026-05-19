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
}
