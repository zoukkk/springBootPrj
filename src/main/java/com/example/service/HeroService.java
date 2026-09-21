package com.example.service;

import com.example.dto.AssignHeroRequest;
import com.example.dto.CreateHeroRequest;
import com.example.dto.HeroStatusRequest;
import com.example.dto.UpdateHeroRequest;
import com.example.vo.HeroVO;
import com.example.vo.PageResult;

public interface HeroService {
    PageResult<HeroVO> list(Long factionId, String keyword, int pageNum, int pageSize);

    HeroVO getById(Long id);

    HeroVO create(CreateHeroRequest request);

    HeroVO update(Long id, UpdateHeroRequest request);

    void delete(Long id);

    HeroVO assign(Long id, AssignHeroRequest request);

    HeroVO remove(Long id);

    HeroVO updateStatus(Long id, HeroStatusRequest request);
}
