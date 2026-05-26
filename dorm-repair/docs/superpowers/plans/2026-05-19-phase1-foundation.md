# 阶段1：地基（安全+架构）— 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 JWT 统一拦截器、全局异常处理器、前端 API 公共层，消除重复代码和安全漏洞。

**Architecture:** 新增 JwtInterceptor 在请求进入 Controller 前统一校验 Token，通过 request attribute 传递 userId 和 role；新增 GlobalExceptionHandler 统一异常返回格式；前端抽取 api.js 封装 axios 调用，4 个 HTML 页面引用它。

**Tech Stack:** Spring Boot 3.2.4, MyBatis-Plus 3.5.7, JJWT 0.11.5, Vue 3 CDN, Axios CDN

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `common/BusinessException.java` | 新建 | 业务异常类，含 code 和 message |
| `common/GlobalExceptionHandler.java` | 新建 | 全局异常捕获，统一返回 Result |
| `common/JwtInterceptor.java` | 新建 | 拦截请求，校验 JWT，注入 userId/role |
| `common/WebMvcConfig.java` | 新建 | 注册 JwtInterceptor 到 Spring MVC |
| `common/JwtUtil.java` | 修改 | 新增 getRole() 方法 |
| `controller/AuthController.java` | 修改 | 用 @RequestAttribute 替代手动解析 token |
| `controller/WorkOrderController.java` | 修改 | 用 @RequestAttribute 替代手动解析 token，/all 加鉴权 |
| `controller/StatsController.java` | 修改 | 加鉴权（之前无任何保护） |
| `static/api.js` | 新建 | 前端公共请求封装 |
| `login.html` | 修改 | 引用 api.js，替换 axios 调用 |
| `student.html` | 修改 | 引用 api.js，替换 axios 调用 |
| `teacher.html` | 修改 | 引用 api.js，替换 axios 调用 |
| `index.html` | 修改 | 引用 api.js，替换 axios 调用 |

---

### Task 1: 创建 BusinessException 业务异常类

**Files:**
- Create: `src/main/java/com/school/dormrepair/common/BusinessException.java`

- [ ] **Step 1: 编写 BusinessException**

```java
package com.school.dormrepair.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(500, message);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw compile -o`
Expected: BUILD SUCCESS

---

### Task 2: 创建 GlobalExceptionHandler 全局异常处理器

**Files:**
- Create: `src/main/java/com/school/dormrepair/common/GlobalExceptionHandler.java`

- [ ] **Step 1: 编写 GlobalExceptionHandler**

```java
package com.school.dormrepair.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        Result<Void> r = new Result<>();
        r.setCode(e.getCode());
        r.setMessage(e.getMessage());
        return r;
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        Result<Void> r = new Result<>();
        r.setCode(500);
        r.setMessage("服务器内部错误");
        return r;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw compile -o`
Expected: BUILD SUCCESS

---

### Task 3: JwtUtil 新增 getRole 方法

**Files:**
- Modify: `src/main/java/com/school/dormrepair/common/JwtUtil.java`

- [ ] **Step 1: 在 JwtUtil 中添加 getRole 方法**

在 `getUserId` 方法之后添加（大约第 43 行）：

