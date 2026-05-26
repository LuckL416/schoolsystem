# 宿舍报修系统 全面优化 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 7 new feature modules (acceptance+rating, emergency channel, auto-dispatch+bidding, overdue reminders, announcements+tips, inventory management, big-screen dashboard) + full-site UI redesign per DESIGN.md.

**Architecture:** Incremental on existing Spring Boot + MyBatis-Plus + Vue 3 CDN stack. Each phase adds entities/mappers/services/controllers on the backend and modifies/creates static HTML pages on the frontend. A shared notification center crosses all modules. Phases 0-7 run sequentially; each phase produces independently testable output.

**Tech Stack:** Spring Boot 3.2.4, MyBatis-Plus 3.5.7, MySQL 8.0, JWT (jjwt 0.11.5), EasyExcel 3.3.2, Vue 3 CDN, Axios CDN, Chart.js 4 CDN, Inter font (Google Fonts CDN)

---

## File Structure

### New Backend Files

| File | Responsibility |
|------|---------------|
| `entity/Notification.java` | 通知实体 |
| `entity/Announcement.java` | 公告实体 |
| `entity/InventoryItem.java` | 耗材实体 |
| `entity/InventoryRecord.java` | 出入库记录实体 |
| `mapper/NotificationMapper.java` | 通知Mapper |
| `mapper/AnnouncementMapper.java` | 公告Mapper |
| `mapper/InventoryItemMapper.java` | 耗材Mapper |
| `mapper/InventoryRecordMapper.java` | 出入库记录Mapper |
| `service/NotificationService.java` | 通知业务逻辑 |
| `service/AnnouncementService.java` | 公告业务逻辑 |
| `service/InventoryService.java` | 库存业务逻辑 |
| `service/SmsService.java` | 短信接口(预留) |
| `controller/NotificationController.java` | 通知API |
| `controller/AnnouncementController.java` | 公告API |
| `controller/InventoryController.java` | 库存API |
| `task/OverdueCheckTask.java` | 超时检测定时任务 |

### Modified Backend Files

| File | Changes |
|------|---------|
| `entity/WorkOrder.java` | 新增11个字段 + 多维度评价字段 |
| `entity/FaultType.java` | 新增 timeoutHours |
| `service/WorkOrderService.java` | 验收、评价、紧急判定、派单、抢单逻辑 |
| `service/AdminService.java` | 故障类型超时配置 |
| `controller/WorkOrderController.java` | 新增验收/评价/紧急/派单/抢单/超时端点 |
| `controller/StatsController.java` | 扩展看板统计 + 热力图 |
| `controller/AdminController.java` | 故障类型超时字段 |

### New Frontend Files

| File | Responsibility |
|------|---------------|
| `bigscreen.html` | 可视化大屏看板(全屏1920x1080, 30s刷新) |
| `announcement.html` | 公告列表+详情页(卡片布局+分类筛选) |
| `inventory.html` | 耗材库存管理(列表+出入库弹窗+记录) |

### Modified Frontend Files

| File | Changes |
|------|---------|
| `login.html` | 底部公告展示 + DESIGN.md样式 |
| `register.html` | DESIGN.md样式 |
| `student.html` | 验收按钮+评价弹窗+紧急标识+通知铃铛+DESIGN.md样式 |
| `teacher.html` | 抢单池标签+超时标记+紧急高亮+通知铃铛+DESIGN.md样式 |
| `index.html` | 升级统计卡片+热力图+超时预警+通知铃铛+DESIGN.md样式 |
| `admin.html` | 超时配置+公告管理入口+库存入口+DESIGN.md样式 |
| `api.js` | 新增API方法封装 |

---

## Phase 0: Foundation (Database + Notification + Global CSS)

### Task 0.1: Database Migration Script

**Files:**
- Create: `src/main/resources/db/migration.sql`

<details>
<summary>Full SQL (click to expand)</summary>

```sql
-- ============================================
-- Phase 0-4: ALTER existing tables
-- ============================================

-- work_order: new status values (MySQL enum not used, just expanding varchar comments)
-- work_order: acceptance & rating
ALTER TABLE work_order
    ADD COLUMN acceptance_time DATETIME NULL COMMENT '验收确认时间',
    ADD COLUMN evaluate_attitude INT NULL COMMENT '态度评分 1-5',
    ADD COLUMN evaluate_speed INT NULL COMMENT '速度评分 1-5',
    ADD COLUMN evaluate_quality INT NULL COMMENT '质量评分 1-5',
    ADD COLUMN evaluate_comment VARCHAR(200) NULL COMMENT '文字评价',
    ADD COLUMN evaluate_time DATETIME NULL COMMENT '评价时间',
    ADD COLUMN is_urgent TINYINT NOT NULL DEFAULT 0 COMMENT '是否紧急 0/1',
    ADD COLUMN urgent_level INT NULL COMMENT '紧急级别 1-3',
    ADD COLUMN assigned_teacher_id BIGINT NULL COMMENT '被分配师傅ID',
    ADD COLUMN assign_time DATETIME NULL COMMENT '分配/抢单时间',
    ADD COLUMN is_overdue TINYINT NOT NULL DEFAULT 0 COMMENT '是否超时 0/1';

-- fault_type: timeout config
ALTER TABLE fault_type
    ADD COLUMN timeout_hours INT NOT NULL DEFAULT 48 COMMENT '超时时限(小时)';

-- ============================================
-- Phase 5-6: New tables
-- ============================================

-- Notification center
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    type VARCHAR(30) NOT NULL COMMENT 'urgent/bad_rating/overdue/low_stock/assign/system',
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL DEFAULT '',
    related_id BIGINT NULL COMMENT '关联业务ID',
    is_read TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Announcements
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT 'notice/tip/general',
    is_pinned TINYINT NOT NULL DEFAULT 0,
    publisher_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL,
    INDEX idx_category_pinned (category, is_pinned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inventory items
CREATE TABLE IF NOT EXISTS inventory_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT '电工/水工/木工/五金',
    quantity INT NOT NULL DEFAULT 0,
    safety_threshold INT NOT NULL DEFAULT 10,
    unit VARCHAR(10) NOT NULL DEFAULT '个',
    update_time DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inventory records
CREATE TABLE IF NOT EXISTS inventory_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL COMMENT 'in/out',
    quantity INT NOT NULL,
    operator_id BIGINT NOT NULL,
    work_order_id BIGINT NULL,
    remark VARCHAR(100) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed data: maintenance tips
INSERT INTO announcement (title, content, category, is_pinned, publisher_id, create_time) VALUES
('宿舍跳闸恢复指南',
 '<p>跳闸后先检查宿舍总闸(通常在门后配电箱)，将所有开关全部下拉到底再推上去。若仍无法恢复，请逐个拔掉大功率电器(热水壶、电磁炉等)后再尝试。<strong>注意：切勿湿手触碰配电箱。</strong></p>',
 'tip', 0, 1, NOW()),
('空调滤网清洗教程',
 '<p>每学期至少清洗一次。<ol><li>打开空调面板</li><li>取出滤网</li><li>清水冲洗(勿用刷子)</li><li>阴凉处晾干</li><li>装回</li></ol>滤网堵塞会导致制冷差、耗电增加。</p>',
 'tip', 0, 1, NOW()),
('马桶/下水道轻微堵塞处理',
 '<p>先用皮搋子反复按压5-10次。无效可倒半杯洗洁精+一壶热水(非开水)，静置15分钟后冲水。<strong>不要将剩饭、卫生巾、湿厕纸丢入马桶。</strong></p>',
 'tip', 0, 1, NOW()),
('水龙头滴水简易处理',
 '<p>关闭水阀 → 用扳手拧开龙头帽 → 检查橡皮垫圈是否老化破裂 → 若损坏可报修更换。临时应急可在龙头下放桶接水避免浪费。</p>',
 'tip', 0, 1, NOW()),
('热水器不出热水排查',
 '<p>先确认电源指示灯是否亮起 → 检查温度设置是否被调至最低 → 查看进出水阀门是否完全打开。如以上均正常仍无热水，请报修。</p>',
 'tip', 0, 1, NOW()),
('窗户卡住/推拉不畅',
 '<p>检查滑轨是否有异物卡住 → 用湿布清理轨道灰尘 → 可涂抹少量蜡烛/肥皂润滑。勿用蛮力，以免玻璃破裂。</p>',
 'tip', 0, 1, NOW());

-- Update urgent fault types with shorter timeouts
UPDATE fault_type SET timeout_hours = 4 WHERE urgent_level = 1;
UPDATE fault_type SET timeout_hours = 24 WHERE urgent_level = 2;
```
</details>

