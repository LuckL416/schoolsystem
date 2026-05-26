# 宿舍报修系统 全面优化 — 设计规格书

> 日期: 2026-05-27 | 基于现有系统 v4 (Phase 1-4 已完成)

## 概述

在现有宿舍报修系统基础上新增7大功能模块，并引入基于 [DESIGN.md](../../../DESIGN.md) 的全站UI优化。

**现有系统:** Spring Boot 3.2.4 + MyBatis-Plus 3.5.7 + MySQL 8.0 + Vue 3 (CDN) + Chart.js

### 实施顺序(按依赖关系)

| # | 模块 | 依赖 |
|---|------|------|
| 1 | 在线验收 + 评价打分 | 无(扩展现有评价) |
| 2 | 紧急报修绿色通道 | 无(扩展urgentLevel) |
| 3 | 自动派单 + 抢单模式 | 模块2(紧急判定) |
| 4 | 超时工单自动提醒 | 模块2(超时配置) + 模块3(分配态) |
| 5 | 宿舍公告与维修小贴士 | 无 |
| 6 | 耗材库存管理 | 无 |
| 7 | 可视化报修大屏看板 | 模块1-6(完整数据) |
| * | 全站UI优化(DESIGN.md) | 与模块1同步进行 |

---

## 模块1: 在线验收 + 评价打分

### 流程变更

```
旧流程: pending → processing → completed → (评价)
新流程: pending → processing → pending_acceptance → accepted → (评价完成后关闭)
```

师傅点"完成维修"后工单状态变为 `pending_acceptance`，学生需确认验收后状态变为 `accepted`，之后可评价打分。验收和评价是两个独立步骤。

### work_order 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| status | VARCHAR | 扩展值: pending_acceptance, accepted (原有: pending, processing, completed) |
| acceptance_time | DATETIME | 学生确认验收时间 |
| evaluate_attitude | INT | 态度评分 1-5 |
| evaluate_speed | INT | 速度评分 1-5 |
| evaluate_quality | INT | 质量评分 1-5 |
| evaluate_comment | VARCHAR(200) | 文字评价 |
| evaluate_time | DATETIME | 评价时间 |

### API 新增/修改

| 方法 | 路径 | 变更 |
|------|------|------|
| POST | /order/acceptance/{orderId} | 新增 — 学生确认验收 |
| POST | /order/evaluate/{orderId} | 修改 — 参数改为 attitude/speed/quality/comment |
| GET | /order/bad-ratings | 新增 — 差评列表(三维均分<3) |

### 差评预警
评价提交后若 (attitude + speed + quality) / 3 < 3，自动插 notification 表通知管理员。

### 前端
- student.html: 待验收工单显示"确认验收"按钮；已验收未评价显示评价入口(三星维度+文本框弹窗)
- teacher.html: 工单完成按钮后显示"待验收"状态

---

## 模块2: 紧急报修绿色通道

### 判定规则
`fault_type.urgent_level ≤ 2` 的故障类型(level=1如水管爆裂/电路起火, level=2如大面积漏水/断电)→ 提交时自动标记为紧急工单 `is_urgent=1`。level=3 为普通报修。

### work_order 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| is_urgent | TINYINT | 是否紧急(0/1) |
| urgent_level | INT | 紧急级别(1-3，从fault_type继承) |

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /order/submit | 修改 — 检测urgentLevel≤2，设is_urgent=1，推送通知给所有师傅和管理员 |
| GET | /order/urgent | 新增 — 查询紧急未处理工单(按urgent_level排序) |

### 通知
紧急工单提交 → notification表插入给所有师傅+管理员 → 前端顶部红色闪烁通知

### 前端
- student.html: 选择urgent_level=1故障类型时显示红色⚠图标，提交弹窗确认
- teacher.html: 紧急工单红色左边框、置顶、"优先处理"标签
- index.html: 紧急工单列表高亮

---

## 模块3: 自动派单 + 抢单模式

### 机制
- **自动派单:** 紧急工单(urgent_level≤2)提交后系统按fault_type.work_type匹配工种，随机选师傅分配
- **抢单:** 普通工单(urgent_level=3)未分配时进入"工单池"，师傅可主动抢单
- **管理员改派:** 管理员可随时手动将工单改派给其他师傅

