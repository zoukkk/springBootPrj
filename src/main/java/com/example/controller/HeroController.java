package com.example.controller;

import com.example.dto.AssignHeroRequest;
import com.example.dto.CreateHeroRequest;
import com.example.dto.HeroStatusRequest;
import com.example.dto.UpdateHeroRequest;
import com.example.pojo.Result;
import com.example.service.HeroService;
import com.example.service.RiotHeroSyncService;
import com.example.vo.HeroVO;
import com.example.vo.PageResult;
import com.example.vo.RiotSyncResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/heroes")
public class HeroController {
    private final HeroService heroService;
    private final RiotHeroSyncService riotHeroSyncService;

    public HeroController(HeroService heroService, RiotHeroSyncService riotHeroSyncService) {
        this.heroService = heroService;
        this.riotHeroSyncService = riotHeroSyncService;
    }

    @GetMapping("/list")
    public Result<PageResult<HeroVO>> list(
            @RequestParam(required = false) @Positive(message = "阵营ID必须大于0") Long factionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNum不能小于1") int pageNum,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "pageSize不能小于1")
            @Max(value = 100, message = "pageSize不能大于100") int pageSize) {
        return Result.success(heroService.list(factionId, keyword, pageNum, pageSize));
    }

    @GetMapping("/detail/{id}")
    public Result<HeroVO> detail(@PathVariable Long id) {
        return Result.success(heroService.getById(id));
    }

    @PostMapping("/add")
    public Result<HeroVO> add(@Valid @RequestBody CreateHeroRequest request) {
        return Result.success(heroService.create(request));
    }

    @PutMapping("/edit/{id}")
    public Result<HeroVO> edit(@PathVariable Long id,
                               @Valid @RequestBody UpdateHeroRequest request) {
        return Result.success(heroService.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        heroService.delete(id);
        return Result.success();
    }

    @PutMapping("/assign/{id}")
    public Result<HeroVO> assign(@PathVariable Long id,
                                 @Valid @RequestBody AssignHeroRequest request) {
        return Result.success(heroService.assign(id, request));
    }

    @PutMapping("/remove/{id}")
    public Result<HeroVO> remove(@PathVariable Long id) {
        return Result.success(heroService.remove(id));
    }

    @PutMapping("/status/{id}")
    public Result<HeroVO> updateStatus(@PathVariable Long id,
                                       @Valid @RequestBody HeroStatusRequest request) {
        return Result.success(heroService.updateStatus(id, request));
    }

    @PostMapping("/sync-riot")
    public Result<RiotSyncResult> syncRiotData(
            @RequestParam(defaultValue = "true") boolean overwriteFaction) {
        return Result.success(riotHeroSyncService.sync(overwriteFaction));
    }
}