- [ ] **Step 1:** Run the migration SQL

```bash
# Connect to MySQL and execute the script
mysql -u root -pLuoliang12 dorm_repair_system < src/main/resources/db/migration.sql
```

Expected: Tables altered, new tables created, seed data inserted. No errors.

- [ ] **Step 2:** Commit

```bash
git add src/main/resources/db/migration.sql
git commit -m "feat: add database migration for all 7 optimization modules"
```

### Task 0.2: Notification Entity + Mapper

**Files:**
- Create: `src/main/java/com/school/dormrepair/entity/Notification.java`
- Create: `src/main/java/com/school/dormrepair/mapper/NotificationMapper.java`

- [ ] **Step 1:** Write Notification entity

```java
package com.school.dormrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;     // urgent/bad_rating/overdue/low_stock/assign/system
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

- [ ] **Step 2:** Write NotificationMapper

```java
package com.school.dormrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.dormrepair.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
```

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/entity/Notification.java src/main/java/com/school/dormrepair/mapper/NotificationMapper.java
git commit -m "feat: add Notification entity and mapper"
```

### Task 0.3: Notification Service + Controller

**Files:**
- Create: `src/main/java/com/school/dormrepair/service/NotificationService.java`
- Create: `src/main/java/com/school/dormrepair/controller/NotificationController.java`

- [ ] **Step 1:** Write NotificationService

```java
package com.school.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.entity.Notification;
import com.school.dormrepair.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** Send a notification to one or more users */
    public void send(Long userId, String type, String title, String content, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /** Send to multiple users at once */
    public void sendToUsers(List<Long> userIds, String type, String title, String content, Long relatedId) {
        for (Long uid : userIds) {
            send(uid, type, title, content, relatedId);
        }
    }

    /** Get notifications for current user */
    public Page<Notification> listByUser(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId)
          .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(new Page<>(page, size), qw);
    }

    /** Unread count */
    public long unreadCount(Long userId) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId)
          .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(qw);
    }

    /** Mark one as read */
    public void markRead(Long id, Long userId) {
        Notification n = new Notification();
        n.setId(id);
        n.setIsRead(1);
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getId, id).eq(Notification::getUserId, userId);
        notificationMapper.update(n, qw);
    }

    /** Mark all as read */
    public void markAllRead(Long userId) {
        Notification n = new Notification();
        n.setIsRead(1);
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId).eq(Notification::getIsRead, 0);
        notificationMapper.update(n, qw);
    }
}
```

- [ ] **Step 2:** Write NotificationController

```java
package com.school.dormrepair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.Notification;
import com.school.dormrepair.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public Result<Page<Notification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(notificationService.listByUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(notificationService.unreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markRead(id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<?> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAllRead(userId);
        return Result.success();
    }
}
```

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/service/NotificationService.java src/main/java/com/school/dormrepair/controller/NotificationController.java
git commit -m "feat: add Notification service and controller"
```

### Task 0.4: Global CSS + api.js Update

**Files:**
- Create: `src/main/resources/static/design-system.css`
- Modify: `api.js`

- [ ] **Step 1:** Create global design system CSS

Create `src/main/resources/static/design-system.css`:

```css
/* ============================================
   DESIGN.md Implementation — Dorm Repair System
   Clean white-canvas + campus-blue accent
   ============================================ */

/* --- Font (Inter via Google Fonts CDN) --- */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

/* --- CSS Variables --- */
:root {
  /* Brand */
  --primary: #2563eb;
  --primary-hover: #1d4ed8;
  --primary-soft: #dbeafe;
  --primary-pressed: #1e40af;

  /* Semantic */
  --success: #16a34a;
  --success-soft: #dcfce7;
  --warning: #d97706;
  --warning-soft: #fef3c7;
  --danger: #dc2626;
  --danger-soft: #fee2e2;
  --info: #0891b2;
  --info-soft: #cffafe;

  /* Surface */
  --canvas: #ffffff;
  --surface: #f8fafc;
  --surface-alt: #f1f5f9;
  --surface-hover: #e2e8f0;

  /* Border */
  --border: #e2e8f0;
  --border-strong: #cbd5e1;

  /* Text */
  --ink: #0f172a;
  --ink-secondary: #475569;
  --ink-muted: #94a3b8;

  /* Spacing */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 12px;
  --spacing-lg: 16px;
  --spacing-xl: 20px;
  --spacing-xxl: 24px;
  --spacing-section: 32px;

  /* Rounded */
  --rounded-sm: 4px;
  --rounded-md: 6px;
  --rounded-lg: 10px;
  --rounded-xl: 14px;
  --rounded-full: 9999px;

  /* Shadow */
  --shadow-card: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-dropdown: 0 4px 16px rgba(0,0,0,0.08);
  --shadow-modal: 0 8px 32px rgba(0,0,0,0.12);

  /* Typography */
  --font-display-lg: 700 32px/1.2 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-display-md: 600 24px/1.3 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-heading: 600 20px/1.35 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-subhead: 600 16px/1.4 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-body: 400 15px/1.5 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-body-sm: 400 13px/1.45 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-caption: 400 12px/1.4 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-button: 500 14px/1.2 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* --- Reset --- */
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font: var(--font-body);
  color: var(--ink);
  background: var(--surface);
  -webkit-font-smoothing: antialiased;
}

/* --- Typography --- */
h1 { font: var(--font-display-lg); color: var(--ink); }
h2 { font: var(--font-display-md); color: var(--ink); }
h3 { font: var(--font-heading); color: var(--ink); }

a { color: var(--primary); text-decoration: none; }
a:hover { color: var(--primary-hover); }

/* --- Buttons --- */
.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  font: var(--font-button); border: none; border-radius: var(--rounded-md);
  padding: 8px 16px; cursor: pointer; transition: all 150ms ease;
  white-space: nowrap;
}
.btn:focus-visible { outline: 3px solid var(--primary-soft); outline-offset: 1px; }

.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-hover); }
.btn-primary:active { background: var(--primary-pressed); }

.btn-secondary { background: var(--canvas); color: var(--ink); border: 1px solid var(--border-strong); }
.btn-secondary:hover { background: var(--surface); }

