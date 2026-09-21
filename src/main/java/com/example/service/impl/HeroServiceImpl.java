package com.example.service.impl;

import com.example.dto.AssignHeroRequest;
import com.example.dto.CreateHeroRequest;
import com.example.dto.HeroStatusRequest;
import com.example.dto.UpdateHeroRequest;
import com.example.exception.BusinessException;
import com.example.mapper.FactionMapper;
import com.example.mapper.HeroMapper;
import com.example.pojo.BizHero;
import com.example.service.HeroService;
import com.example.vo.HeroVO;
import com.example.vo.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HeroServiceImpl implements HeroService {
    private final HeroMapper heroMapper;
    private final FactionMapper factionMapper;

    public HeroServiceImpl(HeroMapper heroMapper, FactionMapper factionMapper) {
        this.heroMapper = heroMapper;
        this.factionMapper = factionMapper;
    }

    @Override
    public PageResult<HeroVO> list(Long factionId, String keyword, int pageNum, int pageSize) {
        validateFaction(factionId);
        int offset = (pageNum - 1) * pageSize;
        List<HeroVO> list = heroMapper.findPage(factionId, keyword, offset, pageSize).stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(list, heroMapper.countPage(factionId, keyword), pageNum, pageSize);
    }

    @Override
    public HeroVO getById(Long id) {
        return toVO(requireHero(id));
    }

    @Override
    @Transactional
    public HeroVO create(CreateHeroRequest request) {
        validateFaction(request.factionId());
        String name = request.name().trim();
        validateNameAvailable(name, null);
        BizHero hero = new BizHero();
        apply(hero, request.avatarUrl(), name, request.nickname(), request.role(), request.factionId(),
                request.gender(), request.introduction(), request.status());
        heroMapper.insert(hero);
        return getById(hero.getId());
    }

    @Override
    @Transactional
    public HeroVO update(Long id, UpdateHeroRequest request) {
        BizHero hero = requireHero(id);
        validateFaction(request.factionId());
        if (heroMapper.countLeaderReferences(id) > 0 && !sameFaction(hero.getFactionId(), request.factionId())) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "该英雄是阵营领袖，请先更换阵营领袖");
        }
        String name = request.name().trim();
        validateNameAvailable(name, id);
        apply(hero, request.avatarUrl(), name, request.nickname(), request.role(), request.factionId(),
                request.gender(), request.introduction(), request.status());
        if (heroMapper.update(hero) == 0) {
            throw notFound();
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireHero(id);
        ensureNotLeader(id);
        if (heroMapper.softDelete(id) == 0) {
            throw notFound();
        }
    }

    @Override
    @Transactional
    public HeroVO assign(Long id, AssignHeroRequest request) {
        BizHero hero = requireHero(id);
        validateFaction(request.factionId());
        if (heroMapper.countLeaderReferences(id) > 0 && !sameFaction(hero.getFactionId(), request.factionId())) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "该英雄是阵营领袖，请先更换阵营领袖");
        }
        hero.setFactionId(request.factionId());
        hero.setRole(normalize(request.role()));
        heroMapper.updateFaction(hero);
        return getById(id);
    }

    @Override
    @Transactional
    public HeroVO remove(Long id) {
        requireHero(id);
        ensureNotLeader(id);
        if (heroMapper.removeFaction(id) == 0) {
            throw notFound();
        }
        return getById(id);
    }

    @Override
    @Transactional
    public HeroVO updateStatus(Long id, HeroStatusRequest request) {
        requireHero(id);
        if (heroMapper.updateStatus(id, request.status()) == 0) {
            throw notFound();
        }
        return getById(id);
    }

    private void apply(BizHero hero, String avatarUrl, String name, String nickname, String role,
                       Long factionId, Integer gender, String introduction, Integer status) {
        hero.setAvatarUrl(normalize(avatarUrl));
        hero.setName(name);
        hero.setNickname(normalize(nickname));
        hero.setRole(normalize(role));
        hero.setFactionId(factionId);
        hero.setGender(gender == null ? 0 : gender);
        hero.setIntroduction(normalize(introduction));
        hero.setStatus(status == null ? 1 : status);
    }

    private HeroVO toVO(BizHero hero) {
        return new HeroVO(hero.getId(), hero.getRiotChampionId(), hero.getDataVersion(),
                hero.getAvatarUrl(), hero.getName(), hero.getNickname(),
                hero.getRole(), hero.getFactionId(), hero.getGender(), hero.getIntroduction(), hero.getStatus(),
                hero.getCreateTime(), hero.getUpdateTime());
    }

    private BizHero requireHero(Long id) {
        BizHero hero = heroMapper.findById(id);
        if (hero == null) {
            throw notFound();
        }
        return hero;
    }

    private void validateFaction(Long factionId) {
        if (factionId != null && factionMapper.findById(factionId) == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "阵营不存在");
        }
    }

    private void validateNameAvailable(String name, Long excludeId) {
        if (heroMapper.countByName(name, excludeId) > 0) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "英雄姓名已存在");
        }
    }

    private void ensureNotLeader(Long id) {
        if (heroMapper.countLeaderReferences(id) > 0) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "该英雄是阵营领袖，请先更换阵营领袖");
        }
    }

    private boolean sameFaction(Long left, Long right) {
        return left == null ? right == null : left.equals(right);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private BusinessException notFound() {
        return new BusinessException(404, HttpStatus.NOT_FOUND, "英雄档案不存在");
    }
}
