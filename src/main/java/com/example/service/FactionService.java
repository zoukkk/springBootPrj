package com.example.service;

import com.example.dto.CreateFactionRequest;
import com.example.dto.FactionStatusRequest;
import com.example.dto.UpdateFactionRequest;
import com.example.vo.FactionVO;

import java.util.List;

public interface FactionService {
    List<FactionVO> getFactionTree(String keyword);

    FactionVO getById(Long id);

    FactionVO create(CreateFactionRequest request);

    FactionVO update(Long id, UpdateFactionRequest request);

    void delete(Long id);

    List<FactionVO> getDeletedFactions(String keyword);

    FactionVO restore(Long id);

    FactionVO updateStatus(Long id, FactionStatusRequest request);
}
