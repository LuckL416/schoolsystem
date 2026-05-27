package com.school.dormrepair.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderExcelVO {
    @ExcelProperty("工单编号")
    private String orderNo;

    @ExcelProperty("宿舍")
    private String dormName;

    @ExcelProperty("故障类型")
    private String faultTypeName;

    @ExcelProperty("故障描述")
    private String description;

    @ExcelProperty("状态")
    private String statusText;

    @ExcelProperty("紧急级别")
    private String urgentText;

    @ExcelProperty("学生")
    private String studentName;

    @ExcelProperty("维修师傅")
    private String teacherName;

    @ExcelProperty("提交时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    @ExcelProperty("接单时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptTime;

    @ExcelProperty("完成时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @ExcelProperty("验收时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptanceTime;

    @ExcelProperty("评价时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime evaluateTime;

    @ExcelProperty("维修备注")
    private String remark;

    @ExcelProperty("态度评分")
    private Integer evaluateAttitude;

    @ExcelProperty("速度评分")
    private Integer evaluateSpeed;

    @ExcelProperty("质量评分")
    private Integer evaluateQuality;

    @ExcelProperty("评价内容")
    private String evaluateComment;

    @ExcelProperty("是否超时")
    private String overdueText;

    public static WorkOrderExcelVO from(WorkOrder o, Dorm dorm, FaultType ft, User student, User teacher) {
        WorkOrderExcelVO vo = new WorkOrderExcelVO();
        vo.setOrderNo(o.getOrderNo());
        vo.setDormName(dorm != null ? dorm.getBuilding() + dorm.getRoom() : "宿舍" + o.getDormId());
        vo.setFaultTypeName(ft != null ? ft.getName() : "类型" + o.getFaultTypeId());
        vo.setDescription(o.getDescription());
        vo.setStatusText(statusToChinese(o.getStatus()));
        vo.setUrgentText(o.getIsUrgent() != null && o.getIsUrgent() == 1 ? "紧急" : "普通");
        vo.setStudentName(student != null ? student.getName() : "学生" + o.getStudentId());
        vo.setTeacherName(teacher != null ? teacher.getName() : (o.getTeacherId() != null ? "师傅" + o.getTeacherId() : "未分配"));
        vo.setSubmitTime(o.getSubmitTime());
        vo.setAcceptTime(o.getAcceptTime());
        vo.setCompleteTime(o.getCompleteTime());
        vo.setAcceptanceTime(o.getAcceptanceTime());
        vo.setEvaluateTime(o.getEvaluateTime());
        vo.setRemark(o.getRemark());
        vo.setEvaluateAttitude(o.getEvaluateAttitude());
        vo.setEvaluateSpeed(o.getEvaluateSpeed());
        vo.setEvaluateQuality(o.getEvaluateQuality());
        vo.setEvaluateComment(o.getEvaluateComment());
        vo.setOverdueText(o.getIsOverdue() != null && o.getIsOverdue() == 1 ? "是" : "否");
        return vo;
    }

    private static String statusToChinese(String s) {
        if (s == null) return "未知";
        switch (s) {
            case "pending": return "待处理";
            case "processing": return "处理中";
            case "pending_acceptance": return "待验收";
            case "accepted": return "已验收";
            case "completed": return "已完成";
            default: return s;
        }
    }
}
