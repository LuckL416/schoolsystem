package com.school.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.dormrepair.common.BusinessException;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.mapper.WorkOrderMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@Service
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final NotificationService notificationService;

    public WorkOrderService(WorkOrderMapper workOrderMapper, NotificationService notificationService) {
        this.workOrderMapper = workOrderMapper;
        this.notificationService = notificationService;
    }

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

    // 查询我的工单（学生，支持分页）
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkOrder>> listByStudent(Long studentId, Integer page, Integer size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkOrder> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getStudentId, studentId);
        wrapper.orderByDesc(WorkOrder::getSubmitTime);
        return Result.success(workOrderMapper.selectPage(p, wrapper));
    }

    // 查询所有工单（支持筛选+分页）
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkOrder>> allList(
            String status, Long dormId, String startDate, String endDate, Integer page, Integer size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkOrder> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page != null ? page : 1, size != null ? size : 10);
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
        return Result.success(workOrderMapper.selectPage(p, wrapper));
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
        order.setStatus("pending_acceptance");
        order.setCompleteTime(LocalDateTime.now());
        if (remark != null && !remark.isEmpty()) {
            order.setRemark(remark);
        }
        workOrderMapper.updateById(order);
        return Result.success("完工成功");
    }

    /** Student confirms acceptance — pending_acceptance -> accepted */
    public void acceptance(Long orderId, Long studentId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        if (!"pending_acceptance".equals(order.getStatus()))
            throw new BusinessException("当前状态不可验收");
        if (!order.getStudentId().equals(studentId))
            throw new BusinessException("只能验收自己的工单");

        WorkOrder update = new WorkOrder();
        update.setId(orderId);
        update.setStatus("accepted");
        update.setAcceptanceTime(LocalDateTime.now());
        workOrderMapper.updateById(update);
    }

    /** Student evaluates — 3 dimensions + comment. Bad rating (avg < 3) alerts admin */
    public void evaluate(Long orderId, Long studentId,
                         Integer attitude, Integer speed, Integer quality, String comment) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        if (!"accepted".equals(order.getStatus()))
            throw new BusinessException("请先确认验收后再评价");
        if (!order.getStudentId().equals(studentId))
            throw new BusinessException("只能评价自己的工单");
        if (attitude == null || speed == null || quality == null)
            throw new BusinessException("请完成所有维度评分");
        if (attitude < 1 || attitude > 5 || speed < 1 || speed > 5 || quality < 1 || quality > 5)
            throw new BusinessException("评分范围为1-5");

        WorkOrder update = new WorkOrder();
        update.setId(orderId);
        update.setEvaluateAttitude(attitude);
        update.setEvaluateSpeed(speed);
        update.setEvaluateQuality(quality);
        update.setEvaluateComment(comment);
        update.setEvaluateTime(LocalDateTime.now());
        workOrderMapper.updateById(update);

        // Bad rating check: average < 3 → alert admin (user id 1 = super admin)
        double avg = (attitude + speed + quality) / 3.0;
        if (avg < 3.0) {
            notificationService.send(1L, "bad_rating",
                "差评预警",
                "工单 " + order.getOrderNo() + " 收到差评（均分 " + String.format("%.1f", avg) + "），请关注处理",
                orderId);
        }
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
