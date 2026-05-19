package com.school.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.FaultType;
import com.school.dormrepair.entity.User;
import com.school.dormrepair.mapper.FaultTypeMapper;
import com.school.dormrepair.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FaultTypeMapper faultTypeMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ========== 用户管理 ==========

    public Result<Page<User>> listUsers(Integer page, Integer size) {
        Page<User> userPage = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(userPage, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    public Result<String> addUser(User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            return Result.error("用户名已存在");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success("添加成功");
    }

    public Result<String> updateUser(User user) {
        User exist = userMapper.selectById(user.getId());
        if (exist == null) {
            return Result.error("用户不存在");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(encoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    public Result<String> deleteUser(Long id) {
        if (id == 1) {
            return Result.error("不能删除超级管理员");
        }
        userMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ========== 故障类型管理 ==========

    public Result<String> addFaultType(FaultType ft) {
        faultTypeMapper.insert(ft);
        return Result.success("添加成功");
    }

    public Result<String> updateFaultType(FaultType ft) {
        faultTypeMapper.updateById(ft);
        return Result.success("更新成功");
    }

    public Result<String> deleteFaultType(Long id) {
        faultTypeMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
