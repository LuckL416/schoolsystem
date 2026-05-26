package com.school.dormrepair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.Notification;
import com.school.dormrepair.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 分页获取当前用户的通知列表
    @GetMapping("/list")
    public Result<Page<Notification>> list(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(notificationService.listByUser(userId, page, size));
    }

    // 获取未读通知数量
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestAttribute("userId") Long userId) {
        return Result.success(notificationService.unreadCount(userId));
    }

    // 标记单条通知为已读
    @PutMapping("/{id}/read")
    public Result<Void> markRead(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId
    ) {
        notificationService.markRead(id, userId);
        return Result.success();
    }

    // 全部标记为已读
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestAttribute("userId") Long userId) {
        notificationService.markAllRead(userId);
        return Result.success();
    }
}