### work_order 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| assigned_teacher_id | BIGINT | 被分配师傅ID(NULL=工单池待抢) |
| assign_time | DATETIME | 分配/抢单时间 |

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /order/assign/{orderId} | 新增 — 管理员手动派单(参数: teacherId) |
| POST | /order/claim/{orderId} | 新增 — 师傅抢单 |
| GET | /order/pool | 新增 — 工单池(未分配且非紧急pending工单) |
| POST | /order/accept/{orderId} | 修改 — 已分配工单直接接单 |

### 前端
- teacher.html: "工单池"标签页 + "我的工单"(分已分配/已抢)
- index.html: 管理员可查看分配状态，手动改派

---

## 模块4: 超时工单自动提醒

### 机制
每个故障类型配置超时时限(小时)，定时任务每分钟扫描超时未处理工单，自动推送管理员通知。

### fault_type 表新增

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| timeout_hours | INT | 48 | 超时时限(小时)，紧急类默认4 |

### work_order 表新增

| 字段 | 类型 | 说明 |
|------|------|------|
| is_overdue | TINYINT | 是否超时(0/1) |

### 定时任务
```java
@Scheduled(fixedRate = 60000)
// 扫描: status IN (pending,processing) 
//       AND accept_time < NOW() - fault_type.timeout_hours 小时
// 设 is_overdue=1，插 notification
```

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /order/overdue | 超时工单列表 |
| POST | /order/overdue/notify/{orderId} | 手动再次提醒师傅 |

### 前端
- 通知中心: 超时提醒
- teacher.html: 超时项红色标记
- index.html: "超时工单"统计卡

---

## 模块5: 宿舍公告与维修小贴士

### announcement 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| title | VARCHAR(100) | 标题 |
| content | TEXT | 正文 |
| category | VARCHAR(20) | notice/tip/general |
| is_pinned | TINYINT | 置顶 0/1 |
| publisher_id | BIGINT | FK user.id |
| create_time | DATETIME | |
| update_time | DATETIME | |

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /announcement/list | 公告列表(分类筛选、分页) |
| GET | /announcement/{id} | 公告详情 |
| POST | /announcement | 管理员发布 |
| PUT | /announcement/{id} | 管理员编辑 |
| DELETE | /announcement/{id} | 管理员删除 |

### 种子数据: 6条维修小贴士
| 标题 | 分类 |
|------|------|
| 宿舍跳闸恢复指南 | tip |
| 空调滤网清洗教程 | tip |
| 马桶/下水道轻微堵塞处理 | tip |
| 水龙头滴水简易处理 | tip |
| 热水器不出热水排查 | tip |
| 窗户卡住/推拉不畅 | tip |

### 前端
- announcement.html: 卡片式公告列表 + 分类筛选 + 详情页
- login.html: 底部展示最新3条公告
- 各首页顶部: 置顶公告横幅

---

## 模块6: 耗材库存管理

### inventory_item 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| name | VARCHAR(50) | 耗材名称 |
| category | VARCHAR(20) | 分类(电工/水工/木工/五金) |
| quantity | INT | 当前库存 |
| safety_threshold | INT | 安全阈值 |
| unit | VARCHAR(10) | 单位(个/米/卷) |
| update_time | DATETIME | |

### inventory_record 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| item_id | BIGINT | FK inventory_item |
| type | VARCHAR(10) | in/out |
| quantity | INT | 数量 |
| operator_id | BIGINT | FK user |
| work_order_id | BIGINT | 关联工单(可空) |
| remark | VARCHAR(100) | |
| create_time | DATETIME | |

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /inventory/list | 耗材列表(分页) |
| POST | /inventory | 新增耗材 |
| PUT | /inventory/{id} | 编辑耗材 |
| POST | /inventory/{id}/in | 入库 |
| POST | /inventory/{id}/out | 出库 |
| GET | /inventory/{id}/records | 出入库记录 |
| GET | /inventory/low-stock | 低库存预警 |

### 低库存预警
出库后 quantity < safety_threshold → notification 通知管理员

### 前端
- inventory.html: 耗材列表+库存管理+出入库记录
- 仅admin角色可访问

---

## 模块7: 可视化报修大屏看板

### 布局(1920×1080)

两大部分: 独立全屏页 bigscreen.html + 嵌入管理首页精简版

