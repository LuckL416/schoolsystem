package com.school.dormrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("work_order")
public class WorkOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long dormId;
    private Long faultTypeId;
    private String description;
    private String imageUrls;
    private String status; // pending/processing/completed
    private Long studentId;
    private Long teacherId;
    private LocalDateTime submitTime;
    private LocalDateTime acceptTime;
    private LocalDateTime completeTime;
    private Integer evaluateStar;
    private String remark;

    // Acceptance & Rating (Module 1)
    private LocalDateTime acceptanceTime;
    private Integer evaluateAttitude;
    private Integer evaluateSpeed;
    private Integer evaluateQuality;
    private String evaluateComment;
    private LocalDateTime evaluateTime;
}