.btn-danger { background: var(--danger); color: #fff; }
.btn-danger:hover { opacity: 0.9; }

.btn-ghost { background: transparent; color: var(--ink-secondary); }
.btn-ghost:hover { background: var(--surface); }

.btn-sm { padding: 6px 12px; font-size: 13px; }
.btn-lg { padding: 10px 20px; font-size: 15px; }

/* --- Inputs --- */
.input {
  font: var(--font-body); color: var(--ink);
  background: var(--canvas); border: 1px solid var(--border);
  border-radius: var(--rounded-sm); padding: 8px 12px;
  transition: border-color 150ms ease, box-shadow 150ms ease;
  width: 100%;
}
.input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.input::placeholder { color: var(--ink-muted); }

select.input { cursor: pointer; }

/* --- Cards --- */
.card {
  background: var(--canvas); border: 1px solid var(--border);
  border-radius: var(--rounded-lg); padding: var(--spacing-xl);
  box-shadow: var(--shadow-card);
}
.card:hover { border-color: var(--border-strong); }

/* --- Tables --- */
.table-wrap { overflow-x: auto; border-radius: var(--rounded-lg); border: 1px solid var(--border); }

table { width: 100%; border-collapse: collapse; font: var(--font-body-sm); }

th {
  font-weight: 600; color: var(--ink-secondary); text-align: left;
  padding: 10px 14px; background: var(--surface);
  border-bottom: 1px solid var(--border);
}

td { padding: 10px 14px; border-bottom: 1px solid var(--border); }

tr:last-child td { border-bottom: none; }
tr:hover td { background: var(--surface-alt); }

/* --- Status Pills --- */
.pill {
  display: inline-flex; align-items: center;
  font: var(--font-caption); font-weight: 500;
  padding: 2px 10px; border-radius: var(--rounded-full);
  white-space: nowrap;
}
.pill-pending { background: var(--warning-soft); color: var(--warning); }
.pill-processing { background: var(--info-soft); color: var(--info); }
.pill-completed, .pill-accepted { background: var(--success-soft); color: var(--success); }
.pill-pending_acceptance { background: var(--primary-soft); color: var(--primary); }
.pill-urgent { background: var(--danger-soft); color: var(--danger); }

/* --- Top Nav Bar --- */
.top-nav {
  display: flex; align-items: center; justify-content: space-between;
  height: 56px; padding: 0 var(--spacing-xl);
  background: var(--canvas); border-bottom: 1px solid var(--border);
  position: sticky; top: 0; z-index: 100;
}
.top-nav-left { display: flex; align-items: center; gap: var(--spacing-md); }
.top-nav-right { display: flex; align-items: center; gap: var(--spacing-lg); }
.top-nav-title { font: var(--font-subhead); color: var(--ink); }

/* Badge */
.badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px;
  font: var(--font-caption); font-weight: 600;
  background: var(--danger); color: #fff;
  border-radius: var(--rounded-full);
}

/* --- Modal --- */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(15,23,42,0.4);
  display: flex; align-items: center; justify-content: center;
  z-index: 200; animation: fadeIn 150ms ease;
}
.modal {
  background: var(--canvas); border-radius: var(--rounded-xl);
  box-shadow: var(--shadow-modal); padding: var(--spacing-xxl);
  min-width: 400px; max-width: 90vw; max-height: 80vh; overflow-y: auto;
}
.modal-title { font: var(--font-heading); margin-bottom: var(--spacing-lg); }
.modal-actions { display: flex; justify-content: flex-end; gap: var(--spacing-sm); margin-top: var(--spacing-xl); }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

/* --- Notification Bell --- */
.notif-bell {
  position: relative; cursor: pointer; padding: 4px;
  background: none; border: none; font-size: 20px; line-height: 1;
}
.notif-bell .badge {
  position: absolute; top: -2px; right: -4px;
}

.notif-dropdown {
  position: absolute; top: 48px; right: 0;
  width: 360px; max-height: 400px; overflow-y: auto;
  background: var(--canvas); border: 1px solid var(--border);
  border-radius: var(--rounded-lg); box-shadow: var(--shadow-dropdown);
  z-index: 150;
}
.notif-item {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border);
  cursor: pointer; transition: background 100ms;
}
.notif-item:hover { background: var(--surface-alt); }
.notif-item.unread { border-left: 3px solid var(--primary); }
.notif-item-title { font-weight: 600; font-size: 13px; }
.notif-item-content { font-size: 12px; color: var(--ink-secondary); margin-top: 2px; }
.notif-item-time { font-size: 11px; color: var(--ink-muted); margin-top: 4px; }

/* --- Stat Cards --- */
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: var(--spacing-lg); }
.stat-card {
  background: var(--canvas); border: 1px solid var(--border);
  border-radius: var(--rounded-lg); padding: var(--spacing-xl);
  box-shadow: var(--shadow-card);
}
.stat-card .stat-icon { font-size: 28px; margin-bottom: var(--spacing-sm); }
.stat-card .stat-value { font: var(--font-display-md); color: var(--ink); }
.stat-card .stat-label { font: var(--font-body-sm); color: var(--ink-muted); margin-top: 4px; }

/* --- Layout --- */
.page-wrap { max-width: 1280px; margin: 0 auto; padding: var(--spacing-section) var(--spacing-xl); }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-section); }
.page-title { font: var(--font-display-md); }

.section { margin-bottom: var(--spacing-section); }
.section-title { font: var(--font-heading); margin-bottom: var(--spacing-lg); }

