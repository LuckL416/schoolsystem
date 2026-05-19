package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.FaultType;
import com.school.dormrepair.mapper.FaultTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fault-type")
public class FaultTypeController {

    @Autowired
    private FaultTypeMapper faultTypeMapper;

    @GetMapping("/list")
    public Result<List<FaultType>> list() {
        return Result.success(faultTypeMapper.selectList(null));
    }
}
