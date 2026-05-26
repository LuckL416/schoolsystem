---
version: alpha
name: dorm-repair-design-system
description: 校园宿舍报修系统设计语言 — 干净白底、校园蓝主色的专业且亲切风格。服务于学生(报修/评价)、师傅(接单/维修)、管理员(看板/管理)三类角色。数据密集页面保持清晰可读，学生端温暖友好。
---

## Visual Theme & Atmosphere

纯白底色搭配浅灰分区，校园蓝 `#2563eb` 作为唯一彩色主调。整体干净、可信赖，适合校园场景。不使用暗色模式，不用炫光渐变。数据看板使用柔和色阶热力图，状态标签使用语义色 pill。

**Key Characteristics:**
- 纯白底色，浅灰分区 — 干净克制
- 校园蓝 `#2563eb` 主 CTA，翠绿成功态，琥珀/红警示
- Inter 字族，display 500 weight 紧凑有力，body 400 weight 舒适可读
- 6px 按钮圆角 — 方中带圆，不幼稚
- 12px 卡片圆角 + 极简 1px 描边
- 数据看板用色阶表达，不靠渐变装饰

## Colors

### Brand & Accent
- **primary** `#2563eb` — 校园蓝，主按钮、链接、品牌标识
- **primary-hover** `#1d4ed8` — hover 加深
- **primary-soft** `#dbeafe` — 浅蓝背景(选中行、信息提示)
- **primary-pressed** `#1e40af` — 按下态

### Semantic
- **success** `#16a34a` — 完成、通过、好评
- **success-soft** `#dcfce7` — 成功浅底
- **warning** `#d97706` — 警告、中等评价
- **warning-soft** `#fef3c7` — 警告浅底
- **danger** `#dc2626` — 差评预警、删除、紧急
- **danger-soft** `#fee2e2` — 危险浅底
- **info** `#0891b2` — 信息提示
- **info-soft** `#cffafe` — 信息浅底

### Surface
- **canvas** `#ffffff` — 页面底色
- **surface** `#f8fafc` — 卡片、面板底色
- **surface-alt** `#f1f5f9` — 交替行、hover 行
- **surface-hover** `#e2e8f0` — 悬停加深

### Border
- **border** `#e2e8f0` — 默认描边
- **border-strong** `#cbd5e1` — 强调描边、聚焦环

### Text
- **ink** `#0f172a` — 正文标题
- **ink-secondary** `#475569` — 次要文字
- **ink-muted** `#94a3b8` — 辅助/占位文字
- **on-primary** `#ffffff` — 主色上的文字
- **on-dark** `#ffffff` — 深色底上的文字

## Typography

### Font Family
- **Display + UI**: Inter (Google Fonts CDN), fallback: `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto`
- **Mono**: `'JetBrains Mono', 'Fira Code', monospace` — 编号、代码

```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```

### Hierarchy

| Token | Size | Weight | Line Height | Letter Spacing | Use |
|---|---|---|---|---|---|
| display-lg | 32px | 700 | 1.2 | -0.5px | 页面大标题 |
| display-md | 24px | 600 | 1.3 | -0.3px | 区块标题 |
| heading | 20px | 600 | 1.35 | -0.1px | 卡片标题 |
| subhead | 16px | 600 | 1.4 | 0 | 列表标题 |
| body | 15px | 400 | 1.5 | 0 | 正文、表格 |
| body-sm | 13px | 400 | 1.45 | 0 | 辅助信息 |
| caption | 12px | 400 | 1.4 | 0 | 标注、时间 |
| button | 14px | 500 | 1.2 | 0 | 按钮文字 |
| mono | 13px | 400 | 1.5 | 0 | 编号、代码 |

## Spacing

| Token | Value |
|---|---|
| xs | 4px |
| sm | 8px |
| md | 12px |
| lg | 16px |
| xl | 20px |
| xxl | 24px |
| section | 32px |
| huge | 48px |

## Rounded

