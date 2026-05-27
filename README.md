# 校园宿舍报修管理系统

> 大学宿舍物品报修系统 — Spring Boot + Vue 3 全栈项目

## 项目简介

便捷报修 · 快速响应 · 全程追踪。支持**学生在线报修、师傅接单维修、管理员后台管理**三大角色，涵盖紧急通道、自动派单、在线验收评价、大屏数据看板、耗材库存管理等完整功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.4 + MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0 |
| 鉴权 | JWT + BCrypt |
| 前端 | Vue 3 CDN + Axios + Chart.js 4 |
| 设计 | DESIGN.md 设计系统 |

## 快速开始

```bash
cd dorm-repair
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS dorm_repair_system DEFAULT CHARSET utf8mb4;"
# 2. 执行迁移
mysql -u root -p dorm_repair_system < src/main/resources/db/migration.sql
# 3. 启动后端
./mvnw spring-boot:run
# 4. 浏览器打开 login.html
```

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 |
| student | 123456 | 学生 |
| teacher | 123456 | 师傅 |

## 功能亮点

- **在线验收 + 多维度评价** — 态度/速度/质量三维打分，差评自动预警
- **紧急报修绿色通道** — 漏水、断电自动优先推送
- **自动派单 + 抢单双机制** — 系统分配与师傅自主抢单并行
- **超时工单自动提醒** — 定时扫描，超时通知管理员
- **宿舍公告 + 维修小贴士** — 停水停电通知、常见故障自查教程
- **耗材库存管理** — 出入库记录，低库存预警
- **可视化大屏看板** — 热力图、饼图、趋势图，30秒自动刷新
- **流体渐变背景** — 浅蓝/淡青/浅紫低饱和动态背景

## 项目结构

```
schoolsystem/
├── README.md
└── dorm-repair/             # 详见 dorm-repair/README.md
    ├── src/                 # Spring Boot 后端
    ├── *.html               # Vue 3 前端页面（9个）
    ├── DESIGN.md            # 设计系统文档
    └── docs/                # 设计规格与实施计划
```

详细文档见 [dorm-repair/README.md](dorm-repair/README.md)
