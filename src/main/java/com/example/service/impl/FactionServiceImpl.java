package com.example.service.impl;

import com.example.dto.CreateFactionRequest;
import com.example.dto.FactionStatusRequest;
import com.example.dto.UpdateFactionRequest;
import com.example.exception.BusinessException;
import com.example.mapper.FactionMapper;
import com.example.mapper.HeroMapper;
import com.example.pojo.BizFaction;
import com.example.pojo.BizHero;
import com.example.service.FactionService;
import com.example.vo.FactionVO;
import com.example.vo.HeroVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FactionServiceImpl implements FactionService {
    private final FactionMapper factionMapper;
    private final HeroMapper heroMapper;

    public FactionServiceImpl(FactionMapper factionMapper, HeroMapper heroMapper) {
        this.factionMapper = factionMapper;
        this.heroMapper = heroMapper;
    }

    @Override
    public List<FactionVO> getFactionTree(String keyword) {
        List<BizFaction> factions = factionMapper.findAllActive();
        Map<Long, BizFaction> allById = new LinkedHashMap<>();
        factions.forEach(faction -> allById.put(faction.getId(), faction));

        Set<Long> retainedIds = retainedIds(factions, allById, keyword);
        Map<Long, MutableFaction> nodes = new LinkedHashMap<>();
        factions.stream()
                .filter(faction -> retainedIds.contains(faction.getId()))
                .forEach(faction -> nodes.put(faction.getId(), new MutableFaction(faction)));

        List<MutableFaction> roots = new ArrayList<>();
        for (MutableFaction node : nodes.values()) {
            MutableFaction parent = nodes.get(node.faction.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }
        return roots.stream().map(root -> root.toVO(0)).toList();
    }

    @Override
    public FactionVO getById(Long id) {
        return toVO(requireFaction(id));
    }

    @Override
    @Transactional
    public FactionVO create(CreateFactionRequest request) {
        Long parentId = defaultParentId(request.parentId());
        validateParent(parentId);
        String code = normalizeRequired(request.code());
        validateCodeAvailable(code, null);

        BizFaction faction = new BizFaction();
        apply(faction, parentId, request.name(), code, request.iconUrl(), request.themeColor(),
                request.leaderHeroId(), request.description(), request.sort(), request.status());
        factionMapper.insert(faction);
        validateAndAssignLeader(faction.getId(), request.leaderHeroId());
        return getById(faction.getId());
    }

    @Override
    @Transactional
    public FactionVO update(Long id, UpdateFactionRequest request) {
        BizFaction faction = requireFaction(id);
        Long parentId = defaultParentId(request.parentId());
        validateNewParent(id, parentId);
        String code = normalizeRequired(request.code());
        validateCodeAvailable(code, id);
        validateAndAssignLeader(id, request.leaderHeroId());

        apply(faction, parentId, request.name(), code, request.iconUrl(), request.themeColor(),
                request.leaderHeroId(), request.description(), request.sort(), request.status());
        if (factionMapper.update(faction) == 0) {
            throw notFound();
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireFaction(id);
        if (factionMapper.countChildren(id) > 0) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "存在下属阵营，不能删除");
        }
        if (factionMapper.countHeroes(id) > 0) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "存在挂载英雄，不能删除，请先迁移成员");
        }
        if (factionMapper.softDelete(id) == 0) {
            throw notFound();
        }
    }

    @Override
    public List<FactionVO> getDeletedFactions(String keyword) {
        return factionMapper.findDeleted(keyword).stream()
                .map(this::toArchivedVO)
                .toList();
    }

    @Override
    @Transactional
    public FactionVO restore(Long id) {
        BizFaction faction = factionMapper.findByIdIncludingDeleted(id);
        if (faction == null || faction.getDelFlag() == 0) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "待恢复阵营不存在");
        }
        validateParent(faction.getParentId());
        validateCodeAvailable(faction.getCode(), faction.getId());
        if (factionMapper.restore(id) == 0) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "待恢复阵营不存在");
        }
        return getById(id);
    }

    @Override
    @Transactional
    public FactionVO updateStatus(Long id, FactionStatusRequest request) {
        requireFaction(id);
        boolean cascade = Boolean.TRUE.equals(request.cascade());
        int changed = cascade
                ? factionMapper.updateStatusCascade(id, request.status())
                : factionMapper.updateStatus(id, request.status());
        if (changed == 0) {
            throw notFound();
        }
        if (cascade) {
            heroMapper.updateStatusByFactionTree(id, request.status());
        }
        return getById(id);
    }

    private Set<Long> retainedIds(List<BizFaction> factions, Map<Long, BizFaction> allById, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new HashSet<>(allById.keySet());
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        Set<Long> retained = new HashSet<>();
        for (BizFaction faction : factions) {
            if (containsIgnoreCase(faction.getName(), normalized)
                    || containsIgnoreCase(faction.getCode(), normalized)) {
                BizFaction current = faction;
                Set<Long> visited = new HashSet<>();
                while (current != null && visited.add(current.getId())) {
                    retained.add(current.getId());
                    current = allById.get(current.getParentId());
                }
            }
        }
        return retained;
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private void validateNewParent(Long id, Long parentId) {
        if (id.equals(parentId)) {
            throw new BusinessException("父级阵营不能指向自身");
        }
        validateParent(parentId);
        if (parentId != 0 && factionMapper.countDescendant(id, parentId) > 0) {
            throw new BusinessException("父级阵营不能指向当前阵营的子节点");
        }
    }

    private void validateParent(Long parentId) {
        if (parentId != 0 && factionMapper.findById(parentId) == null) {
            throw new BusinessException("父级阵营不存在");
        }
    }

    private void validateCodeAvailable(String code, Long excludeId) {
        if (factionMapper.countByCode(code, excludeId) > 0) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "阵营编码已存在");
        }
    }

    private void validateAndAssignLeader(Long factionId, Long leaderHeroId) {
        if (leaderHeroId == null) {
            return;
        }
        BizHero leader = heroMapper.findById(leaderHeroId);
        if (leader == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "领袖英雄不存在");
        }
        if (leader.getFactionId() != null && !factionId.equals(leader.getFactionId())) {
            throw new BusinessException(1, HttpStatus.CONFLICT, "领袖英雄已属于其他阵营");
        }
        if (leader.getFactionId() == null) {
            leader.setFactionId(factionId);
            if (!StringUtils.hasText(leader.getRole())) {
                leader.setRole("阵营领袖");
            }
            heroMapper.updateFaction(leader);
        }
    }

    private BizFaction requireFaction(Long id) {
        BizFaction faction = factionMapper.findById(id);
        if (faction == null) {
            throw notFound();
        }
        return faction;
    }

    private BusinessException notFound() {
        return new BusinessException(404, HttpStatus.NOT_FOUND, "阵营不存在");
    }

    private Long defaultParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private void apply(BizFaction faction, Long parentId, String name, String code,
                       String iconUrl, String themeColor, Long leaderHeroId, String description,
                       Integer sort, Integer status) {
        faction.setParentId(parentId);
        faction.setName(normalizeRequired(name));
        faction.setCode(code);
        faction.setIconUrl(iconUrl == null ? "" : iconUrl.trim());
        faction.setThemeColor(StringUtils.hasText(themeColor) ? themeColor.trim() : "#C89B3C");
        faction.setLeaderHeroId(leaderHeroId);
        faction.setDescription(description);
        faction.setSort(sort == null ? 0 : sort);
        faction.setStatus(status == null ? 1 : status);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private FactionVO toVO(BizFaction faction) {
        return new FactionVO(faction.getId(), faction.getParentId(), faction.getName(), faction.getCode(),
                faction.getIconUrl(), faction.getThemeColor(), faction.getLeaderHeroId(),
                faction.getDescription(), calculateLevel(faction), faction.getSort(), faction.getStatus(),
                faction.getCreateTime(), faction.getUpdateTime(), List.of(), members(faction.getId()));
    }

    private FactionVO toArchivedVO(BizFaction faction) {
        return new FactionVO(faction.getId(), faction.getParentId(), faction.getName(), faction.getCode(),
                faction.getIconUrl(), faction.getThemeColor(), faction.getLeaderHeroId(),
                faction.getDescription(), calculateLevel(faction), faction.getSort(), faction.getStatus(),
                faction.getCreateTime(), faction.getUpdateTime(), List.of(), List.of());
    }

    private List<HeroVO> members(Long factionId) {
        return heroMapper.findActiveByFactionId(factionId).stream()
                .map(hero -> new HeroVO(hero.getId(), hero.getRiotChampionId(), hero.getDataVersion(),
                        hero.getAvatarUrl(), hero.getName(), hero.getNickname(),
                        hero.getRole(), hero.getFactionId(), hero.getGender(), hero.getIntroduction(), hero.getStatus(),
                        hero.getCreateTime(), hero.getUpdateTime()))
                .toList();
    }

    private int calculateLevel(BizFaction faction) {
        int level = 0;
        Long parentId = faction.getParentId();
        Set<Long> visited = new HashSet<>();
        while (parentId != null && parentId != 0 && visited.add(parentId)) {
            BizFaction parent = factionMapper.findById(parentId);
            if (parent == null) {
                break;
            }
            level++;
            parentId = parent.getParentId();
        }
        return level;
    }

    private final class MutableFaction {
        private final BizFaction faction;
        private final List<MutableFaction> children = new ArrayList<>();

        private MutableFaction(BizFaction faction) {
            this.faction = faction;
        }

        private FactionVO toVO(int level) {
            return new FactionVO(faction.getId(), faction.getParentId(), faction.getName(), faction.getCode(),
                    faction.getIconUrl(), faction.getThemeColor(), faction.getLeaderHeroId(),
                    faction.getDescription(), level, faction.getSort(), faction.getStatus(),
                    faction.getCreateTime(), faction.getUpdateTime(),
                    children.stream().map(child -> child.toVO(level + 1)).toList(), members(faction.getId()));
        }
    }
}