/* --- Flex/Grid helpers --- */
.flex { display: flex; }
.flex-col { flex-direction: column; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.gap-sm { gap: var(--spacing-sm); }
.gap-md { gap: var(--spacing-md); }
.gap-lg { gap: var(--spacing-lg); }

/* --- Urgent highlight --- */
.urgent-badge {
  display: inline-flex; align-items: center; gap: 4px;
  font-weight: 600; font-size: 12px; color: var(--danger);
  animation: blink 1s ease-in-out infinite;
}
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

/* --- Toast --- */
.toast-container { position: fixed; top: 20px; right: 20px; z-index: 300; display: flex; flex-direction: column; gap: 8px; }
.toast {
  padding: 12px 20px; border-radius: var(--rounded-md);
  font: var(--font-body-sm); font-weight: 500;
  box-shadow: var(--shadow-dropdown);
  animation: slideIn 200ms ease;
}
.toast-success { background: var(--success); color: #fff; }
.toast-error { background: var(--danger); color: #fff; }
.toast-warning { background: var(--warning); color: #fff; }
@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }

/* --- Tabs --- */
.tabs { display: flex; gap: 0; border-bottom: 2px solid var(--border); margin-bottom: var(--spacing-lg); }
.tab {
  padding: 10px 20px; font: var(--font-button); color: var(--ink-secondary);
  background: none; border: none; cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -2px;
  transition: color 150ms, border-color 150ms;
}
.tab:hover { color: var(--ink); }
.tab.active { color: var(--primary); border-bottom-color: var(--primary); }

/* --- Heatmap --- */
.heatmap-cell {
  width: 100%; aspect-ratio: 1;
  border-radius: var(--rounded-sm);
  transition: transform 100ms;
}
.heatmap-cell:hover { transform: scale(1.15); }

/* --- Rating stars --- */
.star-rating { display: inline-flex; gap: 2px; }
.star { font-size: 20px; cursor: pointer; color: var(--border); transition: color 100ms; }
.star.active { color: #f59e0b; }
.star:hover { color: #f59e0b; }

/* --- Responsive --- */
@media (max-width: 768px) {
  .page-wrap { padding: var(--spacing-lg); }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .top-nav { padding: 0 var(--spacing-md); }
  .modal { min-width: auto; width: 95vw; }
}
```

- [ ] **Step 2:** Update `src/main/resources/static/api.js` — add new API methods

Append to api.js:

```javascript
// ========== Notification APIs ==========
api.notification = {
  list: (page = 1, size = 20) => api.get('/notification/list', { params: { page, size } }),
  unreadCount: () => api.get('/notification/unread-count'),
  markRead: (id) => api.put(`/notification/${id}/read`),
  markAllRead: () => api.put('/notification/read-all'),
};

// ========== Announcement APIs ==========
api.announcement = {
  list: (params = {}) => api.get('/announcement/list', { params }),
  detail: (id) => api.get(`/announcement/${id}`),
  create: (data) => api.post('/announcement', data),
  update: (id, data) => api.put(`/announcement/${id}`, data),
  delete: (id) => api.delete(`/announcement/${id}`),
};

// ========== Inventory APIs ==========
api.inventory = {
  list: (params = {}) => api.get('/inventory/list', { params }),
  create: (data) => api.post('/inventory', data),
  update: (id, data) => api.put(`/inventory/${id}`, data),
  stockIn: (id, data) => api.post(`/inventory/${id}/in`, data),
  stockOut: (id, data) => api.post(`/inventory/${id}/out`, data),
  records: (id, params = {}) => api.get(`/inventory/${id}/records`, { params }),
  lowStock: () => api.get('/inventory/low-stock'),
};
```

- [ ] **Step 3:** Commit

```bash
git add src/main/resources/static/design-system.css src/main/resources/static/api.js
git commit -m "feat: add global design system CSS and expand api.js"
```

---

## Phase 1: Module 1 — Online Acceptance + Rating

### Task 1.1: Update WorkOrder Entity

**Files:**
- Modify: `entity/WorkOrder.java`

- [ ] **Step 1:** Add new fields to WorkOrder — append after existing `remark` field:

```java
// New fields — Module 1: Acceptance & Rating
private LocalDateTime acceptanceTime;
private Integer evaluateAttitude;
private Integer evaluateSpeed;
private Integer evaluateQuality;
private String evaluateComment;
private LocalDateTime evaluateTime;
```

- [ ] **Step 2:** Commit

```bash
git add src/main/java/com/school/dormrepair/entity/WorkOrder.java
git commit -m "feat: add acceptance and multi-dimension rating fields to WorkOrder"
```

### Task 1.2: Update WorkOrderService

**Files:**
- Modify: `service/WorkOrderService.java`

- [ ] **Step 1:** Add `acceptance()` and replace existing `evaluate()` method in WorkOrderService.

Add after existing `complete()` method:

```java
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

/** Student evaluates a completed+accepted order — 3 dimensions + comment */
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

    // Bad rating check: average < 3 → alert admin
    double avg = (attitude + speed + quality) / 3.0;
    if (avg < 3.0) {
        notificationService.send(1L, "bad_rating",
            "差评预警",
            "工单 " + order.getOrderNo() + " 收到差评（均分 " + String.format("%.1f", avg) + "），请关注处理",
            orderId);
    }
}
```

- [ ] **Step 2:** Inject NotificationService into WorkOrderService — add to constructor:

```java
private final NotificationService notificationService;

public WorkOrderService(WorkOrderMapper workOrderMapper, NotificationService notificationService) {
    this.workOrderMapper = workOrderMapper;
    this.notificationService = notificationService;
}
```

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/service/WorkOrderService.java
git commit -m "feat: add acceptance and multi-dimension evaluation in WorkOrderService"
```

### Task 1.3: Update WorkOrderController

**Files:**
- Modify: `controller/WorkOrderController.java`

- [ ] **Step 1:** Add acceptance endpoint and modify evaluate endpoint:

```java
/** Student confirms acceptance */
@PostMapping("/acceptance/{orderId}")
public Result<?> acceptance(@PathVariable Long orderId, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    workOrderService.acceptance(orderId, userId);
    return Result.success();
}

/** Student evaluates with 3 dimensions + comment */
@PostMapping("/evaluate/{orderId}")
public Result<?> evaluate(@PathVariable Long orderId,
                          @RequestParam Integer attitude,
                          @RequestParam Integer speed,
                          @RequestParam Integer quality,
                          @RequestParam(required = false) String comment,
                          HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    workOrderService.evaluate(orderId, userId, attitude, speed, quality, comment);
    return Result.success();
}

/** Admin: list bad ratings (avg < 3) */
@GetMapping("/bad-ratings")
public Result<?> badRatings() {
    // Use LambdaQueryWrapper to find orders where ratings exist and avg < 3
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.isNotNull(WorkOrder::getEvaluateAttitude)
      .gt(WorkOrder::getId, 0)
      .orderByDesc(WorkOrder::getEvaluateTime);
    List<WorkOrder> all = workOrderMapper.selectList(qw);
    List<WorkOrder> bad = all.stream()
        .filter(o -> (o.getEvaluateAttitude() + o.getEvaluateSpeed() + o.getEvaluateQuality()) / 3.0 < 3.0)
        .collect(Collectors.toList());
    return Result.success(bad);
}
```

- [ ] **Step 2:** Remove or comment out the old single-star evaluate endpoint if it exists (replaced by multi-dimension).

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/controller/WorkOrderController.java
git commit -m "feat: add acceptance and multi-dimension evaluation endpoints"
```

### Task 1.4: Update student.html — Acceptance + Rating

**Files:**
- Modify: `student.html`

Key changes (integrate into existing Vue app):

- [ ] **Step 1:** In the work order list table, update the action column logic:

```html
<!-- In the order table row, status-based actions: -->
<template v-if="order.status === 'pending_acceptance'">
  <button class="btn btn-primary btn-sm" @click="confirmAcceptance(order)">
    确认验收
  </button>
</template>
<template v-else-if="order.status === 'accepted' && !order.evaluateAttitude">
  <button class="btn btn-primary btn-sm" @click="openEvaluate(order)">
    评价打分
  </button>
</template>
<template v-else-if="order.evaluateAttitude">
  <span class="pill pill-completed">已评价</span>
  <span style="font-size:12px;color:var(--ink-muted);margin-left:4px">
    态度{{ order.evaluateAttitude }} 速度{{ order.evaluateSpeed }} 质量{{ order.evaluateQuality }}
  </span>
</template>
```

- [ ] **Step 2:** Add evaluation modal and methods:

```html
<!-- Evaluation Modal -->
<div class="modal-overlay" v-if="showEvaluate" @click.self="showEvaluate=false">
  <div class="modal">
    <div class="modal-title">评价工单</div>
    <div style="margin-bottom:16px">
      <div style="margin-bottom:8px;font-weight:500">服务态度</div>
      <div class="star-rating">
        <span v-for="s in 5" :key="s" class="star"
              :class="{ active: evalForm.attitude >= s }"
              @click="evalForm.attitude = s">★</span>
      </div>
    </div>
    <div style="margin-bottom:16px">
      <div style="margin-bottom:8px;font-weight:500">维修速度</div>
      <div class="star-rating">
        <span v-for="s in 5" :key="s" class="star"
              :class="{ active: evalForm.speed >= s }"
              @click="evalForm.speed = s">★</span>
      </div>
    </div>
    <div style="margin-bottom:16px">
      <div style="margin-bottom:8px;font-weight:500">维修质量</div>
      <div class="star-rating">
        <span v-for="s in 5" :key="s" class="star"
              :class="{ active: evalForm.quality >= s }"
              @click="evalForm.quality = s">★</span>
      </div>
    </div>
    <div style="margin-bottom:16px">
      <div style="margin-bottom:8px;font-weight:500">文字评价 (选填)</div>
      <textarea class="input" v-model="evalForm.comment" rows="3"
                maxlength="200" placeholder="说说你的感受..."></textarea>
    </div>
    <div class="modal-actions">
      <button class="btn btn-secondary" @click="showEvaluate=false">取消</button>
      <button class="btn btn-primary" @click="submitEvaluate" :disabled="!canEvaluate">
        提交评价
      </button>
    </div>
  </div>
</div>
```

- [ ] **Step 3:** Add Vue data and methods:

```javascript
// In data():
evalForm: { attitude: 0, speed: 0, quality: 0, comment: '', orderId: null },
showEvaluate: false,

// Computed:
canEvaluate() {
  return this.evalForm.attitude > 0 && this.evalForm.speed > 0 && this.evalForm.quality > 0;
},

// Methods:
confirmAcceptance(order) {
  if (!confirm('确认该工单已维修完成？')) return;
  api.post(`/order/acceptance/${order.id}`).then(() => {
    this.loadOrders();
    this.showToast('验收成功，请评价', 'success');
  });
},
openEvaluate(order) {
  this.evalForm = { attitude: 0, speed: 0, quality: 0, comment: '', orderId: order.id };
  this.showEvaluate = true;
},
submitEvaluate() {
  api.post(`/order/evaluate/${this.evalForm.orderId}`, null, {
    params: {
      attitude: this.evalForm.attitude,
      speed: this.evalForm.speed,
      quality: this.evalForm.quality,
      comment: this.evalForm.comment,
    }
  }).then(() => {
    this.showEvaluate = false;
    this.loadOrders();
    this.showToast('评价提交成功', 'success');
  });
},
```

- [ ] **Step 4:** Swap old CSS/JS to use `design-system.css`. Add `<link rel="stylesheet" href="design-system.css">` in `<head>`. Remove old inline styles that conflict.

- [ ] **Step 5:** Commit

```bash
git add student.html
git commit -m "feat: add acceptance confirmation and multi-dimension rating to student page"
```

---

---

## Phase 2: Module 2 — Emergency Green Channel

### Task 2.1: Extend WorkOrder Entity (Emergency Fields)

**Files:**
- Modify: `entity/WorkOrder.java`

- [ ] **Step 1:** Add emergency fields to WorkOrder:

```java
// Module 2: Emergency
private Integer isUrgent;    // 0/1
private Integer urgentLevel; // 1-3 (from fault_type)
```

- [ ] **Step 2:** Commit

```bash
git add src/main/java/com/school/dormrepair/entity/WorkOrder.java
git commit -m "feat: add emergency fields to WorkOrder entity"
```

### Task 2.2: Auto-Detect Urgency on Submit

**Files:**
- Modify: `service/WorkOrderService.java`

- [ ] **Step 1:** Update `submit()` to auto-detect urgency and push notification:

```java
public void submit(WorkOrder order) {
    // ... existing orderNo generation and insert ...

    // Module 2: Check fault type urgency
    FaultType ft = faultTypeMapper.selectById(order.getFaultTypeId());
    if (ft != null && ft.getUrgentLevel() != null && ft.getUrgentLevel() <= 2) {
        order.setIsUrgent(1);
        order.setUrgentLevel(ft.getUrgentLevel());
        workOrderMapper.updateById(order);

        // Push urgent notification to all teachers and admins
        List<User> teachers = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getRole, "teacher"));
        List<User> admins = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getRole, "admin"));
        for (User t : teachers) {
            notificationService.send(t.getId(), "urgent",
                "紧急工单",
                "工单 " + order.getOrderNo() + " (" + ft.getName() + ") 需要紧急处理",
                order.getId());
        }
        for (User a : admins) {
            notificationService.send(a.getId(), "urgent",
                "紧急工单提醒",
                "工单 " + order.getOrderNo() + " (" + ft.getName() + ") 已提交，请关注",
                order.getId());
        }
    }
}
```

- [ ] **Step 2:** Inject `FaultTypeMapper` and `UserMapper` into WorkOrderService constructor.

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/service/WorkOrderService.java
git commit -m "feat: auto-detect urgent work orders and push notifications"
```

