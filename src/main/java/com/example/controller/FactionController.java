package com.example.controller;

import com.example.dto.CreateFactionRequest;
import com.example.dto.FactionStatusRequest;
import com.example.dto.UpdateFactionRequest;
import com.example.pojo.Result;
import com.example.service.FactionService;
import com.example.vo.FactionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/factions")
public class FactionController {
    private final FactionService factionService;

    public FactionController(FactionService factionService) {
        this.factionService = factionService;
    }

    @GetMapping("/list")
    public Result<List<FactionVO>> list(
            @RequestParam(required = false) String keyword) {
        return Result.success(factionService.getFactionTree(keyword));
    }

    @GetMapping("/detail/{id}")
    public Result<FactionVO> detail(@PathVariable Long id) {
        return Result.success(factionService.getById(id));
    }

    @PostMapping("/add")
    public Result<FactionVO> add(@Valid @RequestBody CreateFactionRequest request) {
        return Result.success(factionService.create(request));
    }

    @PutMapping("/edit/{id}")
    public Result<FactionVO> edit(@PathVariable Long id,
                                  @Valid @RequestBody UpdateFactionRequest request) {
        return Result.success(factionService.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        factionService.delete(id);
        return Result.success();
    }

    @GetMapping("/deleted-list")
    public Result<List<FactionVO>> deletedList(
            @RequestParam(required = false) String keyword) {
        return Result.success(factionService.getDeletedFactions(keyword));
    }

    @PutMapping("/restore/{id}")
    public Result<FactionVO> restore(@PathVariable Long id) {
        return Result.success(factionService.restore(id));
    }

    @PutMapping("/status/{id}")
    public Result<FactionVO> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody FactionStatusRequest request) {
        return Result.success(factionService.updateStatus(id, request));
    }
}
