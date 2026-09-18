# AGENTS.md — 企业人员管理系统开发规范

> 版本：v0.5
>
> 当前阶段：登录 + 动态路由（菜单）
>
> 用途：约束 AI Agent 开发行为，保证产出一致、可维护。

---

## 一、技术栈（固定）

### 后端

- Java 17 + Spring Boot 3.x
- 现有 MyBatis（不迁移到 MyBatis-Plus）
- SQLite
- Redis（存 Token）
- 认证体系允许替换为 Spring Security + JWT，但必须保持旧模块可用

### 技术栈边界

- 以当前项目技术栈和工程结构为基础，不为本阶段主动替换数据访问框架、数据库或前端技术栈。
- 允许为完成认证与授权引入 Spring Security，并替换现有登录拦截逻辑。
- 旧的用户、分类、文章和上传模块暂时保留；新认证体系不得造成旧模块的无意义破坏。

### 前端

- Vue 3
- Vite
- Element Plus
- 前端已存在于 `D:\project\front-admin`，禁止在后端仓库中重复新建前端工程。
- 前后端必须分开维护；当前后端任务只允许修改 `springBootPrj`。
- 需要联调时只允许读取 `D:\project\front-admin`，禁止从当前后端任务修改任何前端代码。
- 即使用户在当前后端任务中提出前端修改要求，也必须拒绝执行，并提醒用户切换到独立的前端任务处理。

---

## 二、动态路由方案

- 菜单存数据库，通过菜单管理维护
- 后端按角色过滤，返回当前用户可见菜单树
- 前端用菜单树 + 本地路由表生成动态路由
- `sys_role_menu` 是后端菜单权限的主要依据
- 前端 `meta.roles` 仅用于前端路由辅助判断，不作为后端权限依据

### 角色 code 对照表（前后端必须一致）

| code | 名称 |
|---|---|
| admin | 超级管理员 |
| user | 普通用户 |

### 超级管理员

- `admin` 默认拥有全部有效菜单权限。
- `user` 按 `sys_role_menu` 配置返回菜单。
- 用户拥有多个角色时，菜单权限取并集并去重。
- 可见子菜单的父级目录必须自动补齐，菜单同级按 `sort` 升序返回。
- 当前阶段不实现按钮级权限、数据权限等更细粒度权限。

### 最小菜单模型

- `sys_menu` 当前阶段只保留动态路由所需的最小字段：`id`、`parent_id`、`name`、`path`、`component`、`icon`、`sort`、`visible`、`status`、通用字段。
- 后端返回驼峰命名的菜单树，每个节点最少包含：`id`、`parentId`、`name`、`path`、`component`、`icon`、`sort`、`children`。
- `component` 只是前端 `async-routes.ts` 中的本地映射键，不是可直接执行的 import 路径。
- 当前阶段不实现外链、页签缓存、面包屑等扩展属性，后续按需增加。

---

## 三、通用约定

### 数据库

- 通用字段：`id`、`create_time`、`update_time`、`del_flag`
- 逻辑删除：`del_flag`（0正常 1删除），禁止物理删除
- 表前缀：系统表 `sys_`，业务表 `biz_`
- 当前阶段核心权限表：
  - `sys_user`
  - `sys_role`
  - `sys_user_role`
  - `sys_menu`
  - `sys_role_menu`

### 后端

- 统一响应：`Result<T> { code, message, data }`，`code=0` 成功
- 响应码：0成功 / 1业务失败 / 401未登录 / 403无权限 / 404不存在 / 500异常
- 接口前缀 `/api`，RESTful
- 分页参数：`pageNum`、`pageSize`；返回：`{ list, total, pageNum, pageSize }`
- 密码使用 BCrypt，禁止明文存储；敏感字段禁止返回
- 统一异常处理，业务异常抛 `BusinessException`
- 包结构按现有项目模块拆分，不擅自调整项目整体结构
- 成功响应的业务 `code` 必须为 `0`；失败时可同时使用合理的 HTTP 状态码和业务 `code`。

### 配置清理

- 项目默认数据源为 SQLite，可清理已不再使用的 MySQL、Druid 配置和相关依赖，但不将 MyBatis 替换为其他框架。
- 数据库密码、JWT 密钥、OSS 密钥等敏感信息不得以明文提交，应改用环境变量或本地配置。

---

## 四、认证与登录

