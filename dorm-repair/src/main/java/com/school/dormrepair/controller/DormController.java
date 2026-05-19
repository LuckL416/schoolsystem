package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.Dorm;
import com.school.dormrepair.mapper.DormMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dorm")
public class DormController {

    @Autowired
    private DormMapper dormMapper;

    @GetMapping("/list")
    public Result<List<Dorm>> list() {
        return Result.success(dormMapper.selectList(null));
    }
}
