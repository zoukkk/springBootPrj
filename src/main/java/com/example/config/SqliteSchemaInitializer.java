package com.example.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class SqliteSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public SqliteSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateLegacyDeptTable();
        addColumnIfMissing("biz_hero", "riot_champion_id", "TEXT DEFAULT NULL");
        addColumnIfMissing("biz_hero", "data_version", "TEXT DEFAULT NULL");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_biz_hero_riot_id " +
                "ON biz_hero(riot_champion_id) WHERE riot_champion_id IS NOT NULL");
        jdbcTemplate.execute("""
                CREATE TRIGGER IF NOT EXISTS trg_biz_faction_update_time
                AFTER UPDATE OF parent_id, name, code, icon_url, theme_color, leader_hero_id,
                                description, sort, status, del_flag
                ON biz_faction
                FOR EACH ROW
                BEGIN
                    UPDATE biz_faction SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
                END
                """);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from pragma_table_info('" + table + "') where name = ?",
                Integer.class, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void migrateLegacyDeptTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from sqlite_master where type = 'table' and name = 'sys_dept'",
                Integer.class);
        if (tableCount == null || tableCount == 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT OR IGNORE INTO biz_faction(
                    id, parent_id, name, code, icon_url, theme_color, leader_hero_id,
                    description, sort, status, del_flag, create_time, update_time)
                SELECT id, parent_id, name, code, icon, color, leader_id,
                       description, sort, status, del_flag, create_time, update_time
                FROM sys_dept
                """);
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_sys_dept_update_time");
        jdbcTemplate.execute("DROP TABLE sys_dept");
    }
}
