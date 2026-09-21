package com.example.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1)
public class FactionDataInitializer implements ApplicationRunner {
    private static final String CDN_ROOT = "https://dd.b.pvp.net/5_10_0/core/en_us/img/regions/";

    private final JdbcTemplate jdbcTemplate;

    public FactionDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long runeterraId = ensureFaction(0L, "符文之地", "runeterra",
                CDN_ROOT + "icon-runeterra.png", "#C89B3C",
                "英雄联盟宇宙中的世界与各城邦共同体。", 1);
        long demaciaId = ensureFaction(runeterraId, "德玛西亚", "demacia",
                CDN_ROOT + "icon-demacia.png", "#D4C28A",
                "崇尚荣誉、正义与传统的强盛王国。", 1);
        ensureFaction(runeterraId, "诺克萨斯", "noxus",
                CDN_ROOT + "icon-noxus.png", "#8E1B1B",
                "以力量与功绩为核心的强大帝国。", 2);
        ensureFaction(runeterraId, "皮尔特沃夫", "piltover",
                CDN_ROOT + "icon-piltoverzaun.png", "#4FA3C7",
                "以进步、贸易与海克斯科技闻名的城邦。", 3);
        ensureFaction(runeterraId, "祖安", "zaun",
                CDN_ROOT + "icon-piltoverzaun.png", "#6B8E23",
                "位于皮尔特沃夫下方、充满炼金科技的地下城邦。", 4);
        ensureFaction(runeterraId, "艾欧尼亚", "ionia",
                CDN_ROOT + "icon-ionia.png", "#C96FA6",
                "重视自然、灵性与平衡的初生之土。", 5);
        ensureFaction(runeterraId, "弗雷尔卓德", "freljord",
                CDN_ROOT + "icon-freljord.png", "#79BFE1",
                "由严酷寒冬与古老部族塑造的北方冻土。", 6);
        ensureFaction(runeterraId, "暗影岛", "shadow-isles",
                CDN_ROOT + "icon-shadowisles.png", "#39A884",
                "被黑雾笼罩、亡灵徘徊的破碎群岛。", 7);
        ensureFaction(runeterraId, "巨神峰", "targon",
                CDN_ROOT + "icon-targon.png", "#8A78C2",
                "直入天际、与星灵力量紧密相连的神圣山峰。", 8);
        ensureFaction(runeterraId, "恕瑞玛", "shurima",
                CDN_ROOT + "icon-shurima.png", "#D7A83D",
                "正在沙海中复兴的古老帝国。", 9);
        ensureFaction(runeterraId, "比尔吉沃特", "bilgewater",
                CDN_ROOT + "icon-bilgewater.png", "#4B7F78",
                "商人、猎手与海盗汇聚的危险港城。", 10);
        ensureFaction(runeterraId, "班德尔城", "bandle-city",
                CDN_ROOT + "icon-bandlecity.png", "#9BBF54",
                "约德尔人的神秘家园。", 11);
        ensureFaction(runeterraId, "以绪塔尔", "ixtal",
                "", "#2E8B57",
                "隐于丛林深处、精通元素魔法的古老文明。", 12);
        ensureFaction(runeterraId, "虚空", "void",
                "", "#6A3D9A",
                "来自现实彼端、试图吞噬符文之地的未知领域。", 13);
        seedDemaciaHeroes(demaciaId);
        jdbcTemplate.update("update biz_faction set leader_hero_id = 3, update_time = CURRENT_TIMESTAMP " +
                "where id = ? and (leader_hero_id is null or leader_hero_id = 0)", demaciaId);
    }

    private void seedDemaciaHeroes(long demaciaId) {
        seedHero(1L, "https://ddragon.leagueoflegends.com/cdn/16.18.1/img/champion/Garen.png",
                "盖伦", "德玛西亚之力", "军团先锋", demaciaId, 1,
                "德玛西亚的无畏战士与军团先锋。", 1);
        seedHero(2L, "https://ddragon.leagueoflegends.com/cdn/16.18.1/img/champion/Lux.png",
                "拉克丝", "光辉女郎", "皇家法师", demaciaId, 2,
                "能够驾驭光明魔法的德玛西亚贵族。", 1);
        seedHero(3L, "https://ddragon.leagueoflegends.com/cdn/16.18.1/img/champion/JarvanIV.png",
                "嘉文四世", "德玛西亚皇子", "阵营领袖", demaciaId, 1,
                "德玛西亚的现任国王与阵营最高领导者。", 1);
    }

    private void seedHero(long id, String avatarUrl, String name, String nickname, String role,
                          long factionId, int gender, String introduction, int status) {
        jdbcTemplate.update("""
                insert or ignore into biz_hero(
                    id, avatar_url, name, nickname, role, faction_id, gender,
                    introduction, status, del_flag, create_time, update_time)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, avatarUrl, name, nickname, role, factionId, gender, introduction, status);
        jdbcTemplate.update("""
                update biz_hero set faction_id = ?, role = ?, update_time = CURRENT_TIMESTAMP
                where id = ? and del_flag = 0
                """, factionId, role, id);
    }

    private long ensureFaction(long parentId, String name, String code, String iconUrl,
                               String themeColor, String description, int sort) {
        List<Long> existingIds = jdbcTemplate.query(
                "select id from biz_faction where lower(code) = lower(?) order by id limit 1",
                (rs, rowNum) -> rs.getLong("id"), code);
        if (!existingIds.isEmpty()) {
            long id = existingIds.get(0);
            jdbcTemplate.update("""
                    update biz_faction set icon_url = ?, update_time = CURRENT_TIMESTAMP
                    where id = ? and (icon_url is null or trim(icon_url) = '')
                    """, iconUrl, id);
            return id;
        }
        jdbcTemplate.update("""
                insert into biz_faction(
                    parent_id, name, code, icon_url, theme_color, leader_hero_id,
                    description, sort, status, del_flag, create_time, update_time)
                values (?, ?, ?, ?, ?, null, ?, ?, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, parentId, name, code, iconUrl, themeColor, description, sort);
        return jdbcTemplate.queryForObject(
                "select id from biz_faction where lower(code) = lower(?) order by id limit 1",
                Long.class, code);
    }
}
