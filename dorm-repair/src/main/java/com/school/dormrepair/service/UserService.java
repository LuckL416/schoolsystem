package com.school.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.dormrepair.common.JwtUtil;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.User;
import com.school.dormrepair.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Result<String> login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!encoder.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return Result.success(token);
    }

    public Result<User> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        user.setPassword(null);
        return Result.success(user);
    }

    // 学生自主注册
    public Result<String> register(String name, String username, String password, String phone, Long dormId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectOne(wrapper) != null) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRole("student");
        user.setPhone(phone);
        user.setDormId(dormId);
        user.setCreateTime(java.time.LocalDateTime.now());
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    // 修改密码
    public Result<String> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return Result.error("原密码错误");
        }
        user.setPassword(encoder.encode(newPassword));
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }
}
