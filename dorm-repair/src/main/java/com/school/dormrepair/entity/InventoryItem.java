package com.school.dormrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("inventory_item")
public class InventoryItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private Integer quantity;
    private Integer safetyThreshold;
    private String unit;
    private LocalDateTime updateTime;
}