**大屏布局:**
```
┌─────────────────────────────────────────────────┐
│  宿舍报修数据看板  [实时]  超时:3  紧急:1     │ 顶栏
├────────┬────────┬────────┬──────────────────┤
│ 总工单  │ 完成率   │ 处理中   │ 平均评分         │ 4卡片
│ 1,247  │ 87.3%  │  42    │ ★ 4.3          │
├────────┴────────┴────────┴──────────────────┤
│                    │                          │
│  楼栋报修热力图     │  故障类型统计(柱状图)     │ 中区
│  (矩阵色块)        │  + 周趋势(折线)          │
│                    │                          │
├────────────────────┴──────────────────────────┤
│  超时工单预警(滚动)  │  最近工单动态(滚动)     │ 底区
└───────────────────────────────────────────────┘
```

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /stats/dashboard | 修改 — 扩展增加completionRate, avgRating |
| GET | /stats/heatmap | 新增 — 各楼栋×故障类型数量矩阵 |
| GET | /stats/overdue | 新增 — 超时工单列表 |
| GET | /stats/recent-orders | 新增 — 最近10条工单 |
| GET | /stats/teacher-ranking | 新增 — 师傅评价排名 |

### 热力图
- 楼栋×楼层矩阵，色阶: #dbeafe(0) → #93bbfd(中) → #2563eb(高) → #1e40af(极高)
- 30秒自动刷新

### 前端
- bigscreen.html: CSS Grid, setInterval(30000), 响应1080p
- index.html: 升级统计卡片，新增精简热力图

---

## 共享基础设施

### notification 表(通知中心)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| user_id | BIGINT | 接收用户ID |
| type | VARCHAR(30) | urgent/bad_rating/overdue/low_stock/assign/system |
| title | VARCHAR(100) | 通知标题 |
| content | VARCHAR(255) | 通知内容 |
| related_id | BIGINT | 关联业务ID(工单/耗材等) |
| is_read | TINYINT | 已读 0/1 |
| create_time | DATETIME | |

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /notification/list | 当前用户通知列表(分页) |
| GET | /notification/unread-count | 未读数量 |
| PUT | /notification/{id}/read | 标记已读 |
| PUT | /notification/read-all | 全部已读 |

### 前端(通知铃铛)
- 各页面顶栏右侧: 铃铛图标+未读红点badge
- 点击展开通知下拉列表(最近5条)，底部"查看全部"
- 红色通知(紧急)、橙色(超时)、蓝色(普通)色标区分

### 短信接口预留
```java
public interface SmsService {
    void send(String phone, String content); // 默认实现: log输出
}
```
application.yml 预留配置: `sms.provider=none` / `sms.aliyun.access-key-id=` / `sms.aliyun.access-key-secret=`

---

## 数据库变更汇总

### 修改表(ALTER)
- **work_order**: 新增11个字段(acceptance_time, evaluate_attitude, evaluate_speed, evaluate_quality, evaluate_comment, evaluate_time, is_urgent, urgent_level, assigned_teacher_id, assign_time, is_overdue), status 扩展2个值
- **fault_type**: 新增 timeout_hours

### 新建表(CREATE)
- **announcement**: 公告/小贴士
- **inventory_item**: 耗材库存
- **inventory_record**: 出入库记录
- **notification**: 系统通知

---

## 前端页面清单

| 页面 | 操作 | 说明 |
|------|------|------|
| login.html | 修改 | 底部公告展示 + UI优化 |
| register.html | 修改 | UI优化 |
| student.html | 修改 | 验收+评价+紧急标识+UI优化 |
| teacher.html | 修改 | 抢单池+超时标记+警报+UI优化 |
| index.html | 修改 | 升级卡片+热力图+通知+UI优化 |
| admin.html | 修改 | UI优化 |
| bigscreen.html | **新建** | 大屏看板 |
| announcement.html | **新建** | 公告/小贴士 |
| inventory.html | **新建** | 耗材库存管理 |

---

## 后端新增文件清单

| 文件 | 说明 |
|------|------|
| entity/Announcement.java | 公告实体 |
| entity/InventoryItem.java | 耗材实体 |
| entity/InventoryRecord.java | 出入库记录实体 |
| entity/Notification.java | 通知实体 |
| mapper/AnnouncementMapper.java | |
| mapper/InventoryItemMapper.java | |
| mapper/InventoryRecordMapper.java | |
| mapper/NotificationMapper.java | |
| service/AnnouncementService.java | |
| service/InventoryService.java | |
| service/NotificationService.java | |
| service/SmsService.java | 短信接口(预留) |
| controller/AnnouncementController.java | |
| controller/InventoryController.java | |
| controller/NotificationController.java | |
| task/OverdueCheckTask.java | 超时检测定时任务 |
