package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @PostMapping("/submit")
    public Result<String> submit(
            @RequestBody WorkOrder workOrder,
            @RequestAttribute("userId") Long userId
    ) {
        workOrder.setStudentId(userId);
        return workOrderService.submit(workOrder);
    }

    @GetMapping("/my")
    public Result<List<WorkOrder>> myOrders(
            @RequestAttribute("userId") Long userId
    ) {
        return workOrderService.listByStudent(userId);
    }

    @GetMapping("/all")
    public Result<List<WorkOrder>> allOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long dormId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return workOrderService.allList(status, dormId, startDate, endDate);
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

    @PostMapping("/evaluate/{orderId}")
    public Result<String> evaluate(
            @PathVariable Long orderId,
            @RequestParam Integer star
    ) {
        return workOrderService.evaluate(orderId, star);
    }
}
