package com.school.dormrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dorm")
public class Dorm {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String building;
    private String room;
}
