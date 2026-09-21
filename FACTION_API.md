# 阵营管理与英雄成员 API

更新日期：2026-09-21

## 通用约定

- 本地后端地址：`http://localhost:8080`
- 所有接口都需要请求头：`Authorization: Bearer <token>`
- JSON 请求需要：`Content-Type: application/json`
- 成功响应：`{ "code": 0, "message": "success", "data": ... }`
- 常见失败：HTTP 400 参数或业务错误、401 Token 无效、404 数据不存在、409 数据冲突、500 服务异常。

## 阵营接口

### 1. 查询有效阵营树

```http
GET /api/factions/list
GET /api/factions/list?keyword=德玛西亚
```

`keyword` 可选，按阵营名称或编码模糊匹配，并保留命中节点的祖先链。每个阵营节点包含直属成员 `members`，但人物表格需要分页或人物搜索时应调用英雄列表接口。

```json
{
  "id": 2,
  "parentId": 1,
  "name": "德玛西亚",
  "code": "demacia",
  "iconUrl": "https://dd.b.pvp.net/5_10_0/core/en_us/img/regions/icon-demacia.png",
  "themeColor": "#D4C28A",
  "leaderHeroId": 3,
  "description": "崇尚荣誉、正义与传统的强盛王国。",
  "level": 1,
  "sort": 1,
  "status": 1,
  "createTime": "2026-09-20 08:19:39",
  "updateTime": "2026-09-21 03:31:14",
  "children": [],
  "members": []
}
```

### 2. 阵营详情

```http
GET /api/factions/detail/{id}
```

返回完整阵营节点和直属 `members`。

### 3. 新增顶级或子阵营

```http
POST /api/factions/add
```

```json
{
  "parentId": 2,
  "name": "无畏先锋",
  "code": "dauntless-vanguard",
  "iconUrl": "https://example.com/icon.png",
  "themeColor": "#D4C28A",
  "leaderHeroId": null,
  "description": "德玛西亚精锐军团。",
  "sort": 1,
  "status": 1
}
```

- `parentId=0` 表示顶级阵营。
- 左侧树“新增子阵营”时，前端直接将当前节点 `id` 写入 `parentId` 即可，当前前端已经这样处理。
- `name`、`code` 必填；未删除阵营中 `code` 忽略大小写唯一。
- 如果传入未分配阵营的 `leaderHeroId`，后端会将该英雄自动挂载到新阵营；属于其他阵营时返回 HTTP 409。

### 4. 编辑阵营

```http
PUT /api/factions/edit/{id}
```

请求体与新增相同。父级不能是自身或自己的后代。`leaderHeroId` 必须属于当前阵营，未分配阵营的英雄会在任命时自动挂载。

### 5. 删除阵营

```http
DELETE /api/factions/delete/{id}
```

执行软删除。存在未删除子阵营或挂载英雄时返回 HTTP 409，前端应提示先迁移节点或成员。

### 6. 查询已删除阵营

```http
GET /api/factions/deleted-list
GET /api/factions/deleted-list?keyword=demacia
```

返回平铺数组，用于“回收站/已删除阵营”抽屉。`keyword` 按名称或编码模糊匹配。

### 7. 恢复阵营

```http
PUT /api/factions/restore/{id}
```

恢复软删除数据。父阵营已删除或当前编码已被其他有效阵营使用时，恢复会被阻止。

前端建议流程：删除成功后刷新 `/factions/list`；回收站调用 `/factions/deleted-list`；点击恢复后调用本接口并同时刷新两个列表。

### 8. 启用或停用阵营

```http
PUT /api/factions/status/{id}
```

```json
{
  "status": 0,
  "cascade": true
}
```

- `status`：`0` 停用，`1` 启用。
- `cascade=false` 或不传：只修改当前阵营。
- `cascade=true`：同步修改当前阵营、所有下属阵营及这些阵营中的英雄状态。

## 英雄与成员接口

英雄返回字段：