### Task 2.3: Urgent Orders API + Query

**Files:**
- Modify: `controller/WorkOrderController.java`

- [ ] **Step 1:** Add urgent orders endpoint:

```java
@GetMapping("/urgent")
public Result<?> urgentList() {
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.eq(WorkOrder::getIsUrgent, 1)
      .in(WorkOrder::getStatus, "pending", "processing")
      .orderByAsc(WorkOrder::getUrgentLevel)
      .orderByDesc(WorkOrder::getSubmitTime);
    return Result.success(workOrderMapper.selectList(qw));
}
```

- [ ] **Step 2:** Commit

```bash
git add src/main/java/com/school/dormrepair/controller/WorkOrderController.java
git commit -m "feat: add urgent orders list endpoint"
```

### Task 2.4: Frontend — Urgent Indicators

**Files:**
- Modify: `student.html`, `teacher.html`, `index.html`

- [ ] **Step 1:** student.html — when selecting fault type with urgentLevel ≤ 2, show red warning icon next to the dropdown.

- [ ] **Step 2:** teacher.html — urgent orders get red left border and float to top. Add `v-if="order.isUrgent"` check with `urgent-badge` CSS class.

- [ ] **Step 3:** index.html — urgent orders highlighted in list.

- [ ] **Step 4:** Commit

```bash
git add student.html teacher.html index.html
git commit -m "feat: add urgent indicators on all role pages"
```

---

## Phase 3: Module 3 — Auto-Dispatch + Bidding

### Task 3.1: Extend WorkOrder Entity + FaultType Entity

**Files:**
- Modify: `entity/WorkOrder.java`, `entity/FaultType.java`

- [ ] **Step 1:** Add dispatch fields to WorkOrder:

```java
// Module 3: Auto-dispatch & bidding
private Long assignedTeacherId;
private LocalDateTime assignTime;
```

- [ ] **Step 2:** Add timeout field to FaultType:

```java
// Module 4: Overdue timeout
private Integer timeoutHours;
```

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/entity/WorkOrder.java src/main/java/com/school/dormrepair/entity/FaultType.java
git commit -m "feat: add assign and timeout fields to entities"
```

### Task 3.2: Auto-Dispatch Logic in WorkOrderService

**Files:**
- Modify: `service/WorkOrderService.java`

- [ ] **Step 1:** Add auto-assign method and bidding/claim methods:

```java
/** Auto-assign urgent order to random teacher of matching workType */
private void autoAssign(WorkOrder order, String workType) {
    LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
    qw.eq(User::getRole, "teacher");
    List<User> teachers = userMapper.selectList(qw);
    if (teachers.isEmpty()) return;

    // Simple random assignment
    User assigned = teachers.get(new Random().nextInt(teachers.size()));
    WorkOrder update = new WorkOrder();
    update.setId(order.getId());
    update.setAssignedTeacherId(assigned.getId());
    update.setAssignTime(LocalDateTime.now());
    workOrderMapper.updateById(update);

    notificationService.send(assigned.getId(), "assign",
        "新工单分配",
        "工单 " + order.getOrderNo() + " 已分配给您，请及时处理",
        order.getId());
}

/** Teacher claims an order from pool */
public void claim(Long orderId, Long teacherId) {
    WorkOrder order = workOrderMapper.selectById(orderId);
    if (order == null) throw new BusinessException("工单不存在");
    if (!"pending".equals(order.getStatus())) throw new BusinessException("工单不可抢");
    if (order.getAssignedTeacherId() != null) throw new BusinessException("该工单已被分配");

    WorkOrder update = new WorkOrder();
    update.setId(orderId);
    update.setAssignedTeacherId(teacherId);
    update.setAssignTime(LocalDateTime.now());
    workOrderMapper.updateById(update);
}

/** Admin manually assigns order to a specific teacher */
public void assign(Long orderId, Long teacherId) {
    WorkOrder order = workOrderMapper.selectById(orderId);
    if (order == null) throw new BusinessException("工单不存在");

    WorkOrder update = new WorkOrder();
    update.setId(orderId);
    update.setAssignedTeacherId(teacherId);
    update.setAssignTime(LocalDateTime.now());
    workOrderMapper.updateById(update);

    notificationService.send(teacherId, "assign",
        "工单分配",
        "管理员已将工单 " + order.getOrderNo() + " 分配给您",
        orderId);
}

/** Unclaimed orders pool */
public List<WorkOrder> pool() {
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.eq(WorkOrder::getStatus, "pending")
      .isNull(WorkOrder::getAssignedTeacherId)
      .eq(WorkOrder::getIsUrgent, 0)
      .orderByDesc(WorkOrder::getSubmitTime);
    return workOrderMapper.selectList(qw);
}
```

- [ ] **Step 2:** Call `autoAssign(order, ft.getWorkType())` in `submit()` for urgent orders (urgentLevel ≤ 2).

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/service/WorkOrderService.java
git commit -m "feat: add auto-dispatch, claim, and pool logic"
```

### Task 3.3: Dispatch/Bidding Endpoints

**Files:**
- Modify: `controller/WorkOrderController.java`

- [ ] **Step 1:** Add endpoints:

```java
@PostMapping("/assign/{orderId}")
public Result<?> assign(@PathVariable Long orderId, @RequestParam Long teacherId) {
    workOrderService.assign(orderId, teacherId);
    return Result.success();
}

@PostMapping("/claim/{orderId}")
public Result<?> claim(@PathVariable Long orderId, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    workOrderService.claim(orderId, userId);
    return Result.success();
}

@GetMapping("/pool")
public Result<?> pool() {
    return Result.success(workOrderService.pool());
}
```

- [ ] **Step 2:** Commit

```bash
git add src/main/java/com/school/dormrepair/controller/WorkOrderController.java
git commit -m "feat: add assign, claim, and pool endpoints"
```

### Task 3.4: Frontend — Teacher Bidding Pool + Admin Assign

**Files:**
- Modify: `teacher.html`, `index.html`

- [ ] **Step 1:** teacher.html — add "工单池" tab showing unclaimed orders with "抢单" button. Add "我的工单" tab split by assigned/claimed.

