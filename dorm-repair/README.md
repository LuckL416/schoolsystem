# 大学宿舍物品报修系统

Spring Boot 3.2.4 + MyBatis-Plus + Vue 3 构建的宿舍报修管理系统，支持学生报修、师傅接单维修、管理员后台管理。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.4 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0 |
| 鉴权 | JWT (jjwt 0.11.5) |
| 密码加密 | BCrypt (spring-security-crypto) |
| Excel | EasyExcel 3.3.2 |
| 接口文档 | SpringDoc (Swagger) |
| 前端 | Vue 3 CDN + Axios + Chart.js |

## 快速启动

### 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS dorm_repair_system DEFAULT CHARSET utf8mb4;
```

### 2. 修改配置

编辑 `src/main/resources/application.yml`，修改 MySQL 用户名和密码：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

### 3. 启动后端

```bash
cd dorm-repair
./mvnw spring-boot:run
```

启动后访问：
- API 地址：`http://localhost:8080/api`
- Swagger 文档：`http://localhost:8080/api/swagger-ui/index.html`

### 4. 打开前端页面

直接用浏览器打开项目根目录下的 HTML 文件：
- `login.html` — 登录页

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `123456` | 管理员 |
| `student` | `123456` | 学生 |
| `teacher` | `123456` | 师傅 |

## 功能模块

### 学生端（student.html）
- 下拉选择宿舍和故障类型
- 图片上传（故障现场拍照）
- 提交报修工单
- 查看我的报修记录
- 取消未处理的工单
- 评价已完成的维修

### 师傅端（teacher.html）
- 查看所有工单
- 接单（pending → processing）
- 完工并填写维修备注

### 宿管端（index.html）
- 统计面板（总工单、待处理、处理中、已完成）
- 近 7 天报修趋势图表
- 工单筛选（状态/宿舍/日期范围）
- Excel 导出（中文表头）

### 系统管理（admin.html）
- 用户管理（增删改查，分页）
- 故障类型管理

### 通用功能
- JWT 统一鉴权
- 角色切换登录（学生/师傅/管理员）
- 学生自主注册
- 修改密码
- 全局异常处理
- 工单日期编号（如 `20260519001`）
- 分页查询

## 项目结构

```
dorm-repair/
├── src/main/java/com/school/dormrepair/
│   ├── common/          # JWT工具、拦截器、异常处理、CORS配置
│   ├── controller/      # 控制器（Auth/WorkOrder/Stats/Admin/Dorm/FaultType/File）
│   ├── entity/          # 实体类（User/WorkOrder/Dorm/FaultType）
│   ├── mapper/          # MyBatis-Plus Mapper
│   └── service/         # 业务逻辑层
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   └── static/api.js    # 前端公共 API 层
├── login.html           # 登录页
├── register.html        # 学生注册页
├── student.html         # 学生端
├── teacher.html         # 师傅端
├── index.html           # 宿管端
├── admin.html           # 系统管理
└── docs/superpowers/    # 设计文档和实现计划
```

## 数据库表

| 表名 | 说明 |
|------|------|
| `user` | 用户（学生/师傅/管理员） |
| `work_order` | 报修工单 |
| `dorm` | 宿舍信息 |
| `fault_type` | 故障类型 |
| `operation_log` | 操作日志 |