| 字段 | 类型 | 可空 | 说明 |
|---|---|---:|---|
| `id` | number | 否 | 本系统英雄档案 ID，后续详情、编辑、挂载等接口使用该值 |
| `riotChampionId` | string | 是 | Riot 稳定英雄标识，例如 `JarvanIV` |
| `dataVersion` | string | 是 | 最近一次 Riot 同步版本，例如 `16.18.1` |
| `avatarUrl` | string | 否 | Data Dragon 方形头像 URL |
| `name` | string | 否 | 人物姓名，例如“嘉文四世” |
| `nickname` | string | 否 | 英雄称号，例如“德玛西亚皇子” |
| `role` | string | 否 | 在当前阵营中的身份，例如“阵营领袖” |
| `factionId` | number | 是 | 当前挂载阵营 ID；`null` 表示未分配 |
| `gender` | number | 否 | `0` 未知、`1` 男、`2` 女 |
| `introduction` | string | 否 | 人物简介 |
| `status` | number | 否 | `1` 启用、`0` 停用 |
| `createTime` | string | 否 | 创建时间 |
| `updateTime` | string | 否 | 最后更新时间 |

### 1. 分页查询英雄

```http
GET /api/heroes/list?pageNum=1&pageSize=20
GET /api/heroes/list?factionId=2&keyword=嘉文&pageNum=1&pageSize=20
```

- `factionId` 可选，人物表格传当前选中阵营 ID。
- `keyword` 可选，匹配姓名、昵称或阵营身份。
- `pageNum` 默认 `1`；`pageSize` 默认 `20`、最大 `100`。

| Query 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---:|---|
| `factionId` | number | 否 | - | 只查询指定阵营的直属成员 |
| `keyword` | string | 否 | - | 模糊匹配 Riot 英文 ID、姓名、昵称或阵营身份 |
| `pageNum` | number | 否 | `1` | 页码，最小为 1 |
| `pageSize` | number | 否 | `20` | 每页数量，范围 1 到 100 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 3,
        "riotChampionId": "JarvanIV",
        "dataVersion": "16.18.1",
        "avatarUrl": "https://ddragon.leagueoflegends.com/cdn/16.18.1/img/champion/JarvanIV.png",
        "name": "嘉文四世",
        "nickname": "德玛西亚皇子",
        "role": "阵营领袖",
        "factionId": 2,
        "gender": 1,
        "introduction": "德玛西亚的现任国王与阵营最高领导者。",
        "status": 1,
        "createTime": "2026-09-21 03:31:14",
        "updateTime": "2026-09-21 03:31:14"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

### 2. 人物详情

```http
GET /api/heroes/detail/{id}
```

| Path 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `id` | number | 是 | 本系统英雄档案 ID，不是 `riotChampionId` |

成功返回单个英雄对象；不存在或已软删除时返回 HTTP 404、业务 `code=404`。

### 3. 新增英雄档案并挂载阵营

```http
POST /api/heroes/add
```

```json
{
  "avatarUrl": "https://ddragon.leagueoflegends.com/cdn/16.18.1/img/champion/Garen.png",
  "name": "盖伦",
  "nickname": "德玛西亚之力",
  "role": "军团先锋",
  "factionId": 2,
  "gender": 1,
  "introduction": "德玛西亚的无畏战士与军团先锋。",
  "status": 1
}
```

`name` 必填且在未删除英雄中忽略大小写唯一；`factionId` 可为 `null`，表示暂未分配阵营。

| Body 字段 | 类型 | 必填 | 默认值 | 校验/说明 |
|---|---|---:|---:|---|
| `avatarUrl` | string | 否 | `""` | 最长 255 |
| `name` | string | 是 | - | 最长 50，未删除英雄中忽略大小写唯一 |
| `nickname` | string | 否 | `""` | 最长 50 |
| `role` | string | 否 | `""` | 最长 50，表示在阵营中的身份 |
| `factionId` | number/null | 否 | `null` | 必须是有效阵营 ID；`null` 表示未分配 |
| `gender` | number | 否 | `0` | 只能是 `0`、`1`、`2` |
| `introduction` | string | 否 | `""` | 最长 2000 |
| `status` | number | 否 | `1` | 只能是 `0` 或 `1` |

`riotChampionId` 和 `dataVersion` 由 Riot 同步接口维护，普通新增/编辑请求不接收这两个字段。

### 4. 编辑英雄

```http
PUT /api/heroes/edit/{id}
```

请求体与新增相同。阵营领袖不能直接调整到其他阵营，必须先修改原阵营的 `leaderHeroId`。

编辑采用完整对象更新，请提交上表列出的全部当前值；没有修改的字段也应回传，避免被重置为默认值。

### 5. 删除英雄档案

