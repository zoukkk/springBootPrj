CREATE TABLE IF NOT EXISTS zip_st_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    nickname TEXT,
    email TEXT,
    user_pic TEXT,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zip_st_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_name TEXT NOT NULL,
    category_alias TEXT NOT NULL,
    create_user INTEGER NOT NULL,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zip_st_article (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    cover_img TEXT,
    state TEXT NOT NULL,
    category_id INTEGER NOT NULL,
    create_user INTEGER NOT NULL,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zip_wang_test (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    age INTEGER,
    gender INTEGER,
    phone TEXT
);

CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    nickname TEXT,
    avatar TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    status INTEGER NOT NULL DEFAULT 1,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id INTEGER NOT NULL DEFAULT 0,
    name TEXT NOT NULL,
    path TEXT NOT NULL,
    component TEXT NOT NULL,
    icon TEXT,
    sort INTEGER NOT NULL DEFAULT 0,
    visible INTEGER NOT NULL DEFAULT 1,
    status INTEGER NOT NULL DEFAULT 1,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_id INTEGER NOT NULL,
    menu_id INTEGER NOT NULL,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER NOT NULL DEFAULT 0,
    UNIQUE(role_id, menu_id)
);

-- 英雄联盟宇宙阵营/城邦表
CREATE TABLE IF NOT EXISTS biz_faction (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id INTEGER NOT NULL DEFAULT 0,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    icon_url TEXT DEFAULT '',
    theme_color TEXT DEFAULT '#C89B3C',
    leader_hero_id INTEGER DEFAULT NULL,
    description TEXT,
    sort INTEGER DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 1,
    del_flag INTEGER NOT NULL DEFAULT 0,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 英雄档案与阵营成员关系表
CREATE TABLE IF NOT EXISTS biz_hero (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    riot_champion_id TEXT DEFAULT NULL,
    data_version TEXT DEFAULT NULL,
    avatar_url TEXT DEFAULT '',
    name TEXT NOT NULL,
    nickname TEXT DEFAULT '',
    role TEXT DEFAULT '',
    faction_id INTEGER DEFAULT NULL,
    gender INTEGER NOT NULL DEFAULT 0,
    introduction TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    del_flag INTEGER NOT NULL DEFAULT 0,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_user_role_user ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_role ON sys_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_parent ON sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_biz_faction_parent_id ON biz_faction(parent_id);
CREATE INDEX IF NOT EXISTS idx_biz_faction_status_del ON biz_faction(status, del_flag);
CREATE INDEX IF NOT EXISTS idx_biz_hero_faction_status ON biz_hero(faction_id, status, del_flag);

INSERT OR IGNORE INTO sys_role (id, code, name) VALUES
    (1, 'admin', '超级管理员'),
    (2, 'user', '普通用户');

INSERT OR IGNORE INTO sys_menu
    (id, parent_id, name, path, component, icon, sort, visible, status)
VALUES
    (1, 0, '首页', '/dashboard', 'DashboardView', 'HomeFilled', 1, 1, 1),
    (2, 0, '系统管理', '/system', 'SystemLayout', 'Setting', 2, 1, 1),
    (3, 2, '用户管理', '/system/users', 'UserView', 'User', 1, 1, 1),
    (4, 2, '角色管理', '/system/roles', 'RoleView', 'UserFilled', 2, 1, 1),
    (5, 2, '菜单管理', '/system/menus', 'MenuView', 'Menu', 3, 1, 1),
    (6, 0, '阵营管理', '/depts', 'FactionView', 'OfficeBuilding', 3, 1, 1);

INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 1);

INSERT OR IGNORE INTO zip_st_user
    (id, username, password, nickname, email, user_pic, create_time, update_time)
VALUES
    (2, 'zhansan', 'e10adc3949ba59abbe56e057f20f883e', 'zsoo', 'zs11@qq.com', 'www.baidu.com', '2026-07-21 11:03:17', '2026-07-28 16:25:12'),
    (3, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '', '', '', '2026-08-27 16:55:35', '2026-08-27 16:55:35');

INSERT OR IGNORE INTO zip_st_category
    (id, category_name, category_alias, create_user, create_time, update_time)
VALUES
    (1, '飞机', '射手', 2, '2026-07-29 14:57:10', '2026-07-29 19:23:25');

INSERT OR IGNORE INTO zip_st_article
    (id, title, content, cover_img, state, category_id, create_user, create_time, update_time)
VALUES
    (3, '67岁许家印被判无期徒刑', '许家印案一审宣判，67岁的许家印被判无期徒刑，很多网友不解：为何没判死刑呢？', 'https://www.baidu.com', '已发布', 3, 2, '2026-08-20 15:34:51', '2026-08-20 15:34:51'),
    (4, '驾照色盲图24人仅6人答对', '驾照体检色盲图识别24人仅6人答对：连AI也过不了关', 'https://www.baidu.com', '草稿', 4, 2, '2026-08-21 10:27:55', '2026-08-21 10:27:55'),
    (5, '重庆两名行人疑似触电昏迷', '8月25日20时35分，两名行人在重庆渝中区嘉滨路88号人行天桥下人行道疑似触电倒地昏迷', 'https://www.baidu.com', '已发布', 1, 2, '2026-08-26 10:17:37', '2026-08-26 10:17:37'),
    (6, '霍尔木兹海峡临时航线谅解', '据央视新闻报道，伊朗方面25日消息，伊朗外交部副部长加里巴巴迪表示，根据伊朗与阿曼就霍尔木兹海峡航线新达成的谅解', 'https://www.baidu.com', '已发布', 1, 2, '2026-08-26 10:18:12', '2026-08-26 10:18:12');

INSERT OR IGNORE INTO zip_wang_test (id, name, age, gender, phone) VALUES
    (1, '白眉鹰王', 55, 1, '18800000000'),
    (2, '金毛狮王', 45, 1, '18800000001'),
    (3, '青翼蝠王', 38, 1, '18800000002'),
    (4, '紫衫龙王', 42, 2, '18800000003'),
    (5, '光明左使', 37, 1, '18800000004'),
    (6, '光明右使', 48, 1, '18800000005');
