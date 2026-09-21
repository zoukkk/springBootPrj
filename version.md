# 版本记录

## 2026-09-21 - 阵营恢复、状态管理与英雄成员 CRUD

- 新增阵营归档查询、软删除恢复和独立状态切换接口；状态切换可选择级联下属阵营及英雄。
- 新增英雄档案分页查询、详情、新增、修改、软删除、启停、挂载阵营和移出阵营接口。
- 阵营领袖增加归属校验；未分配英雄可在任命时自动挂载，其他阵营英雄会被阻止。
- 新增 `HeroFlowTest`，覆盖人物 CRUD、成员关系、阵营删除保护、恢复和级联停用。
- 全量测试通过：共 15 个测试，13 个通过、2 个跳过、0 个失败；8080 真实请求确认英雄列表、英雄详情和阵营归档列表可用。
- 完整联调文档见 `FACTION_API.md`。

## 2026-09-21 - Riot 全英雄与阵营同步

- 增加 `riot_champion_id` 和 `data_version` 字段及兼容旧 SQLite 数据库的自动迁移。
- 新增 `POST /api/heroes/sync-riot`，从 Riot Data Dragon 和 Universe 幂等同步英雄资料、头像和阵营归属。
- 新增以绪塔尔、虚空两个阵营，符文之地下现有 13 个地区阵营。
- 已按 Data Dragon `16.18.1` 写入 173 位英雄；151 位使用 Universe 直接归属，19 位按 Riot 中文剧情资料补充，3 位无稳定城邦归属并保留在符文之地。
- `FACTION_API.md` 已补充英雄字段表、所有请求参数、校验规则、响应示例和同步接口说明。

## 2026-09-20 - 阵营 CDN 数据与语义化接口

- 阵营菜单显示名称为“阵营管理”，前端路由映射路径改为 `/depts`，组件映射键仍为 `FactionView`。
- 阵营接口改为语义化路径：`add`、`list`、`edit`、`delete`、`detail`。
- 根据 Riot 官方 LoR Data Dragon 地区数据补充符文之地及主要地区，并写入官方地区徽章 CDN URL。
- 皮尔特沃夫和祖安在官方数据中属于同一个 `PiltoverZaun` 地区，因此共用同一官方徽章。
- 初始化是幂等的：缺少的地区会新增；已有地区仅在 `icon_url` 为空时补齐，不覆盖手工配置。
- 全量测试执行成功：共 13 个测试，11 个通过，2 个跳过，0 失败。
- 正式 SQLite 已完成初始化并在 8080 验证：`list` 返回 1 个根节点和 11 个地区，`detail` 返回 CDN 图标，菜单名称返回“阵营管理”、路径为 `/depts`。
- 新增 `biz_hero` 英雄档案表，支持头像、姓名、昵称、阵营角色、所属阵营、性别、介绍、状态和软删除。
- `factions/list` 和 `factions/detail` 的每个阵营节点新增 `members` 字段；已初始化德玛西亚成员盖伦、拉克丝、嘉文四世，其中嘉文四世为“阵营领袖”。
- 阵营删除增加成员保护：存在挂载英雄时返回 HTTP 409。
- 2026-09-21 在 8080 真实验证：按 `keyword=德玛西亚` 查询返回祖先链、德玛西亚节点及 3 名成员，成员 ID 为 `1,2,3`，领袖 ID 为 `3`。

接口调用方式：

```text
GET    /api/factions/list?keyword={nameOrCode}
GET    /api/factions/detail/{id}
POST   /api/factions/add
PUT    /api/factions/edit/{id}
DELETE /api/factions/delete/{id}
```

以上接口都需要 `Authorization: Bearer <token>`；成功响应业务 `code=0`。`list` 返回阵营树，其他响应结构继续使用 `Result<T>`。

请求参数明细：

```http
GET /api/factions/list?keyword=德玛西亚
Authorization: Bearer <token>
```

`keyword` 可选，同时匹配 `name` 和 `code`，返回命中节点及其祖先链。

阵营节点的 `members` 为直属英雄成员，不递归混入子阵营成员。单条成员结构如下：

```json
{
  "id": 3,
  "avatarUrl": "https://ddragon.leagueoflegends.com/cdn/16.17.1/img/champion/JarvanIV.png",
  "name": "嘉文四世",
  "nickname": "德玛西亚皇子",
  "role": "阵营领袖",
  "factionId": 2,
  "gender": 1,
  "introduction": "德玛西亚的现任国王与阵营最高领导者。",
  "status": 1
}
```

