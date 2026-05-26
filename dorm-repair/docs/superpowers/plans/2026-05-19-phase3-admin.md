# 阶段3：管理端增强 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 用户管理页、故障类型管理、工单筛选搜索、图表统计面板、Excel 导出优化

**Architecture:** 新增 AdminController 统一管理后端，新增 admin.html 做管理员专属页面，index.html 加筛选和图表。

**Tech Stack:** Spring Boot 3.2.4, MyBatis-Plus 3.5.7, Chart.js CDN, EasyExcel 3.3.2

---

## 文件变更

| 文件 | 操作 |
|------|------|
| `controller/AdminController.java` | 新建 — 用户CRUD + 故障类型CRUD |
| `service/AdminService.java` | 新建 — 管理端业务逻辑 |
| `controller/StatsController.java` | 修改 — 加趋势/按宿舍/按类型统计 |
| `controller/WorkOrderController.java` | 修改 — /all 加筛选参数 |
| `service/WorkOrderService.java` | 修改 — allList 加筛选逻辑 |
| `entity/WorkOrderExcelVO.java` | 新建 — Excel 中文表头 VO |
| `admin.html` | 新建 — 管理员管理页面 |
| `index.html` | 修改 — 加筛选栏+图表 |
| `common/WebMvcConfig.java` | 修改 — 加 /admin/** 拦截 |

---

### Task 1: AdminService + AdminController

**AdminService:** 用户增删改查、故障类型增删改
- `listUsers(page, size)` → 分页用户列表
- `addUser(User)` → BCrypt 加密后插入
- `updateUser(User)` → 更新用户（密码为空则不更新）
- `deleteUser(id)` → 删除用户
- `addFaultType(FaultType)` / `updateFaultType(FaultType)` / `deleteFaultType(id)`

**AdminController:**
- `GET /admin/users?page=1&size=10`
- `POST /admin/users`
- `PUT /admin/users/{id}`
- `DELETE /admin/users/{id}`
- `POST /admin/fault-types`
- `PUT /admin/fault-types/{id}`
- `DELETE /admin/fault-types/{id}`

### Task 2: 工单筛选

WorkOrderService.allList 加参数 `status, dormId, startDate, endDate`。
WorkOrderController `/order/all` 加对应 @RequestParam。

### Task 3: 统计增强

StatsController 加:
- `GET /stats/trend?days=7` → 近 N 天每天工单数
- `GET /stats/by-dorm` → 按宿舍统计
- `GET /stats/by-type` → 按故障类型统计

### Task 4: Excel 导出优化

WorkOrderExcelVO: 用 @ExcelProperty 定义中文列头 + 只导出必要字段
StatsController.export 改造: 用 WorkOrderExcelVO 替代 WorkOrder，支持筛选导出

### Task 5: admin.html

管理员管理页面：用户列表（分页+增删改弹窗）+ 故障类型管理（列表+增删改）

### Task 6: index.html 增强

加筛选栏（状态下拉+宿舍下拉+日期范围）+ Chart.js 图表（近7天趋势柱状图 + 故障类型饼图）

### Task 7: 编译+测试+提交
