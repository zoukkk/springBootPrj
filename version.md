# 版本记录

## 2026-09-18 - 后端快捷启动脚本

- 新增 `start-backend.bat`，双击即可启动后端 JAR 到 8080。
- 脚本会检测端口占用，避免重复启动，并将后台日志写入 `backend.log`。

## 2026-09-17 - 新认证注册接口

- 新增匿名接口 `POST /api/auth/register`，使用 JSON 请求写入 `sys_user`。
- 新用户密码使用 BCrypt 保存，注册成功后默认绑定 `user` 角色，可直接使用 `/api/auth/login` 登录。
- 用户名限制为 2–18 位非空白字符，密码限制为 6–32 位；重复用户名返回 HTTP 409、业务 `code=1`。
- 保留旧 `/user/register` 兼容接口，但旧接口仍写入 `zip_st_user`，不能与新认证登录混用。
- `mvn clean test` 执行成功：共 11 个测试，9 个通过，2 个跳过，0 失败。
- 正式 SQLite 库已通过真实 HTTP 完成注册、登录、用户信息和菜单验证，并保留测试账号 `registertest / 123456`（`user` 角色）。

注册调用示例：

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "123456"
}
```

## 2026-09-17 - 菜单数据修复

- 修复 SQLite 中已经写入的中文菜单乱码，启动时会幂等校正现有菜单记录，无需删除数据库。
- 保留“首页”，新增“系统管理”父级菜单，并设置“用户管理”“角色管理”“菜单管理”三个子菜单。
- 后端集成测试通过：共 10 个测试，8 个通过，2 个跳过，0 失败。
- 成品 JAR 已重新打包并运行于 `http://127.0.0.1:8080`，真实 `/api/auth/menus` 响应已验证中文和树形层级正确。
- 前端白色毛玻璃侧栏及本地路由改动因前端目录写入权限审核服务异常而暂未落盘。

## 2026-09-17 - 规范 v0.5

### 本次修改

- 确定保留现有 Java 17、Spring Boot 3.x、MyBatis、SQLite 和 Redis 技术栈，不迁移到 MyBatis-Plus。
- 允许用 Spring Security + JWT 替换现有认证逻辑，但必须兼容旧模块。
- 明确前端项目位置为 `D:\project\front-admin`，不在后端仓库内重复创建前端。
- 增加最小菜单模型、多角色菜单并集、父级目录补齐和排序规则。
- 明确认证接口访问规则：登录允许匿名，其余接口不限角色但需有效 Token。
- 成功响应继续使用 `code=0`，不强制所有失败响应使用 HTTP 200。
- 默认管理员为 `adming / 123456`，对应 `admin` 角色，密码使用 BCrypt 存储。
- 不内置额外普通用户账号；`user` 角色菜单测试由自动化测试数据完成。
- 同一用户最多同时保留 2 个有效登录会话；第 3 次登录使最早 Token 失效。
- 授权清理旧 MySQL/Druid 配置和明文敏感信息。

### 实施结果

- 新增 `sys_user`、`sys_role`、`sys_user_role`、`sys_menu`、`sys_role_menu` 五张系统表，保留原有 `zip_st_*` 表和旧业务数据。
- 应用启动时以 BCrypt 创建缺失的默认管理员 `adming / 123456`，并关联 `admin` 角色。
- 新增 Spring Security 无状态过滤链、Bearer JWT 认证、Redis Token 有效性检查和统一 401/403 JSON 响应。
- 实现单用户最多两个有效会话；第三次登录淘汰最早 Token，退出仅撤销当前 Token。
- 实现用户信息、多角色菜单并集、管理员全菜单、普通用户菜单过滤和菜单树构建。
- 保留旧 `/user`、`/article`、`/category`、`/upload` 接口，旧登录产生的裸 Token 仍可访问旧接口；新 Token 也会向旧模块提供当前用户上下文。
- 清理 MySQL、Druid、Kotlin 依赖和配置，统一使用 SQLite；JWT、Redis 和 OSS 配置改用环境变量。
- 新增 HTTP 集成测试，覆盖错误密码、Bearer 校验、用户信息、管理员/普通用户菜单、双会话、退出、404 和旧接口兼容。

### 配置说明

- 生产或持久化环境应设置 `JWT_SECRET`；未设置时，后端每次启动会生成随机密钥，重启后旧 JWT 失效。
- Redis 可通过 `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE` 配置，默认为 `localhost:6379/0`。
- OSS 使用 `OSS_ENDPOINT`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`OSS_BUCKET_NAME`、`OSS_REGION`；未配置时请勿调用上传接口。

### 自测记录

- SQLite 初始化脚本已在隔离的测试数据库执行通过，已验证 5 张系统表、2 个角色和 4 条菜单数据。
- `pom.xml` XML 结构和 Git 空白错误检查通过。
- Maven 主代码和测试代码编译通过。
- `mvn test` 执行成功：共 10 个测试，8 个通过，2 个会写默认 Redis 库的旧演示测试按设计跳过，0 失败。
- 首次增量测试因 `target/classes` 中残留已删除的 `webConfig.class` 而误返 401；执行 `mvn clean test` 清理旧产物后已排除，新建或删除配置类后应优先使用 `clean test`。
- `mvn -DskipTests package` 打包成功，已生成可执行 JAR `target/springbootProject-1.0-SNAPSHOT.jar`。
- 成品 JAR 已在 `http://127.0.0.1:8081` 启动，并完成真实 HTTP 冒烟测试：登录、用户信息、菜单和退出均正常，成功响应保持 `code=0`，退出后的 Token 返回 HTTP 401。
- 双会话真实验证通过：连续登录三次后，最早 Token 返回 HTTP 401，后两个 Token 均返回 HTTP 200；验证完成后已退出有效测试会话。

### 接口调用方式

> 以下接口已在后端实现；调用受保护接口时，必须使用登录响应中的 Token。

#### 1. 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "adming",
  "password": "123456"
}
```

成功响应：

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

#### 2. 获取当前用户

```http
GET /api/auth/userinfo
Authorization: Bearer <token>
```

#### 3. 获取当前用户菜单

```http
GET /api/auth/menus
Authorization: Bearer <token>
```

成功响应中 `data` 为菜单树数组，节点包含 `id`、`parentId`、`name`、`path`、`component`、`icon`、`sort`、`children`。

#### 4. 退出登录

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

退出只使当前 Token 失效，不影响同一用户的另一个有效会话。

### 前端联调现状

- 已确认前端项目 `D:\project\front-admin` 存在，本次仅做了只读核对，未修改其代码。
- 前端当前仍调用旧接口 `POST /api/user/login`，且以 query 参数传递用户名和密码；后续联调时需改为本文档中的 JSON 请求。
- 前端当前在 `Authorization` 中直接发送裸 Token；后续需改为 `Bearer <token>`。
- 前端当前使用静态路由，其自身 `AGENTS.md` 仍记录为“不依赖后端返回菜单树”，与本阶段的数据库菜单方案冲突。开始前端动态路由改造时，需先同步修订前端规范。