其中 `id` 是英雄档案 ID（`biz_hero.id`），不是登录账号 `sys_user.id`；`gender` 当前约定为 `0` 未知、`1` 男、`2` 女。

```json
POST /api/factions/add
Content-Type: application/json

{
  "parentId": 1,
  "name": "德玛西亚北境",
  "code": "demacia-north",
  "iconUrl": "https://example.com/icon.png",
  "themeColor": "#D4C28A",
  "leaderHeroId": null,
  "description": "阵营描述",
  "sort": 1,
  "status": 1
}
```

新增和修改请求体字段含义：`parentId` 父节点 ID，顶级传 `0`；`name` 阵营名称，必填且最长 50；`code` 阵营编码，必填且最长 50、未删除数据中忽略大小写唯一；`iconUrl` 图标 URL，最长 255；`themeColor` 主题色，最长 20；`leaderHeroId` 预留的领袖英雄 ID；`description` 描述；`sort` 非负排序值；`status` 只能是 `0` 或 `1`。

```http
PUT /api/factions/edit/2
Content-Type: application/json

{ "parentId": 1, "name": "德玛西亚", "code": "demacia", "iconUrl": "", "themeColor": "#D4C28A", "leaderHeroId": null, "description": "崇尚荣誉的王国", "sort": 1, "status": 1 }
```

```http
GET /api/factions/detail/2
DELETE /api/factions/delete/2
```

成功响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 2,
    "parentId": 1,
    "name": "德玛西亚",
    "code": "demacia",
    "iconUrl": "https://dd.b.pvp.net/5_10_0/core/en_us/img/regions/icon-demacia.png",
    "themeColor": "#D4C28A",
    "leaderHeroId": null,
    "description": "崇尚荣誉、正义与传统的强盛王国。",
    "level": 1,
    "sort": 1,
    "status": 1,
    "children": []
  }
}
```

## 2026-09-20 - 英雄联盟宇宙阵营模型

- 将通用部门模型收敛为英雄联盟宇宙阵营模型，SQLite 业务表改为 `biz_faction`；启动时会先迁移已有 `sys_dept` 数据再清理旧表。
- 字段改为 `icon_url`、`theme_color`、`leader_hero_id`，并保留父级、描述、排序、状态、软删除和通用时间字段。
- 新增一级菜单“阵营管理”：前端路径现为 `/depts`，组件映射键 `FactionView`，图标 `OfficeBuilding`。
- 最初实现阵营树、详情、新增、修改和软删除接口；接口路径已在上方版本更新为语义化形式。
- 树查询支持 `keyword` 按阵营名称/编码模糊过滤，搜索结果保留完整祖先链；响应中的 `level` 由树层级计算，不冗余存库。
- 启动时幂等初始化“符文之地”根节点，以及德玛西亚、诺克萨斯、皮尔特沃夫、祖安、艾欧尼亚五个一级城邦。
- 新增与修改会校验未删除阵营的 `code`（忽略大小写）；修改时禁止父级指向自身或任意后代。
- 删除使用 `del_flag=1` 软删除，存在未删除直接子阵营时返回 HTTP 409 并阻断删除。
- 全量测试执行成功：共 12 个测试，10 个通过，2 个跳过，0 失败。
- 新包已在正式 SQLite 库完成迁移，并在 8080 通过登录、完整阵营树、中文关键字过滤和动态菜单 HTTP 冒烟验证。
- Spring Boot 可执行包改为 `target/springbootProject-1.0-SNAPSHOT-exec.jar`，避免运行中的后端锁住 Maven 普通 JAR；快捷启动脚本已同步。

接口调用时统一携带：`Authorization: Bearer <token>`。

树查询：

```http
GET /api/factions/list
GET /api/factions/list?keyword=demacia
```

新增/修改请求体字段：

```json
{
  "parentId": 0,
  "name": "德玛西亚",
  "code": "demacia",
  "iconUrl": "",
  "themeColor": "#D4C28A",
  "leaderHeroId": null,
  "description": "崇尚荣誉、正义与传统的强盛王国。",
  "sort": 0,
  "status": 1
}
```

其中新增使用 `POST /api/factions/add`；修改使用 `PUT /api/factions/edit/{id}`；详情使用 `GET /api/factions/detail/{id}`；软删除使用 `DELETE /api/factions/delete/{id}`。

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
