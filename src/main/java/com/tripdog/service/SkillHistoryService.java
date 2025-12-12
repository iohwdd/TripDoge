package com.tripdog.service;

import com.tripdog.model.entity.SkillHistory;

import java.util.List;

public interface SkillHistoryService {
    void save(SkillHistory history);
    List<SkillHistory> listByRole(Long roleId, Long userId);
    SkillHistory findById(Long id, Long userId);
}

