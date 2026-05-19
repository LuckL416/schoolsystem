package com.school.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WorkOrderService {

    @Autowired
    private WorkOrderMapper workOrderMapper;

    // 学生提交报修
    public Result<String> submit(WorkOrder workOrder) {
        workOrder.setStatus("pending");
        workOrder.setSubmitTime(LocalDateTime.now());
        // 生成日期编号：yyyyMMdd + 3位序号（当天自增）
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(WorkOrder::getOrderNo, datePrefix);
        wrapper.orderByDesc(WorkOrder::getOrderNo);
        wrapper.last("LIMIT 1");
        WorkOrder last = workOrderMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getOrderNo() != null && last.getOrderNo().length() == 11) {
            seq = Integer.parseInt(last.getOrderNo().substring(8)) + 1;
        }
        workOrder.setOrderNo(datePrefix + String.format("%03d", seq));
        workOrderMapper.insert(workOrder);
        return Result.success("报修提交成功");
    }

    // 学生取消工单（仅pending状态且本人）
    public Result<String> cancel(Long orderId, Long studentId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null || !"pending".equals(order.getStatus())) {
            return Result.error("工单不存在或状态不可取消");
        }
        if (!studentId.equals(order.getStudentId())) {
            return Result.error("只能取消自己的工单");
        }
        workOrderMapper.deleteById(orderId);
        return Result.success("取消成功");
    }

    // 查询我的工单（学生）
    public Result<List<WorkOrder>> listByStudent(Long studentId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getStudentId, studentId);
        wrapper.orderByDesc(WorkOrder::getSubmitTime);
        List<WorkOrder> list = workOrderMapper.selectList(wrapper);
        return Result.success(list);
    }

    // 查询所有工单（支持筛选）
    public Result<List<WorkOrder>> allList(String status, Long dormId, String startDate, String endDate) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WorkOrder::getStatus, status);
        }
        if (dormId != null) {
            wrapper.eq(WorkOrder::getDormId, dormId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(WorkOrder::getSubmitTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(WorkOrder::getSubmitTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(WorkOrder::getSubmitTime);
        List<WorkOrder> list = workOrderMapper.selectList(wrapper);
        return Result.success(list);
    }

    // 师傅接单
    public Result<String> accept(Long orderId, Long teacherId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null || !"pending".equals(order.getStatus())) {
            return Result.error("工单不存在或状态不可接单");
        }
        order.setStatus("processing");
        order.setTeacherId(teacherId);
        order.setAcceptTime(LocalDateTime.now());
        workOrderMapper.updateById(order);
        return Result.success("接单成功");
    }

    // 师傅完工
    public Result<String> complete(Long orderId, String remark) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null || !"processing".equals(order.getStatus())) {
            return Result.error("工单不存在或状态不可完工");
        }
        order.setStatus("completed");
        order.setCompleteTime(LocalDateTime.now());
        if (remark != null && !remark.isEmpty()) {
            order.setRemark(remark);
        }
        workOrderMapper.updateById(order);
        return Result.success("完工成功");
    }

    // 学生评价
    public Result<String> evaluate(Long orderId, Integer star) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null || !"completed".equals(order.getStatus())) {
            return Result.error("工单不存在或未完工，无法评价");
        }
        order.setEvaluateStar(star);
        workOrderMapper.updateById(order);
        return Result.success("评价成功");
    }
}
