package com.school.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.entity.Announcement;
import com.school.dormrepair.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    /**
     * 分页查询公告列表，支持按分类筛选，置顶优先，最新在前
     */
    public Page<Announcement> list(String category, int page, int size) {
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            qw.eq(Announcement::getCategory, category);
        }
        qw.orderByDesc(Announcement::getIsPinned)
          .orderByDesc(Announcement::getCreateTime);
        return announcementMapper.selectPage(new Page<>(page, size), qw);
    }

    /**
     * 根据ID查询单条公告
     */
    public Announcement getById(Long id) {
        return announcementMapper.selectById(id);
    }

    /**
     * 创建公告
     */
    public void create(Announcement announcement) {
        announcement.setCreateTime(LocalDateTime.now());
        announcementMapper.insert(announcement);
    }

    /**
     * 更新公告
     */
    public void update(Announcement announcement) {
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
    }

    /**
     * 删除公告
     */
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }
}
