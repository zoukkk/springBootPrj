package com.example.mapper;

import com.example.pojo.BizFaction;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FactionMapper {
    @Select("select id, parent_id, name, code, icon_url, theme_color, leader_hero_id, description, sort, " +
            "status, del_flag, create_time, update_time from biz_faction " +
            "where del_flag = 0 order by sort, id")
    List<BizFaction> findAllActive();

    @Select("select id, parent_id, name, code, icon_url, theme_color, leader_hero_id, description, sort, " +
            "status, del_flag, create_time, update_time from biz_faction " +
            "where del_flag = 1 and (#{keyword} is null or trim(#{keyword}) = '' " +
            "or lower(name) like '%' || lower(trim(#{keyword})) || '%' " +
            "or lower(code) like '%' || lower(trim(#{keyword})) || '%') order by update_time desc, id desc")
    List<BizFaction> findDeleted(String keyword);

    @Select("select id, parent_id, name, code, icon_url, theme_color, leader_hero_id, description, sort, " +
            "status, del_flag, create_time, update_time from biz_faction " +
            "where id = #{id} and del_flag = 0 limit 1")
    BizFaction findById(Long id);

    @Select("select id, parent_id, name, code, icon_url, theme_color, leader_hero_id, description, sort, " +
            "status, del_flag, create_time, update_time from biz_faction " +
            "where lower(code) = lower(#{code}) and del_flag = 0 limit 1")
    BizFaction findByCode(String code);

    @Select("select id, parent_id, name, code, icon_url, theme_color, leader_hero_id, description, sort, " +
            "status, del_flag, create_time, update_time from biz_faction where id = #{id} limit 1")
    BizFaction findByIdIncludingDeleted(Long id);

    @Select("select count(*) from biz_faction where lower(code) = lower(#{code}) " +
            "and del_flag = 0 and (#{excludeId} is null or id != #{excludeId})")
    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    @Select("select count(*) from biz_faction where parent_id = #{parentId} and del_flag = 0")
    int countChildren(Long parentId);

    @Select("select count(*) from biz_hero where faction_id = #{factionId} and del_flag = 0")
    int countHeroes(Long factionId);

    @Select("with recursive descendants(id) as (" +
            "select id from biz_faction where parent_id = #{factionId} and del_flag = 0 " +
            "union all " +
            "select child.id from biz_faction child join descendants d on child.parent_id = d.id " +
            "where child.del_flag = 0" +
            ") select count(*) from descendants where id = #{candidateParentId}")
    int countDescendant(@Param("factionId") Long factionId,
                        @Param("candidateParentId") Long candidateParentId);

    @Insert("insert into biz_faction(parent_id, name, code, icon_url, theme_color, leader_hero_id, description, " +
            "sort, status, del_flag, create_time, update_time) values(" +
            "#{parentId}, #{name}, #{code}, #{iconUrl}, #{themeColor}, #{leaderHeroId}, #{description}, " +
            "#{sort}, #{status}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizFaction faction);

    @Update("update biz_faction set parent_id = #{parentId}, name = #{name}, code = #{code}, " +
            "icon_url = #{iconUrl}, theme_color = #{themeColor}, leader_hero_id = #{leaderHeroId}, " +
            "description = #{description}, sort = #{sort}, status = #{status}, " +
            "update_time = CURRENT_TIMESTAMP where id = #{id} and del_flag = 0")
    int update(BizFaction faction);

    @Update("update biz_faction set del_flag = 1, update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 0")
    int softDelete(Long id);

    @Update("update biz_faction set del_flag = 0, update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 1")
    int restore(Long id);

    @Update("update biz_faction set status = #{status}, update_time = CURRENT_TIMESTAMP " +
            "where id = #{id} and del_flag = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("with recursive descendants(id) as (" +
            "select id from biz_faction where id = #{id} and del_flag = 0 " +
            "union all select child.id from biz_faction child join descendants parent " +
            "on child.parent_id = parent.id where child.del_flag = 0" +
            ") update biz_faction set status = #{status}, update_time = CURRENT_TIMESTAMP " +
            "where id in (select id from descendants)")
    int updateStatusCascade(@Param("id") Long id, @Param("status") Integer status);
}