```http
DELETE /api/heroes/delete/{id}
```

| Path 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `id` | number | 是 | 本系统英雄档案 ID |

执行软删除。英雄仍是任一阵营领袖时返回 HTTP 409。人物表格通常应优先使用“移出阵营”，不要把移除成员直接等同于删除档案。

### 6. 挂载已有英雄到阵营

```http
PUT /api/heroes/assign/{id}
```

```json
{
  "factionId": 2,
  "role": "军团先锋"
}
```

| Body 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `factionId` | number | 是 | 目标阵营 ID，必须大于 0 且阵营有效 |
| `role` | string | 否 | 在目标阵营中的身份，最长 50 |

适合“增加成员”时从未分配英雄列表选择已有档案。成功后刷新当前阵营的英雄列表。

### 7. 将成员移出阵营

```http
PUT /api/heroes/remove/{id}
```

只需要 Path 参数 `id`，没有请求体。

将 `factionId` 设为 `null` 并清空 `role`，保留英雄档案。英雄是阵营领袖时返回 HTTP 409。

### 8. 启用或停用英雄

```http
PUT /api/heroes/status/{id}
```

```json
{
  "status": 0
}
```

| Body 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `status` | number | 是 | `0` 停用、`1` 启用 |

### 9. 从 Riot 官方数据同步全部英雄

```http
POST /api/heroes/sync-riot?overwriteFaction=true
Authorization: Bearer <token>
```

没有请求体。接口运行时需要后端能够访问 Riot 官方 CDN。

| Query 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---:|---|
| `overwriteFaction` | boolean | 否 | `true` | `true` 按官方/剧情归属覆盖英雄阵营；`false` 保留已有非空阵营 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "version": "16.18.1",
    "totalChampions": 173,
    "officialFactionCount": 151,
    "inferredFactionCount": 19,
    "fallbackCount": 3,
    "inserted": 0,
    "updated": 173,
    "fallbackChampionIds": ["Alistar", "Smolder", "Zaahen"]
  }
}
```

- 英雄名称、称号、简介、头像和版本来自 Riot Data Dragon。
- 阵营优先使用 Riot Universe 的 `associated-faction-slug`。
- Universe 索引缺失但官方中文简介明确所属地区的英雄使用内置剧情映射。
- 无稳定城邦归属的英雄进入“符文之地”，并在 `fallbackChampionIds` 中返回。
- 同步不会覆盖人工维护的 `role`、`gender`、`status`，也不会恢复已软删除英雄。
- 同一个 Riot 英雄通过 `riotChampionId` 幂等更新，重复同步不会创建重复档案。

## 当前前端功能对照

| 功能 | 当前前端 | 后端接口 |
|---|---|---|
| 阵营树与选择 | 已有 | `GET /api/factions/list` |
| 新增子阵营并预填父级 | 已有 | `POST /api/factions/add` |
| 编辑阵营 | 已有 | `PUT /api/factions/edit/{id}` |
| 删除阵营 | 已有 | `DELETE /api/factions/delete/{id}` |
| 阵营详情 | 已有 | `GET /api/factions/detail/{id}` |
| 删除恢复/回收站 | 前端待接 | `GET /api/factions/deleted-list`、`PUT /api/factions/restore/{id}` |
| 独立启用/停用与级联选项 | 前端待接 | `PUT /api/factions/status/{id}` |
| 人物分页列表 | 前端待接 | `GET /api/heroes/list` |
| 人物详情 | 前端待接 | `GET /api/heroes/detail/{id}` |
| 新增人物 | 前端待接 | `POST /api/heroes/add` |
| 编辑人物 | 前端待接 | `PUT /api/heroes/edit/{id}` |
| 挂载已有成员 | 前端待接 | `PUT /api/heroes/assign/{id}` |
| 移出成员 | 前端待接 | `PUT /api/heroes/remove/{id}` |
| 删除人物档案 | 前端待接 | `DELETE /api/heroes/delete/{id}` |
| 人物启用/停用 | 前端待接 | `PUT /api/heroes/status/{id}` |
| 同步 Riot 全部英雄 | 可作为管理工具接入 | `POST /api/heroes/sync-riot` |

当前后端任务没有修改 `D:\project\front-admin`。前端对接时建议给“移出阵营”和“删除档案”使用不同按钮与确认文案。