- [ ] **Step 2:** index.html — admin can see assigned teacher and reassign via dropdown.

- [ ] **Step 3:** Commit

```bash
git add teacher.html index.html
git commit -m "feat: add teacher bidding pool and admin assign UI"
```

---

## Phase 4: Module 4 — Overdue Auto-Reminders

### Task 4.1: Update FaultType Admin Management

**Files:**
- Modify: `service/AdminService.java`

- [ ] **Step 1:** Ensure `addFaultType()` and `updateFaultType()` handle the new `timeoutHours` field (already part of FaultType entity since Task 3.1).

- [ ] **Step 2:** Commit

```bash
git add src/main/java/com/school/dormrepair/service/AdminService.java
git commit -m "feat: support timeoutHours in fault type CRUD"
```

### Task 4.2: Overdue Check Scheduled Task

**Files:**
- Create: `task/OverdueCheckTask.java`

- [ ] **Step 1:** Add `@EnableScheduling` to `DormRepairApplication.java`:

```java
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableScheduling
public class DormRepairApplication { ... }
```

- [ ] **Step 2:** Create `task/OverdueCheckTask.java`:

```java
package com.school.dormrepair.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.dormrepair.entity.*;
import com.school.dormrepair.mapper.*;
import com.school.dormrepair.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class OverdueCheckTask {

    private final WorkOrderMapper workOrderMapper;
    private final FaultTypeMapper faultTypeMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public OverdueCheckTask(WorkOrderMapper wom, FaultTypeMapper ftm,
                            UserMapper um, NotificationService ns) {
        this.workOrderMapper = wom; this.faultTypeMapper = ftm;
        this.userMapper = um; this.notificationService = ns;
    }

    @Scheduled(fixedRate = 60000)
    public void checkOverdue() {
        // Find all uncompleted, not-yet-marked-overdue orders
        List<WorkOrder> orders = workOrderMapper.selectList(
            new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, "pending", "processing")
                .eq(WorkOrder::getIsOverdue, 0));

        for (WorkOrder o : orders) {
            FaultType ft = faultTypeMapper.selectById(o.getFaultTypeId());
            if (ft == null || ft.getTimeoutHours() == null) continue;
            int timeoutHours = ft.getTimeoutHours();

            LocalDateTime checkTime = o.getAcceptTime() != null
                ? o.getAcceptTime() : o.getSubmitTime();
            if (checkTime.plusHours(timeoutHours).isBefore(LocalDateTime.now())) {
                // Mark overdue
                WorkOrder update = new WorkOrder();
                update.setId(o.getId());
                update.setIsOverdue(1);
                workOrderMapper.updateById(update);

                // Notify admins
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
```

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/task/OverdueCheckTask.java
git add src/main/java/com/school/dormrepair/DormRepairApplication.java
git commit -m "feat: add overdue check scheduled task"
```

### Task 4.3: Overdue API + Frontend

**Files:**
- Modify: `controller/WorkOrderController.java`, `teacher.html`, `index.html`

- [ ] **Step 1:** Add overdue list endpoint:

```java
@GetMapping("/overdue")
public Result<?> overdueList() {
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.eq(WorkOrder::getIsOverdue, 1).orderByDesc(WorkOrder::getSubmitTime);
    return Result.success(workOrderMapper.selectList(qw));
}

