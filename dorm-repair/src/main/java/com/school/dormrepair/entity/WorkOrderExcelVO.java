package com.school.dormrepair.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderExcelVO {
    @ExcelProperty("编号")
    private String orderNo;

    @ExcelProperty("宿舍ID")
    private Long dormId;

    @ExcelProperty("故障类型ID")
    private Long faultTypeId;

    @ExcelProperty("故障描述")
    private String description;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("学生ID")
    private Long studentId;

    @ExcelProperty("师傅ID")
    private Long teacherId;

    @ExcelProperty("提交时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    @ExcelProperty("完成时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @ExcelProperty("维修备注")
    private String remark;

    @ExcelProperty("评分")
    private Integer evaluateStar;

    public static WorkOrderExcelVO from(WorkOrder o) {
        WorkOrderExcelVO vo = new WorkOrderExcelVO();
        vo.setOrderNo(o.getOrderNo());
        vo.setDormId(o.getDormId());
        vo.setFaultTypeId(o.getFaultTypeId());
        vo.setDescription(o.getDescription());
        vo.setStatus(o.getStatus());
        vo.setStudentId(o.getStudentId());
        vo.setTeacherId(o.getTeacherId());
        vo.setSubmitTime(o.getSubmitTime());
        vo.setCompleteTime(o.getCompleteTime());
        vo.setRemark(o.getRemark());
        vo.setEvaluateStar(o.getEvaluateStar());
        return vo;
    }
}