```java
public String getRole(String token) {
    Claims claims = Jwts.parserBuilder()
            .setSigningKey(getKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    return claims.get("role", String.class);
}
```

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw compile -o`
Expected: BUILD SUCCESS

---

### Task 4: 创建 JwtInterceptor 拦截器

**Files:**
- Create: `src/main/java/com/school/dormrepair/common/JwtInterceptor.java`

- [ ] **Step 1: 编写 JwtInterceptor**

```java
package com.school.dormrepair.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或Token格式错误");
        }
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validate(token)) {
            throw new BusinessException(401, "Token已过期或无效");
        }
        request.setAttribute("userId", jwtUtil.getUserId(token));
        request.setAttribute("role", jwtUtil.getRole(token));
        return true;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw compile -o`
Expected: BUILD SUCCESS

---

### Task 5: 创建 WebMvcConfig 注册拦截器

**Files:**
- Create: `src/main/java/com/school/dormrepair/common/WebMvcConfig.java`

- [ ] **Step 1: 编写 WebMvcConfig**

```java
package com.school.dormrepair.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/order/**", "/stats/**", "/auth/info")
                .excludePathPatterns("/auth/login", "/auth/register");
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw compile -o`
Expected: BUILD SUCCESS

---

### Task 6: 更新 AuthController 使用 request attribute

**Files:**
- Modify: `src/main/java/com/school/dormrepair/controller/AuthController.java`

- [ ] **Step 1: 重写 AuthController**

```java
package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.User;
import com.school.dormrepair.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<String> login(
            @RequestParam String username,
            @RequestParam String password
    ) {
        return userService.login(username, password);
    }

    @GetMapping("/info")
    public Result<User> info(@RequestAttribute("userId") Long userId) {
        return userService.getUserInfo(userId);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw clean compile -o`
Expected: BUILD SUCCESS

---

### Task 7: 更新 WorkOrderController 使用 request attribute

**Files:**
- Modify: `src/main/java/com/school/dormrepair/controller/WorkOrderController.java`

- [ ] **Step 1: 重写 WorkOrderController**

```java
package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    // 学生提交报修
    @PostMapping("/submit")
    public Result<String> submit(
            @RequestBody WorkOrder workOrder,
            @RequestAttribute("userId") Long userId
    ) {
        workOrder.setStudentId(userId);
        return workOrderService.submit(workOrder);
    }

    // 学生查看我的报修
    @GetMapping("/my")
    public Result<List<WorkOrder>> myOrders(
            @RequestAttribute("userId") Long userId
    ) {
        return workOrderService.listByStudent(userId);
    }

    // 管理员/师傅查看全部工单（拦截器已保证登录即可访问）
    @GetMapping("/all")
    public Result<List<WorkOrder>> allOrders() {
        return workOrderService.allList();
    }

    // 师傅接单
    @PostMapping("/accept/{orderId}")
    public Result<String> accept(
            @PathVariable Long orderId,
            @RequestAttribute("userId") Long userId
    ) {
        return workOrderService.accept(orderId, userId);
    }

    // 师傅完工
    @PostMapping("/complete/{orderId}")
    public Result<String> complete(@PathVariable Long orderId) {
        return workOrderService.complete(orderId);
    }

    // 学生评价
    @PostMapping("/evaluate/{orderId}")
    public Result<String> evaluate(
            @PathVariable Long orderId,
            @RequestParam Integer star
    ) {
        return workOrderService.evaluate(orderId, star);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw clean compile -o`
Expected: BUILD SUCCESS

---

### Task 8: 更新 StatsController 加鉴权

**Files:**
- Modify: `src/main/java/com/school/dormrepair/controller/StatsController.java`

- [ ] **Step 1: 更新 StatsController（无需改代码，仅确认路径被拦截器覆盖）**

StatsController 的 `/stats/**` 路径已在 WebMvcConfig 中配置为拦截路径，无需修改其源码。但为代码清晰，加一行注释说明鉴权由拦截器处理：

在类声明上方添加注释：`// 鉴权由 JwtInterceptor 统一处理`

- [ ] **Step 2: 编译验证**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw compile -o`
Expected: BUILD SUCCESS

---

### Task 9: 创建前端 API 公共层

**Files:**
- Create: `src/main/resources/static/api.js`

- [ ] **Step 1: 编写 api.js**

```javascript
// 宿舍报修系统 — 公共 API 层
const API_BASE = 'http://localhost:8080/api';

const api = {
    getToken() {
        return localStorage.getItem('token');
    },

    headers() {
        const token = this.getToken();
        return token ? { Authorization: 'Bearer ' + token } : {};
    },

    async get(url, params) {
        return axios.get(API_BASE + url, { headers: this.headers(), params });
    },

    async post(url, data) {
        return axios.post(API_BASE + url, data, { headers: this.headers() });
    },

    // 登录用（不需要 token）
    async loginPost(url, data) {
        return axios.post(API_BASE + url, data, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });
    },

    goLogin() {
        localStorage.removeItem('token');
        location.href = 'login.html';
    }
};
```

- [ ] **Step 2: 确认文件位置正确**

Run: `ls "F:/学习/schoolsystem/dorm-repair/src/main/resources/static/api.js"`
Expected: 文件存在

---

### Task 10: 更新 login.html 使用 api.js

**Files:**
- Modify: `login.html`

- [ ] **Step 1: 在 login.html 中替换 axios 调用**

将原有 script 部分替换为（页面 HTML 结构不变，只改 script）：

```html
<script src="api.js"></script>
<script>
const { createApp } = Vue
createApp({
    data() {
        return {
            role: 'student',
            username: '',
            password: '',
            errorMsg: ''
        }
    },
    methods: {
        async login() {
            this.errorMsg = ''
            if (!this.username || !this.password) {
                this.errorMsg = '请输入用户名和密码'
                return
            }
            try {
                const res = await api.loginPost('/auth/login',
                    `username=${encodeURIComponent(this.username)}&password=${encodeURIComponent(this.password)}`
                )
                localStorage.setItem('token', res.data.data)

                // 获取用户信息，验证角色
                const infoRes = await api.get('/auth/info')
                const user = infoRes.data.data
                if (user.role !== this.role) {
                    this.errorMsg = '身份不匹配：您是"' + this.getRoleName(user.role)
                        + '"，请切换正确的身份后重新登录'
                    localStorage.removeItem('token')
                    return
                }
                // 根据角色跳转
                const pages = { student:'student.html', teacher:'teacher.html', admin:'index.html' }
                location.href = pages[this.role] || 'student.html'
            } catch (e) {
                if (e.response && e.response.data) {
                    this.errorMsg = '登录失败：' + (e.response.data.message || '未知错误')
                } else {
                    this.errorMsg = '无法连接服务器，请确认后端已启动'
                }
            }
        },
        getRoleName(role) {
            return { student:'学生', teacher:'师傅', admin:'管理员' }[role] || role
        }
    }
}).mount('#app')
</script>
```

- [ ] **Step 2: 确认登录功能可用**

启动后端后，用浏览器打开 login.html，测试三种角色登录均能正确跳转。

---

### Task 11: 更新 student.html 使用 api.js

**Files:**
- Modify: `student.html`

- [ ] **Step 1: 更新 student.html**

在 `<script>` 前加 `<script src="api.js"></script>`，并将 script 部分替换为：

```html
<script src="api.js"></script>
<script>
const { createApp } = Vue
createApp({
    data() {
        return {
            form: { dormId:null, faultTypeId:null, description:'' },
            list: [],
            user: {}
        }
    },
    mounted() {
        if (!api.getToken()) { api.goLogin(); return }
        this.loadUser()
        this.loadList()
    },
    methods: {
        async loadUser() {
            const res = await api.get('/auth/info')
            this.user = res.data.data
        },
        async loadList() {
            const res = await api.get('/order/my')
            this.list = res.data.data || []
        },
        async submit() {
            if (!this.form.dormId || !this.form.faultTypeId || !this.form.description) {
                alert('请填写完整信息')
                return
            }
            await api.post('/order/submit', this.form)
            alert('提交成功！')
            this.form.description = ''
            this.loadList()
        },
        async evaluate(id, star) {
            await api.post(`/order/evaluate/${id}?star=${star}`)
            alert('评价成功！')
            this.loadList()
        },
        statusText(status) {
            const map = { pending:'待处理', processing:'处理中', completed:'已完成' }
            return map[status] || status
        },
        statusClass(status) {
            return 'tag-' + (status || '')
        },
        formatTime(t) {
            if (!t) return ''
            return t.replace('T',' ').substring(0,19)
        },
        goBack() { api.goLogin() }
    }
}).mount('#app')
</script>
```

页面结构不变，但将 `<button class="btn-back" @click="goBack">` 的 `goBack` 改为调用 `api.goLogin()`。

---

### Task 12: 更新 teacher.html 使用 api.js

**Files:**
- Modify: `teacher.html`

- [ ] **Step 1: 更新 teacher.html**

在 `<script>` 前加 `<script src="api.js"></script>`，并将 script 部分替换为：

```html
<script src="api.js"></script>
<script>
const { createApp } = Vue
createApp({
    data() {
        return {
            list: [],
            user: {}
        }
    },
    mounted() {
        if (!api.getToken()) { api.goLogin(); return }
        this.loadUser()
        this.loadList()
    },
    methods: {
        async loadUser() {
            const res = await api.get('/auth/info')
            this.user = res.data.data
        },
        async loadList() {
            const res = await api.get('/order/all')
            this.list = res.data.data || []
        },
        async accept(id) {
            await api.post(`/order/accept/${id}`)
            alert('接单成功！')
            this.loadList()
        },
        async complete(id) {
            await api.post(`/order/complete/${id}`)
            alert('完工成功！')
            this.loadList()
        },
        statusText(status) {
            const map = { pending:'待处理', processing:'处理中', completed:'已完成' }
            return map[status] || status
        },
        statusClass(status) {
            return 'tag-' + (status || '')
        },
        formatTime(t) {
            if (!t) return ''
            return t.replace('T',' ').substring(0,19)
        },
        goBack() { api.goLogin() }
    }
}).mount('#app')
</script>
```

---

### Task 13: 更新 index.html 使用 api.js

**Files:**
- Modify: `index.html`

- [ ] **Step 1: 更新 index.html**

在 `<script>` 前加 `<script src="api.js"></script>`，并将 script 部分替换为：

```html
<script src="api.js"></script>
<script>
const { createApp } = Vue
createApp({
    data() {
        return {
            stats: {},
            list: [],
            user: {}
        }
    },
    mounted() {
        if (!api.getToken()) { api.goLogin(); return }
        this.loadUser()
        this.refresh()
    },
    methods: {
        async loadUser() {
            const res = await api.get('/auth/info')
            this.user = res.data.data
        },
        async loadStats() {
            const res = await api.get('/stats/dashboard')
            this.stats = res.data.data
        },
        async loadList() {
            const res = await api.get('/order/all')
            this.list = res.data.data || []
        },
        async refresh() {
            await Promise.all([this.loadStats(), this.loadList()])
        },
        exportExcel() {
            window.open(API_BASE + '/stats/export')
        },
        statusText(status) {
            const map = { pending:'待处理', processing:'处理中', completed:'已完成' }
            return map[status] || status
        },
        statusClass(status) {
            return 'tag-' + (status || '')
        },
        formatTime(t) {
            if (!t) return ''
            return t.replace('T',' ').substring(0,19)
        },
        goBack() { api.goLogin() }
    }
}).mount('#app')
</script>
```

- [ ] **Step 2: 删除 index.html 中硬编码的 token**

移除 `<script>` 中 `data()` 里的 `token: 'eyJh...'` 硬编码，改为从 `api.getToken()` 动态获取。

---

### Task 14: 完整编译 + 启动验证

- [ ] **Step 1: 停掉旧进程**

Run: `netstat -ano | grep ":8080" | grep LISTENING`
如果有进程占用，记下 PID 并 kill：
`taskkill //PID <PID> //F`

- [ ] **Step 2: 重新编译**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw clean compile -o`
Expected: BUILD SUCCESS

- [ ] **Step 3: 启动后端**

Run: `cd "F:/学习/schoolsystem/dorm-repair" && ./mvnw spring-boot:run -o` (后台)
Expected: Spring Boot 启动在 8080 端口

- [ ] **Step 4: 验证鉴权生效 — 不带 token 访问受保护接口应返回 401**

Run: `curl -s http://localhost:8080/api/order/all`
Expected: `{"code":401,"message":"未登录或Token格式错误"}`

- [ ] **Step 5: 验证带 token 访问正常**

Run:
```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login?username=admin&password=123456" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'])")
curl -s http://localhost:8080/api/order/all -H "Authorization: Bearer $TOKEN"
```
Expected: 返回工单列表 JSON (code=200)

- [ ] **Step 6: 验证 login.html 工作正常**

在浏览器打开 `login.html`，用三种角色登录测试：
- 学生(student/123456) → 跳转 student.html
- 师傅(teacher/123456) → 跳转 teacher.html
- 管理员(admin/123456) → 跳转 index.html

---

### Task 15: 提交阶段1代码

- [ ] **Step 1: 提交**

```bash
cd "F:/学习/schoolsystem/dorm-repair"
git add src/main/java/com/school/dormrepair/common/BusinessException.java
git add src/main/java/com/school/dormrepair/common/GlobalExceptionHandler.java
git add src/main/java/com/school/dormrepair/common/JwtInterceptor.java
git add src/main/java/com/school/dormrepair/common/WebMvcConfig.java
git add src/main/java/com/school/dormrepair/common/JwtUtil.java
git add src/main/java/com/school/dormrepair/controller/AuthController.java
git add src/main/java/com/school/dormrepair/controller/WorkOrderController.java
git add src/main/java/com/school/dormrepair/controller/StatsController.java
git add src/main/resources/static/api.js
git add login.html
git add student.html
git add teacher.html
git add index.html
git commit -m "feat: 阶段1 — JWT拦截器、全局异常处理、前端API公共层

- 新增 JwtInterceptor 统一鉴权，拦截 /order, /stats, /auth/info
- 新增 GlobalExceptionHandler 统一异常返回格式
- 新增 BusinessException 支持业务异常码
- Controller 中用 @RequestAttribute 替代手动 token 解析
- 前端抽取 api.js，4 个页面去除重复的 axios 调用
- /order/all 和 /stats/* 鉴权漏洞已修复"
```