@PostMapping("/overdue/notify/{orderId}")
public Result<?> overdueNotify(@PathVariable Long orderId) {
    WorkOrder o = workOrderMapper.selectById(orderId);
    if (o != null && o.getAssignedTeacherId() != null) {
        notificationService.send(o.getAssignedTeacherId(), "overdue",
            "超时工单催促", "工单 " + o.getOrderNo() + " 已超时，请尽快处理", orderId);
    }
    return Result.success();
}
```

- [ ] **Step 2:** teacher.html — overdue orders show red marker. index.html — overdue count in stat cards.

- [ ] **Step 3:** Commit

```bash
git add src/main/java/com/school/dormrepair/controller/WorkOrderController.java teacher.html index.html
git commit -m "feat: add overdue list API and frontend markers"
```

---

## Phase 5: Module 5 — Announcements & Tips

### Task 5.1: Announcement Entity + Mapper

**Files:**
- Create: `entity/Announcement.java`
- Create: `mapper/AnnouncementMapper.java`

- [ ] **Step 1:** Announcement entity (8 fields: id, title, content, category, isPinned, publisherId, createTime, updateTime).

- [ ] **Step 2:** AnnouncementMapper extends BaseMapper.

- [ ] **Step 3:** Commit

### Task 5.2: Announcement Service + Controller

**Files:**
- Create: `service/AnnouncementService.java`
- Create: `controller/AnnouncementController.java`

- [ ] **Step 1:** AnnouncementService with CRUD + paginated list by category + pinned-first sorting.

- [ ] **Step 2:** AnnouncementController with REST endpoints (list, detail, create, update, delete). POST/PUT/DELETE guarded by admin role check.

- [ ] **Step 3:** Commit

### Task 5.3: announcement.html + Login Page Update

**Files:**
- Create: `announcement.html`
- Modify: `login.html`

- [ ] **Step 1:** announcement.html — card-style list with category filter tabs (全部/停水停电/维修小贴士/普通通知), pinned items with pin icon, detail modal.

- [ ] **Step 2:** login.html — add "最新公告" section at page bottom showing latest 3 pinned announcements (fetch from `/announcement/list?pinned=1&size=3`).

- [ ] **Step 3:** Add announcement entry link in admin.html top bar.

- [ ] **Step 4:** Commit

```bash
git add announcement.html login.html admin.html
git add src/main/java/com/school/dormrepair/entity/Announcement.java
git add src/main/java/com/school/dormrepair/mapper/AnnouncementMapper.java
git add src/main/java/com/school/dormrepair/service/AnnouncementService.java
git add src/main/java/com/school/dormrepair/controller/AnnouncementController.java
git commit -m "feat: add announcement module with seed tips and login integration"
```

---

## Phase 6: Module 6 — Inventory Management

### Task 6.1: Inventory Entities + Mappers

**Files:**
- Create: `entity/InventoryItem.java`
- Create: `entity/InventoryRecord.java`
- Create: `mapper/InventoryItemMapper.java`
- Create: `mapper/InventoryRecordMapper.java`

- [ ] **Step 1:** InventoryItem: id, name, category, quantity, safetyThreshold, unit, updateTime.

- [ ] **Step 2:** InventoryRecord: id, itemId, type(in/out), quantity, operatorId, workOrderId, remark, createTime.

- [ ] **Step 3:** Both mappers extend BaseMapper.

- [ ] **Step 4:** Commit

### Task 6.2: Inventory Service + Controller

**Files:**
- Create: `service/InventoryService.java`
- Create: `controller/InventoryController.java`

- [ ] **Step 1:** InventoryService with:
  - CRUD for items
  - `stockIn(itemId, quantity, operatorId, remark)` — increase quantity + insert record
  - `stockOut(itemId, quantity, operatorId, workOrderId, remark)` — decrease quantity + insert record + check low stock → notification
  - `lowStock()` — query items where quantity < safetyThreshold
  - `records(itemId)` — paginated record list

- [ ] **Step 2:** InventoryController — REST endpoints for all above.

- [ ] **Step 3:** Commit

### Task 6.3: inventory.html

**Files:**
- Create: `inventory.html`

- [ ] **Step 1:** Full page with:
  - Inventory list table (name, category, quantity, threshold, unit, actions)
  - Low stock items highlighted in red
  - "入库" / "出库" buttons opening modals
  - Record history timeline for each item
  - "低库存预警" badge on top nav
  - Admin-only access

- [ ] **Step 2:** Add inventory entry link in admin.html.

- [ ] **Step 3:** Commit

```bash
git add inventory.html admin.html
git add src/main/java/com/school/dormrepair/entity/InventoryItem.java
git add src/main/java/com/school/dormrepair/entity/InventoryRecord.java
git add src/main/java/com/school/dormrepair/mapper/InventoryItemMapper.java
git add src/main/java/com/school/dormrepair/mapper/InventoryRecordMapper.java
git add src/main/java/com/school/dormrepair/service/InventoryService.java
git add src/main/java/com/school/dormrepair/controller/InventoryController.java
git commit -m "feat: add inventory management module"
```

---

## Phase 7: Module 7 — Big Screen Dashboard

### Task 7.1: Extended Stats APIs

**Files:**
- Modify: `controller/StatsController.java`

- [ ] **Step 1:** Extend `/stats/dashboard` response to include completionRate and avgRating:

```java
@GetMapping("/dashboard")
public Result<Map<String, Object>> dashboard() {
    Map<String, Object> data = new HashMap<>();
    data.put("total", workOrderMapper.selectCount(null));
    data.put("pending", countByStatus("pending"));
    data.put("processing", countByStatus("processing"));
    data.put("pendingAcceptance", countByStatus("pending_acceptance"));

    Long completed = countByStatus("completed") + countByStatus("accepted");
    Long total = workOrderMapper.selectCount(null);
    data.put("completionRate", total > 0
        ? String.format("%.1f", completed * 100.0 / total) : "0.0");

    // Average rating
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.isNotNull(WorkOrder::getEvaluateAttitude);
    List<WorkOrder> rated = workOrderMapper.selectList(qw);
    if (!rated.isEmpty()) {
        double avg = rated.stream()
            .mapToDouble(o -> (o.getEvaluateAttitude() + o.getEvaluateSpeed() + o.getEvaluateQuality()) / 3.0)
            .average().orElse(0);
        data.put("avgRating", String.format("%.1f", avg));
    } else {
        data.put("avgRating", "0.0");
    }
    return Result.success(data);
}
```

- [ ] **Step 2:** Add `/stats/heatmap`:

```java
@GetMapping("/heatmap")
public Result<List<Map<String, Object>>> heatmap() {
    List<Dorm> dorms = dormMapper.selectList(null);
    List<Map<String, Object>> result = new ArrayList<>();
    for (Dorm d : dorms) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(WorkOrder::getDormId, d.getId());
        Map<String, Object> item = new HashMap<>();
        item.put("building", d.getBuilding());
        item.put("room", d.getRoom());
        item.put("count", workOrderMapper.selectCount(qw));
        result.add(item);
    }
    return Result.success(result);
}
```

- [ ] **Step 3:** Add `/stats/teacher-ranking`:

```java
@GetMapping("/teacher-ranking")
public Result<List<Map<String, Object>>> teacherRanking() {
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.isNotNull(WorkOrder::getEvaluateAttitude)
      .isNotNull(WorkOrder::getTeacherId);
    List<WorkOrder> orders = workOrderMapper.selectList(qw);
    Map<Long, List<WorkOrder>> group = orders.stream()
        .collect(Collectors.groupingBy(WorkOrder::getTeacherId));
    List<Map<String, Object>> ranking = new ArrayList<>();
    for (var entry : group.entrySet()) {
        double avg = entry.getValue().stream()
            .mapToDouble(o -> (o.getEvaluateAttitude() + o.getEvaluateSpeed() + o.getEvaluateQuality()) / 3.0)
            .average().orElse(0);
        User t = userMapper.selectById(entry.getKey());
        Map<String, Object> item = new HashMap<>();
        item.put("teacherId", entry.getKey());
        item.put("teacherName", t != null ? t.getName() : "未知");
        item.put("avgRating", String.format("%.1f", avg));
        item.put("orderCount", entry.getValue().size());
        ranking.add(item);
    }
    ranking.sort((a, b) -> Double.compare(
        Double.parseDouble((String) b.get("avgRating")),
        Double.parseDouble((String) a.get("avgRating"))));
    return Result.success(ranking);
}
```

- [ ] **Step 4:** Add `/stats/recent-orders`:

```java
@GetMapping("/recent-orders")
public Result<List<WorkOrder>> recentOrders() {
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.orderByDesc(WorkOrder::getSubmitTime).last("LIMIT 10");
    return Result.success(workOrderMapper.selectList(qw));
}
```

- [ ] **Step 5:** Commit

```bash
git add src/main/java/com/school/dormrepair/controller/StatsController.java
git commit -m "feat: extend stats API with heatmap, ranking, and completion metrics"
```

### Task 7.2: bigscreen.html — Full-Screen Dashboard

**Files:**
- Create: `bigscreen.html`

- [ ] **Step 1:** Build full-screen dashboard page:

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=1920, initial-scale=1">
<title>宿舍报修数据看板</title>
<link rel="stylesheet" href="design-system.css">
<script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"></script>
<script src="https://unpkg.com/axios/dist/axios.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
<script src="api.js"></script>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { background: #0f172a; color: #e2e8f0; font-family: 'Inter', sans-serif; overflow: hidden; }
  .dashboard { display: grid; grid-template-rows: auto 1fr 1fr; height: 100vh; padding: 20px 30px; gap: 20px; }
  .top-bar { display: flex; align-items: center; justify-content: space-between; }
  .top-bar h1 { font-size: 36px; font-weight: 700; color: #f1f5f9; }
  .top-bar .time { font-size: 18px; color: #94a3b8; }
  .top-bar .alerts { display: flex; gap: 24px; font-size: 18px; }
  .top-bar .alerts .count { font-size: 32px; font-weight: 700; }
  .alert-overdue { color: #f87171; }
  .alert-urgent { color: #fbbf24; }
  .stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
  .stat-card { background: #1e293b; border-radius: 10px; padding: 20px 24px; border: 1px solid #334155; }
  .stat-card .label { font-size: 14px; color: #94a3b8; margin-bottom: 8px; }
  .stat-card .value { font-size: 40px; font-weight: 700; }
  .stat-card .value.blue { color: #60a5fa; }
  .stat-card .value.green { color: #4ade80; }
  .stat-card .value.amber { color: #fbbf24; }
  .stat-card .value.yellow { color: #facc15; }
  .main-area { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
  .panel { background: #1e293b; border-radius: 10px; padding: 16px; border: 1px solid #334155; }
  .panel-title { font-size: 16px; font-weight: 600; color: #cbd5e1; margin-bottom: 12px; }
  .heatmap { display: grid; grid-template-columns: repeat(auto-fill, minmax(60px, 1fr)); gap: 4px; }
  .heatmap-cell { aspect-ratio: 1; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 600; cursor: pointer; }
  .bottom-area { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
  .scroll-list { overflow-y: auto; max-height: 200px; }
  .scroll-item { padding: 10px 12px; border-bottom: 1px solid #334155; font-size: 14px; display: flex; justify-content: space-between; }
  .scroll-item:last-child { border-bottom: none; }
</style>
</head>
<body>
<div id="app">
  <div class="dashboard">
    <!-- Top Bar -->
    <div class="top-bar">
      <h1>宿舍报修数据看板</h1>
      <div class="time">{{ now }}</div>
      <div class="alerts">
        <div>超时 <span class="count alert-overdue">{{ overdueCount }}</span></div>
        <div>紧急 <span class="count alert-urgent">{{ urgentCount }}</span></div>
      </div>
    </div>

    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="label">总工单</div>
        <div class="value blue">{{ stats.total }}</div>
      </div>
      <div class="stat-card">
        <div class="label">完成率</div>
        <div class="value green">{{ stats.completionRate }}%</div>
      </div>
      <div class="stat-card">
        <div class="label">处理中</div>
        <div class="value amber">{{ stats.processing }}</div>
      </div>
      <div class="stat-card">
        <div class="label">平均评分</div>
        <div class="value yellow">{{ stats.avgRating }}</div>
      </div>
    </div>

    <!-- Main Area: Heatmap + Charts -->
    <div class="main-area">
      <div class="panel">
        <div class="panel-title">楼栋报修热力图</div>
        <div class="heatmap">
          <div v-for="h in heatmapData" :key="h.label"
               class="heatmap-cell" :style="{ background: heatColor(h.count) }"
               :title="h.label + ': ' + h.count + '单'">
            {{ h.label }}
          </div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-title">故障类型统计 / 周趋势</div>
        <canvas id="trendChart" height="180"></canvas>
      </div>
    </div>

    <!-- Bottom: Overdue + Recent -->
    <div class="bottom-area">
      <div class="panel">
        <div class="panel-title">超时工单预警</div>
        <div class="scroll-list">
          <div v-for="o in overdueList" :key="o.id" class="scroll-item">
            <span>{{ o.orderNo }}</span>
            <span>{{ o.description?.substring(0, 20) }}</span>
            <span style="color:#f87171">{{ o.submitTime }}</span>
          </div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-title">最近工单动态</div>
        <div class="scroll-list">
          <div v-for="o in recentOrders" :key="o.id" class="scroll-item">
            <span>{{ o.orderNo }}</span>
            <span :class="'pill-' + o.status">{{ o.status }}</span>
            <span style="color:#94a3b8">{{ o.submitTime?.substring(0, 16) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<script>
const { createApp } = Vue;
createApp({
  data() {
    return {
      now: '',
      overdueCount: 0,
      urgentCount: 0,
      stats: { total: 0, completionRate: '0.0', processing: 0, avgRating: '0.0' },
      heatmapData: [],
      overdueList: [],
      recentOrders: [],
    };
  },
  mounted() {
    this.refresh();
    setInterval(() => this.refresh(), 30000);
    setInterval(() => this.now = new Date().toLocaleString('zh-CN'), 1000);
  },
  methods: {
    async refresh() {
      try {
        const [dashboard, heatmap, overdue, recent, urgent] = await Promise.all([
          api.get('/stats/dashboard'),
          api.get('/stats/heatmap'),
          api.get('/order/overdue'),
          api.get('/stats/recent-orders'),
          api.get('/order/urgent'),
        ]);
        this.stats = dashboard.data.data;
        this.heatmapData = heatmap.data.data;
        this.overdueList = overdue.data.data;
        this.overdueCount = overdue.data.data.length;
        this.recentOrders = recent.data.data;
        this.urgentCount = urgent.data.data.length;
        this.drawTrend();
      } catch (e) { console.error(e); }
    },
    heatColor(count) {
      if (count === 0) return '#334155';
      if (count <= 3) return '#1e40af';
      if (count <= 6) return '#3b82f6';
      if (count <= 10) return '#60a5fa';
      return '#93c5fd';
    },
    drawTrend() {
      api.get('/stats/trend?days=7').then(res => {
        const data = res.data.data;
        const ctx = document.getElementById('trendChart');
        if (!ctx) return;
        new Chart(ctx, {
          type: 'bar',
          data: {
            labels: data.map(d => d.date),
            datasets: [{
              label: '报修数', data: data.map(d => d.count),
              backgroundColor: '#3b82f6', borderRadius: 4
            }]
          },
          options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { labels: { color: '#94a3b8' } } },
            scales: {
              x: { ticks: { color: '#94a3b8' }, grid: { color: '#334155' } },
              y: { ticks: { color: '#94a3b8' }, grid: { color: '#334155' } }
            }
          }
        });
      });
    }
  }
}).mount('#app');
</script>
</body>
</html>
```

