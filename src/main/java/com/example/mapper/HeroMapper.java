package com.example.mapper;

import com.example.pojo.BizHero;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface HeroMapper {
    @Select("select id, riot_champion_id, data_version, avatar_url, name, nickname, role, faction_id, gender, introduction, " +
            "status, del_flag, create_time, update_time from biz_hero " +
            "where faction_id = #{factionId} and del_flag = 0 order by id")
    List<BizHero> findActiveByFactionId(Long factionId);

    @Select("select id, riot_champion_id, data_version, avatar_url, name, nickname, role, faction_id, gender, introduction, " +
            "status, del_flag, create_time, update_time from biz_hero " +
            "where id = #{id} and del_flag = 0 limit 1")
    BizHero findById(Long id);

    @Select("select id, riot_champion_id, data_version, avatar_url, name, nickname, role, faction_id, gender, introduction, " +
            "status, del_flag, create_time, update_time from biz_hero where del_flag = 0 " +
            "and (#{factionId} is null or faction_id = #{factionId}) " +
            "and (#{keyword} is null or trim(#{keyword}) = '' " +
            "or lower(riot_champion_id) like '%' || lower(trim(#{keyword})) || '%' " +
            "or lower(name) like '%' || lower(trim(#{keyword})) || '%' " +
            "or lower(nickname) like '%' || lower(trim(#{keyword})) || '%' " +
            "or lower(role) like '%' || lower(trim(#{keyword})) || '%') " +
            "order by id limit #{pageSize} offset #{offset}")
    List<BizHero> findPage(@Param("factionId") Long factionId,
                           @Param("keyword") String keyword,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    @Select("select count(*) from biz_hero where del_flag = 0 " +
            "and (#{factionId} is null or faction_id = #{factionId}) " +
            "and (#{keyword} is null or trim(#{keyword}) = '' " +
            "or lower(riot_champion_id) like '%' || lower(trim(#{keyword})) || '%' " +
            "or lower(name) like '%' || lower(trim(#{keyword})) || '%' " +
            "or lower(nickname) like '%' || lower(trim(#{keyword})) || '%' " +
            "or lower(role) like '%' || lower(trim(#{keyword})) || '%')")
    long countPage(@Param("factionId") Long factionId, @Param("keyword") String keyword);

    @Select("select count(*) from biz_hero where lower(name) = lower(#{name}) and del_flag = 0 " +
            "and (#{excludeId} is null or id != #{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Select("select count(*) from biz_faction where leader_hero_id = #{heroId} and del_flag = 0")
    int countLeaderReferences(Long heroId);

    @Select("select id, riot_champion_id, data_version, avatar_url, name, nickname, role, faction_id, gender, introduction, " +
            "status, del_flag, create_time, update_time from biz_hero " +
            "where riot_champion_id = #{riotChampionId} and del_flag = 0 limit 1")
    BizHero findByRiotChampionId(String riotChampionId);

    @Select("select id, riot_champion_id, data_version, avatar_url, name, nickname, role, faction_id, gender, introduction, " +
            "status, del_flag, create_time, update_time from biz_hero " +
            "where lower(name) = lower(#{name}) and del_flag = 0 order by id limit 1")
    BizHero findByName(String name);

    @Insert("insert into biz_hero(riot_champion_id, data_version, avatar_url, name, nickname, role, faction_id, gender, introduction, " +
            "status, del_flag, create_time, update_time) values(#{riotChampionId}, #{dataVersion}, #{avatarUrl}, #{name}, " +
            "#{nickname}, #{role}, #{factionId}, #{gender}, #{introduction}, #{status}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizHero hero);

    @Update("update biz_hero set avatar_url = #{avatarUrl}, name = #{name}, nickname = #{nickname}, " +
            "role = #{role}, faction_id = #{factionId}, gender = #{gender}, introduction = #{introduction}, " +
            "status = #{status}, update_time = CURRENT_TIMESTAMP where id = #{id} and del_flag = 0")
    int update(BizHero hero);

    @Update("update biz_hero set riot_champion_id = #{riotChampionId}, data_version = #{dataVersion}, " +
            "avatar_url = #{avatarUrl}, name = #{name}, nickname = #{nickname}, faction_id = #{factionId}, " +
            "introduction = #{introduction}, update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 0")
    int updateOfficialData(BizHero hero);

    @Update("update biz_hero set faction_id = #{factionId}, role = #{role}, update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 0")
    int updateFaction(BizHero hero);

    @Update("update biz_hero set faction_id = null, role = '', update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 0")
    int removeFaction(Long id);

    @Update("update biz_hero set status = #{status}, update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("update biz_hero set status = #{status}, update_time = CURRENT_TIMESTAMP where del_flag = 0 " +
            "and faction_id in (with recursive descendants(id) as (" +
            "select id from biz_faction where id = #{factionId} and del_flag = 0 " +
            "union all select child.id from biz_faction child join descendants parent " +
            "on child.parent_id = parent.id where child.del_flag = 0" +
            ") select id from descendants)")
    int updateStatusByFactionTree(@Param("factionId") Long factionId, @Param("status") Integer status);

    @Update("update biz_hero set del_flag = 1, update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 0")
    int softDelete(Long id);
}
