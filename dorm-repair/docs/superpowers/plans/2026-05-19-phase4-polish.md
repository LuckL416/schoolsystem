# 阶段4：体验打磨 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 学生注册、修改密码、用户信息展示、工单分页、状态变更通知

**Architecture:** UserService 加 register/changePassword，所有页面加用户信息顶栏，工单列表加分页，学生端状态变更轮询提示。

---

## 文件变更

| 文件 | 操作 |
|------|------|
| `service/UserService.java` | 修改 — 加 register + changePassword |
| `controller/AuthController.java` | 修改 — 加 register + changePassword 端点 |
| `service/WorkOrderService.java` | 修改 — listByStudent + allList 加分页 |
| `controller/WorkOrderController.java` | 修改 — /my + /all 加分页参数 |
| `register.html` | 新建 — 学生注册页 |
| `login.html` | 修改 — 加注册链接 |
| `student.html` | 修改 — 顶栏用户信息 + 分页 + 状态通知 |
| `teacher.html` | 修改 — 顶栏用户信息 + 分页 |
| `index.html` | 修改 — 顶栏用户信息 + 分页 |
| `admin.html` | 修改 — 顶栏用户信息 |
| `common/WebMvcConfig.java` | 修改 — /auth/register 放行已在 exclude 中 |

### Task 1: UserService 加 register + changePassword

### Task 2: AuthController 加端点

### Task 3: 工单分页（后端）

### Task 4: register.html（含改密码）

### Task 5: 所有页面加用户信息顶栏 + 分页 + 通知

### Task 6: 编译+测试+提交
