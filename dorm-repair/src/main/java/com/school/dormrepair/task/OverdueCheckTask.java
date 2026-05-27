package com.school.dormrepair.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.dormrepair.entity.*;
import com.school.dormrepair.mapper.*;
import com.school.dormrepair.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OverdueCheckTask {

    private final WorkOrderMapper workOrderMapper;
    private final FaultTypeMapper faultTypeMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public OverdueCheckTask(WorkOrderMapper wom, FaultTypeMapper ftm,
                            UserMapper um, NotificationService ns) {
        this.workOrderMapper = wom;
        this.faultTypeMapper = ftm;
        this.userMapper = um;
        this.notificationService = ns;
    }

    @Scheduled(fixedRate = 60000)
    public void checkOverdue() {
        // Auto-accept pending_acceptance orders stuck for >72 hours
        List<WorkOrder> unaccepted = workOrderMapper.selectList(
            new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getStatus, "pending_acceptance"));
        for (WorkOrder o : unaccepted) {
            if (o.getCompleteTime() != null
                && o.getCompleteTime().plusHours(72).isBefore(LocalDateTime.now())) {
                WorkOrder update = new WorkOrder();
                update.setId(o.getId());
                update.setStatus("accepted");
                update.setAcceptanceTime(LocalDateTime.now());
                workOrderMapper.updateById(update);
            }
        }

        // Existing overdue check
        List<WorkOrder> orders = workOrderMapper.selectList(
            new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, "pending", "processing")
                .eq(WorkOrder::getIsOverdue, 0));

        for (WorkOrder o : orders) {
            FaultType ft = faultTypeMapper.selectById(o.getFaultTypeId());
            if (ft == null || ft.getTimeoutHours() == null || ft.getTimeoutHours() == 0)
                continue;

            int timeoutHours = ft.getTimeoutHours();
            LocalDateTime checkTime = o.getAcceptTime() != null
                ? o.getAcceptTime() : o.getSubmitTime();

            if (checkTime.plusHours(timeoutHours).isBefore(LocalDateTime.now())) {
                WorkOrder update = new WorkOrder();
                update.setId(o.getId());
                update.setIsOverdue(1);
                workOrderMapper.updateById(update);

                // Notify all admins
                List<User> admins = userMapper.selectList(
                    new LambdaQueryWrapper<User>().eq(User::getRole, "admin"));
                for (User a : admins) {
                    notificationService.send(a.getId(), "overdue",
                        "超时工单提醒",
                        "工单 " + o.getOrderNo() + " 已超过处理时限 " + timeoutHours + " 小时",
                        o.getId());
                }
            }
        }
    }
}