| Token | Value | Use |
|---|---|---|
| sm | 4px | 输入框、小标签 |
| md | 6px | 按钮(签名半径) |
| lg | 10px | 卡片、面板 |
| xl | 14px | 模态框 |
| full | 9999px | 状态 pill、头像 |

## Elevation

| Level | Treatment | Use |
|---|---|---|
| 0 | 无阴影，1px border | 默认卡片 |
| 1 | `0 1px 3px rgba(0,0,0,0.06)` | 悬浮卡片 |
| 2 | `0 4px 16px rgba(0,0,0,0.08)` | 下拉菜单、模态框 |
| 3 | `0 8px 32px rgba(0,0,0,0.12)` | 模态框遮罩 |

## Components

### Button Sizes
- **sm**: 6px 12px padding, 13px font
- **md**: 8px 16px padding, 14px font (默认)
- **lg**: 10px 20px padding, 15px font

### button-primary
- bg `primary`, text `on-primary`, rounded `md`, weight 500
- hover: bg `primary-hover`
- pressed: bg `primary-pressed`
- focus: 2px `primary-soft` ring

### button-secondary
- bg `canvas`, text `ink`, border `border-strong`, rounded `md`
- hover: bg `surface`

### button-danger
- bg `danger`, text `on-dark`, rounded `md`
- hover: opacity 0.9

### button-ghost
- bg transparent, text `ink-secondary`, rounded `md`
- hover: bg `surface`

### Status Pills (pill-sm / pill-md)
- `pending`: bg `warning-soft`, text `warning`
- `processing`: bg `info-soft`, text `info`
- `completed`: bg `success-soft`, text `success`
- `urgent`: bg `danger-soft`, text `danger`
- rounded `full`, padding 2px 10px, caption font

### card-default
- bg `canvas`, border `border`, rounded `lg`, padding `xl`
- hover: border `border-strong`

### text-input
- bg `canvas`, border `border`, rounded `sm`, padding 8px 12px
- focus: border `primary`, ring 3px `primary-soft`
- placeholder: text `ink-muted`

### table-default
- header: bg `surface`, text `ink-secondary`, weight 500, caption size
- row: border-bottom `border`
- row hover: bg `surface-alt`
- cell padding: 10px 14px

### badge-count
- bg `danger`, text `on-dark`, rounded `full`, min-width 18px, height 18px
- 用于通知未读数

### Top Nav Bar
- bg `canvas`, border-bottom `border`, height 56px
- 左侧 logo/标题，右侧用户区(头像+姓名+角色pill+退出)

### Dashboard Stat Card
- bg `canvas`, border `border`, rounded `lg`, padding `xl`
- 图标 + 数字(display-md) + 标签(body-sm ink-muted)

### Heatmap Cell (大屏看板)
- 使用 primary 色阶: `#dbeafe`(低) → `#93bbfd`(中) → `#2563eb`(高) → `#1e40af`(极高)

## Do's and Don'ts

### Do
- 主色仅用于 CTA 按钮、链接、选中态 — 稀缺使用
- 状态用语义色 pill — 一目了然
- 数据看板用色阶表达密度，不用装饰性渐变
- 卡片用极简 1px 描边 + 微阴影区分层级
- 过渡动画 150ms ease — 快而克制
- 表格隔行用 `surface-alt` 微区分

### Don't
- 不要用暗色模式
- 不要用第二种主色 — 校园蓝足矣
- 不要 pill 形按钮 — 按钮用 6px 圆角
- 不要重阴影或渐变背景
- 不要纯黑 `#000` 文字 — 始终用 `ink` `#0f172a`
- 不要省略聚焦环 — 每次聚焦都要有 3px primary-soft ring

## Responsive

| Breakpoint | Width | Changes |
|---|---|---|
| Desktop | ≥ 1280px | 默认布局，侧栏 + 主内容 |
| Tablet | 768–1279px | 侧栏收起，表格横向滚动 |
| Mobile | < 768px | 单列，卡片全宽 |

表格在移动端添加 `overflow-x: auto` 包裹，不截断内容。
