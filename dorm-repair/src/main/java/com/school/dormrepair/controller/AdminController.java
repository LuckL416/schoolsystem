package com.school.dormrepair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.FaultType;
import com.school.dormrepair.entity.User;
import com.school.dormrepair.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public Result<Page<User>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return adminService.listUsers(page, size);
    }

    @PostMapping("/users")
    public Result<String> addUser(@RequestBody User user) {
        return adminService.addUser(user);
    }

    @PutMapping("/users/{id}")
    public Result<String> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return adminService.updateUser(user);
    }

    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        return adminService.deleteUser(id);
    }

    // ========== 故障类型管理 ==========

    @PostMapping("/fault-types")
    public Result<String> addFaultType(@RequestBody FaultType ft) {
        return adminService.addFaultType(ft);
    }

    @PutMapping("/fault-types/{id}")
    public Result<String> updateFaultType(@PathVariable Long id, @RequestBody FaultType ft) {
        ft.setId(id);
        return adminService.updateFaultType(ft);
    }

    @DeleteMapping("/fault-types/{id}")
    public Result<String> deleteFaultType(@PathVariable Long id) {
        return adminService.deleteFaultType(id);
    }
}
