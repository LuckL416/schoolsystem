package com.school.dormrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("fault_type")
public class FaultType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String workType;
    private Integer urgentLevel;

    // Overdue timeout (Module 4)
    private Integer timeoutHours;
}
