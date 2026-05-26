# 阶段2：核心流程 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 宿舍/故障类型下拉选择、图片上传、学生取消工单、师傅填写维修备注

**Architecture:** 新增 Dorm/FaultType 实体对接已有数据库表，新增 FileController 处理图片上传，WorkOrder 增加 remark 字段，前端改为下拉选择+文件上传。

**Tech Stack:** Spring Boot 3.2.4, MyBatis-Plus 3.5.7, Vue 3 CDN, Axios CDN

---

## 文件变更总览

| 文件 | 操作 | 职责 |
|------|------|------|
| `entity/Dorm.java` | 新建 | 宿舍实体 |
| `entity/FaultType.java` | 新建 | 故障类型实体 |
| `mapper/DormMapper.java` | 新建 | 宿舍 Mapper |
| `mapper/FaultTypeMapper.java` | 新建 | 故障类型 Mapper |
| `controller/DormController.java` | 新建 | 宿舍列表 API |
| `controller/FaultTypeController.java` | 新建 | 故障类型列表 API |
| `controller/FileController.java` | 新建 | 图片上传 API |
| `entity/WorkOrder.java` | 修改 | 加 remark 字段 |
| `service/WorkOrderService.java` | 修改 | cancel + complete 加 remark |
| `controller/WorkOrderController.java` | 修改 | 新增 cancel 端点 |
| `student.html` | 修改 | 下拉框 + 文件上传 + 取消按钮 |
| `teacher.html` | 修改 | 完工弹 remark |
| 数据库 | 修改 | work_order 加 remark 列 |

---

### Task 1: 插入测试数据 + 加 remark 列

- [ ] **Step 1: 加 remark 列**

```sql
ALTER TABLE work_order ADD COLUMN remark TEXT COMMENT '维修备注';
```

- [ ] **Step 2: 插入宿舍和故障类型测试数据**

```sql
INSERT INTO dorm (building, room) VALUES
('1栋', '101'), ('1栋', '102'), ('1栋', '103'),
('2栋', '201'), ('2栋', '202'), ('2栋', '203'),
('3栋', '301'), ('3栋', '302'), ('3栋', '303');
INSERT INTO fault_type (name, work_type, urgent_level) VALUES
('水龙头漏水', '水工', 2),
('马桶堵塞', '水工', 3),
('灯管不亮', '电工', 2),
('插座损坏', '电工', 2),
('门锁故障', '木工', 2),
('空调故障', '电工', 1);
```

---

### Task 2: 创建 Dorm + FaultType 实体和 Mapper

**Files:** Create 4 files

Dorm.java:
```java
package com.school.dormrepair.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dorm")
public class Dorm {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String building;
    private String room;
}
```

FaultType.java:
```java
package com.school.dormrepair.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("fault_type")
public class FaultType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String workType;
    private Integer urgentLevel;
}
```

DormMapper.java & FaultTypeMapper.java — standard MyBatis-Plus BaseMapper extension.

---

### Task 3: 创建 DormController + FaultTypeController

**DormController:** `GET /dorm/list` — 返回所有宿舍，Result<List<Dorm>>
**FaultTypeController:** `GET /fault-type/list` — 返回所有故障类型，Result<List<FaultType>>

路径需要加入 JwtInterceptor 拦截（在 WebMvcConfig 中加 `/dorm/**`、`/fault-type/**`）。

---

### Task 4: 创建 FileController

`POST /file/upload` — 接收 MultipartFile，保存到 `app.upload-path`，返回可访问 URL。
注意：静态资源映射，使上传目录可被访问。

---

### Task 5: WorkOrder 加 remark + cancel + complete 改

- entity/WorkOrder.java: 加 `private String remark;`
- service/WorkOrderService.java:
  - `cancel(orderId, userId)`: 只允许 pending + 本人取消
  - `complete(orderId, remark)`: 保存 remark
- controller/WorkOrderController.java: 新增 `POST /order/cancel/{orderId}`

---

### Task 6: 更新 WebMvcConfig 拦截路径

加 `/dorm/**`、`/fault-type/**` 到拦截路径。

---

### Task 7: 更新 student.html

- 表单改为下拉选择宿舍和故障类型（调用 list API）
- 加图片上传按钮（选文件→上传→拿到 URL→存入 form）
- 列表 pending 状态加"取消"按钮
- 列表显示宿舍名、故障类型名（通过加载 dorm/faultType 映射）

---

### Task 8: 更新 teacher.html

- 完工按钮点击后弹出输入框，填写维修备注
- 列表显示备注内容

---

### Task 9: 编译 + 启动验证

编译重启，测试下拉列表、图片上传、取消工单、完工备注全流程。

---

### Task 10: 提交

```bash
git add ... && git commit -m "feat: 阶段2 — 下拉选择、图片上传、取消工单、维修备注"
```
