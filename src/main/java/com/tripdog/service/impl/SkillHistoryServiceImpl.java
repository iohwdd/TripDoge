package com.tripdog.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdog.common.utils.MinioUtils;
import com.tripdog.mapper.TravelPlanerHistoryMapper;
import com.tripdog.model.entity.SkillHistory;
import com.tripdog.model.entity.TravelPlanerHistory;
import com.tripdog.service.SkillHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillHistoryServiceImpl implements SkillHistoryService {

    private final TravelPlanerHistoryMapper travelHistoryMapper;
    private final MinioUtils minioUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void save(SkillHistory history) {
        travelHistoryMapper.insert(toTravelHistory(history));
    }

    @Override
    public List<SkillHistory> listByRole(Long roleId, Long userId) {
        List<TravelPlanerHistory> list = travelHistoryMapper.listByUserAndRole(userId, roleId);
        return list.stream().map(this::toSkillHistory).collect(Collectors.toList());
    }

    @Override
    public SkillHistory findById(Long id, Long userId) {
        TravelPlanerHistory h = travelHistoryMapper.selectByIdAndUser(id, userId);
        return h == null ? null : toSkillHistory(h);
    }

    private TravelPlanerHistory toTravelHistory(SkillHistory src) {
        TravelPlanerHistory t = new TravelPlanerHistory();
        t.setId(src.getId());
        t.setUserId(src.getUserId());
        t.setRoleId(src.getRoleId());
        t.setDestination(src.getDestination());
        t.setDays(src.getDays());
        t.setPeople(src.getPeople());
        t.setPreferences(writeJsonArray(src.getPreferences()));
        t.setMdPath(src.getMdPath());
        t.setMdUrl(src.getMdUrl());
        return t;
    }

    private SkillHistory toSkillHistory(TravelPlanerHistory t) {
        SkillHistory s = new SkillHistory();
        s.setId(t.getId());
        s.setUserId(t.getUserId());
        s.setRoleId(t.getRoleId());
        s.setDestination(t.getDestination());
        s.setDays(t.getDays());
        s.setPeople(t.getPeople());
        s.setPreferences(readJsonArray(t.getPreferences()));
        String url = null;
        // mdUrl 如果已有，直接用；否则基于 mdPath 生成预签名，供前端预览或作为下载回退
        if (t.getMdUrl() != null && !t.getMdUrl().isBlank()) {
            url = t.getMdUrl();
        } else if (t.getMdPath() != null) {
            url = minioUtils.getTemporaryUrlByPath(t.getMdPath());
        }
        s.setMdUrl(url);
        // 保持 mdPath 为对象 key，下载接口用 key 取对象
        s.setMdPath(t.getMdPath());
        s.setCreatedAt(t.getCreatedAt());
        return s;
    }

    private String writeJsonArray(List<String> prefs) {
        try {
            return objectMapper.writeValueAsString(prefs == null ? List.of() : prefs);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

