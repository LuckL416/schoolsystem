package com.school.dormrepair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("inventory_record")
public class InventoryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String type;
    private Integer quantity;
    private Long operatorId;
    private Long workOrderId;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
