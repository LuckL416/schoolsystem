# 宿舍报修系统 — 全面优化设计

## 概述

在现有 Spring Boot 3.2.4 + MyBatis-Plus + Vue3 项目基础上，分 4 阶段完成 17 项优化和新功能。

---

## 阶段1：地基（安全+架构）

### 1.1 JWT 拦截器

- 新增 `common/JwtInterceptor.java`：实现 `HandlerInterceptor`，从 `Authorization` header 解析 token，验证后注入 `request.setAttribute("userId")` 和 `request.setAttribute("role")`
- 新增 `common/WebMvcConfig.java`：注册拦截器，拦截 `/order/**`、`/stats/**`、`/auth/info`，放行 `/auth/login`、`/auth/register`
- 废弃各处 Controller 中重复的 `token.replace("Bearer ","")` + `jwtUtil.getUserId(token)` 逻辑
- Controller 参数改用 `@RequestAttribute("userId") Long userId` 或从 request 取值

### 1.2 全局异常处理

- 新增 `common/GlobalExceptionHandler.java`：`@RestControllerAdvice`，捕获 `BusinessException`（返回自定义 code+msg）、`Exception`（返回 500 + 通用错误信息）
- 新增 `common/BusinessException.java`：`RuntimeException` 子类，含 `code` 和 `message`
- 日志打印到后端控制台，前端只看到 Result 格式的错误

### 1.3 前端 API 公共层

- 新增 `src/main/resources/static/api.js`：封装 `API_BASE`、`headers()`、`get()`、`post()`
- 4 个 HTML 文件移除重复的 axios 调用和手动 token 拼接，改用 `api.get()` / `api.post()`

---

## 阶段2：核心流程

### 2.1 宿舍/故障类型选择

- 新增 `Dorm.java`、`FaultType.java` Entity
- 新增 `DormMapper.java`、`FaultTypeMapper.java`
- 新增 `DormService.java`、`FaultTypeService.java`（含列表查询）
- `/order/submit` 前端改为下拉框选择，不再手填 ID

### 2.2 工单图片上传

- 新增 `FileController.java`：接收 multipart 上传，存到 `app.upload-path`，返回 URL
- 前端 `student.html` 提交表单加文件选择器，先上传图片获取 URL，再提交工单

### 2.3 学生取消工单

- `POST /order/cancel/{orderId}`：只允许 `pending` 状态的工单取消
- 前端 student.html 列表中对 pending 工单显示"取消"按钮

### 2.4 师傅维修备注

- 给 `work_order` 表加 `remark` 字段（TEXT）
- `POST /order/complete/{orderId}` 接收可选的 `remark` 参数
- 前端 teacher.html 完工时弹出输入框填写处理方法

---

## 阶段3：管理端

### 3.1 用户管理页

- 新增 `admin.html`：管理员专属页面
- `GET /admin/users` — 用户列表（分页）
- `POST /admin/users` — 新增用户
- `PUT /admin/users/{id}` — 编辑用户
- `DELETE /admin/users/{id}` — 删除用户
- 新增 `AdminController.java`

### 3.2 故障类型管理

- 在 admin.html 中加入故障类型管理区域
- `POST /admin/fault-types` — 新增
- `PUT /admin/fault-types/{id}` — 编辑
- `DELETE /admin/fault-types/{id}` — 删除

### 3.3 工单筛选搜索

- 后端 `GET /order/all` 加参数：`status`、`dormId`、`startDate`、`endDate`
- 所有页面工单列表加筛选栏

### 3.4 图表统计面板

- 后端 `GET /stats/trend?days=7`：返回近 N 天每日工单数
- `GET /stats/by-dorm`：按宿舍统计
- `GET /stats/by-type`：按故障类型统计
- 前端用 Chart.js CDN 绘制柱状图/饼图

### 3.5 Excel 导出优化

- 新增 `WorkOrderExcelVO.java`：定义中文列头（`@ExcelProperty`）
- 支持按日期、状态筛选后导出
- 导出文件名为 `工单列表_20260519.xlsx`

---

## 阶段4：体验打磨

### 4.1 学生自主注册

- `POST /auth/register`：接收 name、username、password、phone、dormId，BCrypt 加密密码，role 固定为 student
- 新增 `register.html`

### 4.2 修改密码

- `POST /auth/change-password`：接收 oldPassword、newPassword
- 各角色页面加"修改密码"入口

### 4.3 用户信息展示

- 顶部栏显示当前用户名 + 角色
- 各页面 mounted 时调用 `/auth/info`

### 4.4 工单分页

- 后端 `GET /order/all` 和 `GET /order/my` 加分页参数（page、size）
- 前端列表底部加分页条

### 4.5 状态变更通知

- 前端 student.html 轮询或加载时检测：有新接单/完工的工单时右上角 Badge 提示
- 简单实现：加载列表时比对上次状态变化，用高亮行 + Toast 提醒

---

## 阶段5：登录直接跳转到对应页面

根据角色自动跳转到对应页面

---

## 文件变更总览

| 阶段 | 新增文件 | 修改文件 | 删除 |
|------|---------|---------|------|
| 阶段1 | 4 | 5 | 0 |
| 阶段2 | 4 | 3 | 0 |
| 阶段3 | 5 | 3 | 0 |
| 阶段4 | 3 | 6 | 0 |

---

## 注意事项

- 所有新增文件和现有文件保持一致：中文注释、Lombok、MyBatis-Plus 风格
- 前端保持 Vue3 CDN 方式，不引入构建工具
- 阶段间独立，每阶段完成后可独立验证