- [ ] **Step 2:** Commit

```bash
git add bigscreen.html
git commit -m "feat: add full-screen dashboard with heatmap, trends, and alerts"
```

### Task 7.3: Update index.html Admin Dashboard

**Files:**
- Modify: `index.html`

- [ ] **Step 1:** Replace existing stat cards with DESIGN.md `stat-card` style showing total, completionRate, avgRating, overdueCount.

- [ ] **Step 2:** Add compact heatmap section (building × count colored cells).

- [ ] **Step 3:** Add notification bell component.

- [ ] **Step 4:** Commit

```bash
git add index.html
git commit -m "feat: upgrade admin dashboard with new stats, heatmap, and notification bell"
```

---

## Phase 8: Polish — Notification Bell + SMS Interface + UI Consolidation

### Task 8.1: Notification Bell Component (All Pages)

**Files:**
- Modify: `student.html`, `teacher.html`, `index.html`, `admin.html`

- [ ] **Step 1:** Add notification bell to top bar on all pages:

```html
<div class="notif-bell" @click="toggleNotif" @click.outside="showNotif=false">
  🔔
  <span class="badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
  <div class="notif-dropdown" v-if="showNotif">
    <div v-for="n in notifications" :key="n.id"
         class="notif-item" :class="{ unread: n.isRead === 0 }"
         @click="readNotif(n)">
      <div class="notif-item-title">{{ n.title }}</div>
      <div class="notif-item-content">{{ n.content }}</div>
      <div class="notif-item-time">{{ n.createTime }}</div>
    </div>
    <div v-if="notifications.length === 0" style="padding:16px;text-align:center;color:var(--ink-muted)">
      暂无通知
    </div>
    <div style="padding:8px;text-align:center;border-top:1px solid var(--border)" v-if="notifications.length > 0">
      <button class="btn btn-ghost btn-sm" @click="markAllRead">全部已读</button>
    </div>
  </div>
</div>
```

- [ ] **Step 2:** Add shared Vue mixin/composable for notification logic (fetch unreadCount every 30s, toggle dropdown, mark read).

- [ ] **Step 3:** Commit

```bash
git add student.html teacher.html index.html admin.html
git commit -m "feat: add notification bell to all pages"
```

### Task 8.2: SMS Service Interface (Reserved)

**Files:**
- Create: `service/SmsService.java`

- [ ] **Step 1:** Create SmsService interface:

```java
package com.school.dormrepair.service;

public interface SmsService {
    void send(String phone, String content);
}
```

- [ ] **Step 2:** Create default no-op implementation:

```java
package com.school.dormrepair.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceDefault implements SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsServiceDefault.class);
    @Override
    public void send(String phone, String content) {
        log.info("[SMS] To: {}, Content: {}", phone, content);
    }
}
```

- [ ] **Step 3:** Add config to `application.yml`:

```yaml
sms:
  provider: none  # none / aliyun / tencent
  aliyun:
    access-key-id:
    access-key-secret:
    sign-name: 宿舍报修系统
    template-code:
```

- [ ] **Step 4:** Commit

```bash
git add src/main/java/com/school/dormrepair/service/SmsService.java
git add src/main/java/com/school/dormrepair/service/SmsServiceDefault.java
git add src/main/resources/application.yml
git commit -m "feat: add SMS service interface with no-op default implementation"
```

### Task 8.3: Final UI Consolidation Pass

**Files:**
- Modify: All `.html` files

- [ ] **Step 1:** Ensure every page loads `<link rel="stylesheet" href="design-system.css">` and Inter font.

- [ ] **Step 2:** Replace all remaining inline styles with DESIGN.md CSS classes.

- [ ] **Step 3:** Verify responsive behavior at 768px breakpoint.

- [ ] **Step 4:** Commit

```bash
git add *.html
git commit -m "style: final UI consolidation per DESIGN.md"
```

---

## Testing Checklist

After all phases:

1. Start the Spring Boot app: `./mvnw spring-boot:run`
2. Run migration SQL if not auto-executed
3. Login as each role (student/teacher/admin)
4. **Module 1:** Student submits order → teacher accepts → teacher completes → student confirms acceptance → student rates 3 dimensions → verify bad rating notifies admin
5. **Module 2:** Student selects urgentLevel=1 fault type → verify urgent notification pushed → verify urgent highlight on all pages
6. **Module 3:** Urgent order auto-assigned to random teacher → teacher claims order from pool → admin reassigns order
7. **Module 4:** Set timeout_hours=0 for a fault type → submit and wait 1 minute → verify overdue flag → verify notification
8. **Module 5:** Admin publishes announcement → verify on login page and announcement list
9. **Module 6:** Admin creates inventory item → stock in → stock out below threshold → verify low stock notification
10. **Module 7:** Open bigscreen.html → verify 4 stat cards, heatmap, chart, overdue list, 30s auto-refresh
11. **UI:** Verify Inter font loaded, color system correct, responsive at 768px
