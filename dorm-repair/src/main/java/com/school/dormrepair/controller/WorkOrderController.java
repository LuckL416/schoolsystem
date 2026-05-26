package com.school.dormrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.mapper.WorkOrderMapper;
import com.school.dormrepair.service.NotificationService;
import com.school.dormrepair.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/submit")
    public Result<String> submit(
            @RequestBody WorkOrder workOrder,
            @RequestAttribute("userId") Long userId
    ) {
        workOrder.setStudentId(userId);
        return workOrderService.submit(workOrder);
    }

    @GetMapping("/my")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkOrder>> myOrders(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return workOrderService.listByStudent(userId, page, size);
    }

    @GetMapping("/all")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkOrder>> allOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long dormId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return workOrderService.allList(status, dormId, startDate, endDate, page, size);
    }

    @PostMapping("/accept/{orderId}")
    public Result<String> accept(
            @PathVariable Long orderId,
            @RequestAttribute("userId") Long userId
    ) {
        return workOrderService.accept(orderId, userId);
    }

    @PostMapping("/complete/{orderId}")
    public Result<String> complete(
            @PathVariable Long orderId,
            @RequestParam(required = false) String remark
    ) {
        return workOrderService.complete(orderId, remark);
    }

    // 学生取消工单
    @PostMapping("/cancel/{orderId}")
    public Result<String> cancel(
            @PathVariable Long orderId,
            @RequestAttribute("userId") Long userId
    ) {
        return workOrderService.cancel(orderId, userId);
    }

    /** Student confirms acceptance of a completed repair */
    @PostMapping("/acceptance/{orderId}")
    public Result<?> acceptance(@PathVariable Long orderId,
                                @RequestAttribute("userId") Long userId) {
        workOrderService.acceptance(orderId, userId);
        return Result.success();
    }

    /** Student evaluates with 3 dimensions + optional comment */
    @PostMapping("/evaluate/{orderId}")
    public Result<?> evaluate(@PathVariable Long orderId,
                              @RequestParam Integer attitude,
                              @RequestParam Integer speed,
                              @RequestParam Integer quality,
                              @RequestParam(required = false) String comment,
                              @RequestAttribute("userId") Long userId) {
        workOrderService.evaluate(orderId, userId, attitude, speed, quality, comment);
        return Result.success();
    }

    /** Admin: list work orders with bad ratings (avg of 3 dimensions < 3) */
    @GetMapping("/bad-ratings")
    public Result<List<WorkOrder>> badRatings() {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(WorkOrder::getEvaluateAttitude)
          .orderByDesc(WorkOrder::getEvaluateTime);
        List<WorkOrder> all = workOrderMapper.selectList(qw);
        List<WorkOrder> bad = all.stream()
            .filter(o -> {
                double avg = (o.getEvaluateAttitude() + o.getEvaluateSpeed() + o.getEvaluateQuality()) / 3.0;
                return avg < 3.0;
            })
            .collect(Collectors.toList());
        return Result.success(bad);
    }

    /** Admin manually assigns order to a teacher */
    @PostMapping("/assign/{orderId}")
    public Result<?> assign(@PathVariable Long orderId, @RequestParam Long teacherId) {
        workOrderService.assign(orderId, teacherId);
        return Result.success();
    }

    /** Teacher claims an order from the pool */
    @PostMapping("/claim/{orderId}")
    public Result<?> claim(@PathVariable Long orderId, @RequestAttribute("userId") Long userId) {
        workOrderService.claim(orderId, userId);
        return Result.success();
    }

    /** Get unclaimed orders pool */
    @GetMapping("/pool")
    public Result<List<WorkOrder>> pool() {
        return Result.success(workOrderService.pool());
    }

    /** List all urgent uncompleted orders */
    @GetMapping("/urgent")
    public Result<List<WorkOrder>> urgentList() {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(WorkOrder::getIsUrgent, 1)
          .in(WorkOrder::getStatus, "pending", "processing")
          .orderByAsc(WorkOrder::getUrgentLevel)
          .orderByDesc(WorkOrder::getSubmitTime);
        return Result.success(workOrderMapper.selectList(qw));
    }

    /** List all overdue orders */
    @GetMapping("/overdue")
    public Result<List<WorkOrder>> overdueList() {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(WorkOrder::getIsOverdue, 1)
          .orderByDesc(WorkOrder::getSubmitTime);
        return Result.success(workOrderMapper.selectList(qw));
    }

    /** Manually notify assigned teacher about overdue order */
    @PostMapping("/overdue/notify/{orderId}")
    public Result<?> overdueNotify(@PathVariable Long orderId) {
        WorkOrder o = workOrderMapper.selectById(orderId);
        if (o != null && o.getAssignedTeacherId() != null) {
            notificationService.send(o.getAssignedTeacherId(), "overdue",
                "超时工单催促",
                "工单 " + o.getOrderNo() + " 已超时，请尽快处理",
                orderId);
        }
        return Result.success();
    }
}
