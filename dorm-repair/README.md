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
| 前端 | Vue 3 CDN + Axios + Chart.js 4 |
| 设计系统 | DESIGN.md + design-system.css |

## 快速启动

### 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS dorm_repair_system DEFAULT CHARSET utf8mb4;
```

### 2. 执行数据迁移

```bash
mysql -u root -p dorm_repair_system < src/main/resources/db/migration.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，修改 MySQL 用户名和密码：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

### 4. 启动后端

```bash
cd dorm-repair
./mvnw spring-boot:run
```

启动后访问：
- API 地址：`http://localhost:8080/api`
- Swagger 文档：`http://localhost:8080/api/swagger-ui.html`

### 5. 打开前端页面

直接用浏览器打开 `dorm-repair/` 目录下的 HTML 文件：
- `login.html` — 登录页

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `123456` | 管理员 |
| `student` | `123456` | 学生 |
| `teacher` | `123456` | 师傅 |

## 功能模块

### 学生端（student.html）
- 提交报修（宿舍选择、故障类型、描述、图片上传）
- 紧急报修自动识别（红色预警标识）
- 查看我的报修记录（分页、状态实时更新）
- 取消未处理工单
- **在线验收** — 师傅维修完成后确认验收
- **多维度评价** — 态度/速度/质量三维度1-5星+文字评价
- **维修小贴士** — 右侧6条常见故障自查教程，点击展开详细步骤
- Toast 通知提示，加载动画，空状态友好引导

### 师傅端（teacher.html）
- 查看所有工单（紧急工单红色边框置顶）
- **工单池抢单** — 普通工单自主抢单
- **自动派单** — 紧急工单系统自动按工种分配
- 接单 → 完工填写维修备注
- 超时工单红色标记提醒
- 通知铃铛实时推送

### 宿管端/数据看板（index.html）
- 统计面板（总工单、待处理、处理中、已完成、超时工单、完成率、平均评分）
- 近7天报修趋势图表
- 工单筛选（状态/宿舍/日期范围）
- Excel 导出（中文表头）
- 楼栋热力图
- **大屏看板入口** → `bigscreen.html`

### 大屏数据看板（bigscreen.html）
- 1920×1080 全屏设计，30秒自动刷新
- 4大核心指标卡片（总工单/完成率/处理中/平均评分）
- 楼栋报修热力图（色阶矩阵）
- 故障类型饼图 + 近7天趋势柱状图
- 超时工单预警滚动列表 + 最近工单动态
- 紧急/超时实时告警计数

### 系统管理（admin.html）
- 用户管理（增删改查，分页）
- 故障类型管理（超时时限配置）
- 公告管理入口、库存管理入口、大屏看板入口

### 公告管理（announcement.html）
- 管理员发布/编辑/删除公告
- 分类筛选：停水停电通知 / 维修小贴士 / 普通通知
- 置顶功能
- 学生/师傅/管理员页面顶部横向滚动公告栏
- 登录页底部最新公告展示
- 6条维修小贴士种子数据 + 5条通知公告

### 耗材库存管理（inventory.html）
- 耗材增删改查（名称/分类/数量/安全阈值/单位）
- 入库/出库操作 + 出入库记录查询
- 低库存自动预警（系统通知管理员）

### 通用功能
- JWT 统一鉴权
- 角色切换登录（学生/师傅/管理员）
- 学生自主注册
- 修改密码
- 全局异常处理
- **系统通知中心** — 紧急工单/差评预警/超时提醒/库存预警/派单通知
- **流体渐变背景** — 浅蓝/淡青/浅紫低饱和动态背景
- SMS 短信接口预留
- 全局响应式设计（适配平板/手机）

## 项目结构

```
dorm-repair/
├── src/main/java/com/school/dormrepair/
│   ├── common/              # JWT工具、拦截器、异常处理、CORS配置
│   ├── controller/          # 8个控制器（Auth/WorkOrder/Stats/Admin/Dorm/FaultType/File/Notification/Announcement/Inventory）
│   ├── entity/              # 8个实体类（User/WorkOrder/Dorm/FaultType/Notification/Announcement/InventoryItem/InventoryRecord）
│   ├── mapper/              # MyBatis-Plus Mapper（8个）
│   ├── service/             # 业务逻辑层（6个Service + SMS接口）
│   └── task/                # 定时任务（OverdueCheckTask）
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   ├── db/migration.sql     # 数据库迁移脚本
│   └── static/
│       ├── api.js           # 前端公共 API 层
│       └── design-system.css # 全局设计系统
├── DESIGN.md                # 设计系统文档
├── login.html               # 登录页
├── register.html            # 学生注册页
├── student.html             # 学生端
├── teacher.html             # 师傅端
├── index.html               # 宿管端
├── admin.html               # 系统管理
├── bigscreen.html           # 大屏数据看板
├── announcement.html        # 公告管理
├── inventory.html           # 耗材库存管理
├── api.js                   # 前端 API 层（项目根目录副本）
├── design-system.css        # 设计系统 CSS（项目根目录副本）
└── docs/superpowers/        # 设计文档和实现计划
```

## 数据库表

| 表名 | 说明 |
|------|------|
| `user` | 用户（学生/师傅/管理员） |
| `work_order` | 报修工单（含验收/评价/紧急/派单/超时字段） |
| `dorm` | 宿舍信息 |
| `fault_type` | 故障类型（含超时时限配置） |
| `notification` | 系统通知 |
| `announcement` | 公告/维修小贴士 |
| `inventory_item` | 耗材库存 |
| `inventory_record` | 出入库记录 |

## 设计系统

本项目使用 [DESIGN.md](DESIGN.md) 定义的设计系统，基于 awesome-design-md 规范：

| 要素 | 选择 |
|------|------|
| 底色 | 纯白画布 + 流体渐变背景 |
| 主色调 | 校园蓝 `#2563eb` |
| 辅助色 | 翠绿(成功)、琥珀(警告)、红(紧急) |
| 字体 | Inter (Google Fonts CDN) |
| 圆角 | 6px 按钮、10px 卡片 |
| 动效 | 6层柔光椭圆18秒缓慢流动 |
