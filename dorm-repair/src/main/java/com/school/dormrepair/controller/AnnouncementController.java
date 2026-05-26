package com.school.dormrepair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.Announcement;
import com.school.dormrepair.service.AnnouncementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * 分页查询公告列表，支持按分类筛选
     */
    @GetMapping("/list")
    public Result<Page<Announcement>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(announcementService.list(category, page, size));
    }

    /**
     * 查询单条公告详情
     */
    @GetMapping("/{id}")
    public Result<Announcement> detail(@PathVariable Long id) {
        return Result.success(announcementService.getById(id));
    }

    /**
     * 创建公告（管理端）
     */
    @PostMapping
    public Result<?> create(@RequestBody Announcement announcement,
                            @RequestAttribute("userId") Long userId) {
        announcement.setPublisherId(userId);
        announcementService.create(announcement);
        return Result.success();
    }

    /**
     * 更新公告（管理端）
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementService.update(announcement);
        return Result.success();
    }

    /**
     * 删除公告（管理端）
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }
}