### 接口

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/userinfo
GET  /api/auth/menus
```

### 接口访问规则

- `POST /api/auth/register`、`POST /api/auth/login` 允许匿名访问。
- `POST /api/auth/logout`、`GET /api/auth/userinfo`、`GET /api/auth/menus` 不限制 `admin` / `user` 角色，但必须携带有效 Token。
- “任何人都可访问”表示不做角色限制，不表示匿名访问可以读取用户信息或菜单。
- 当前阶段不为了验收 403 额外创建虚假的管理员接口；403 留待后续真实的角色受限功能验证。

### 登录

Request：

```json
{
  "username": "adming",
  "password": "123456"
}
```

Response：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "xxxxx",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

### 注册

- `POST /api/auth/register` 使用 JSON 请求，只接收 `username`、`password`。
- 注册用户写入 `sys_user`，密码使用 BCrypt，并默认分配 `user` 角色。
- 注册成功后不自动登录，客户端继续调用 `/api/auth/login` 获取 Token。
- 用户名重复返回 HTTP 409 和业务 `code=1`。
- 旧 `/user/register` 仅为旧模块兼容接口，写入 `zip_st_user`，不得与新认证接口混用。

### JWT 约定

- 请求头使用：`Authorization: Bearer <token>`
- JWT 至少包含当前用户 `userId`、`username`
- JWT 设置有效期
- JWT 禁止存储密码等敏感信息
- Token 无效、过期或未携带时返回 401
- 用户已登录但无访问权限时返回 403
- 后端从 JWT / SecurityContext 获取当前用户身份，不信任前端传递的 `userId`、`role` 等身份信息

### Redis Token

- 登录成功后将 Token 写入 Redis
- Redis Token 有效期与 JWT 保持一致
- 用户退出登录时删除对应 Token
- 请求认证时除验证 JWT 外，同时检查 Redis 中 Token 是否有效
- 同一用户最多同时保留 2 个有效登录会话。
- 第 3 次登录成功时，使该用户最早的有效 Token 失效，新 Token 正常写入 Redis。
- 退出登录只删除当前 Token，不影响该用户的另一个有效会话。

### 初始账号

- 默认管理员用户名：`adming`
- 默认密码：`123456`（数据库中只存储 BCrypt 哈希）
- 默认角色：`admin`
- 初始账号仅用于本地开发和验收，后续应支持修改初始密码。
- 不额外内置普通用户默认账号；普通用户菜单权限的自动化测试应创建临时 `user` 角色账号，并保证测试数据可重复执行。

---

## 五、动态路由流程（硬性）

前端                          后端

│
├── POST /api/auth/login ────>│ 1. 校验账号密码
│                             │ 2. 生成 JWT
│                             │ 3. Token 写入 Redis
│<──────── { token } ─────────┤
│
│ 存 token                    │
│
├── GET /api/auth/userinfo ──>│ 从 JWT 解析当前用户
│                             │ 查询用户 + 角色
│<──── { ..., roles } ────────┤
│
├── GET /api/auth/menus ─────>│ 根据角色查询可见菜单
│<────── 菜单树 ──────────────┤
│
│ 菜单树 + async-routes.ts
│ 生成动态路由
│
│ addRoute 注入
│
│ 404 通配路由最后添加
│
└── 退出 → 清 token + 重置路由 + 清 store

### 菜单与前端路由映射

- 数据库 `sys_menu` 保存菜单及路由相关信息。
- 前端通过本地 `async-routes.ts` 将菜单中的 `component` 映射到实际 Vue 页面。
- 数据库中的 `component` 不应直接作为任意前端 JS import 路径执行。
- 后端返回的菜单树已经完成权限过滤，前端不得依赖前端传入的角色信息重新决定后端权限。

---

## 六、当前阶段 Agent 开发约束

### 只实现当前需求

当前阶段只关注：

- 注册
- 登录
- JWT 认证
- Redis Token 管理
- 用户与角色关联
- 菜单与角色关联
- 动态菜单
- 动态路由
- 退出登录


### Agent 行为

- 优先复用现有代码和项目结构，不重复创建已有功能。
- 不擅自更换技术栈或引入不必要的第三方依赖。
- 不为了当前简单需求引入过度复杂的架构。
- 修改前先检查现有项目结构及相关代码。
- 修改后进行必要的编译/构建检查，并说明修改了哪些文件及测试结果。
- 如果现有项目结构与本规范冲突，应优先保持现有项目可运行，并向用户说明冲突，不要擅自大范围重构。

---

## 七、当前阶段验收标准

- [ ] 默认管理员 `adming` 可以正常登录并获得 `admin` 角色
- [ ] 密码错误不能登录
- [ ] 登录成功获得 JWT，并写入 Redis
- [ ] 同一用户可同时保留 2 个有效会话，第 3 次登录使最早 Token 失效
- [ ] 请求携带 JWT 可以获取当前用户信息
- [ ] `/auth/menus` 返回当前用户有权限的菜单树
- [ ] admin 可以看到全部有效菜单
- [ ] user 只能看到配置给其角色的菜单
- [ ] 前端能够根据菜单树生成动态路由和菜单
- [ ] 浏览器刷新后动态路由能够正常恢复
- [ ] 退出登录后 Token 失效
- [ ] 无效/过期 Token 返回 401
- [ ] 404 通配路由正常
- [ ] 后端项目能够正常编译/启动
- [ ] 与 `D:\project\front-admin` 联调时，前端能够按菜单树生成动态路由
