package com.tripdog.controller;

import com.tripdog.common.Result;
import com.tripdog.model.entity.SkillHistory;
import com.tripdog.common.utils.MinioUtils;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.SkillHistoryService;
import com.tripdog.service.direct.UserSessionService;
import com.tripdog.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillHistoryService skillHistoryService;
    private final UserSessionService userSessionService;
    private final MinioUtils minioUtils;

    @GetMapping("/history")
    public Result<List<SkillHistory>> listHistory(@RequestParam Long roleId) {
        UserInfoVO user = userSessionService.getCurrentUser();
        if (user == null) {
            return Result.error(ErrorCode.USER_NOT_LOGIN);
        }
        return Result.success(skillHistoryService.listByRole(roleId, user.getId()));
    }

    @GetMapping("/history/{id}/md")
    public ResponseEntity<byte[]> downloadMarkdown(@PathVariable Long id) {
        UserInfoVO user = userSessionService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        SkillHistory history = skillHistoryService.findById(id, user.getId());
        if (history == null || history.getMdPath() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = minioUtils.getObjectBytes(history.getMdPath());
            String filename = URLEncoder.encode((history.getDestination() == null ? "travel" : history.getDestination()) + ".md", StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/markdown; charset=utf-8"))
                .body(bytes);
        } catch (RuntimeException e) {
            // 如果对象路径取不到，回退重定向预签名 URL，避免返回空内容
            if (history.getMdUrl() != null) {
                return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, history.getMdUrl())
                    .build();
            }
            throw e;
        }
    }

    /**
     * 预览 Markdown：返回 302 到可访问的 md 链接，前端可直接 fetch/iframe 渲染。
     */
    @GetMapping("/history/{id}/md/preview")
    public ResponseEntity<Void> previewMarkdown(@PathVariable Long id) {
        UserInfoVO user = userSessionService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        SkillHistory history = skillHistoryService.findById(id, user.getId());
        if (history == null || history.getMdPath() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, history.getMdUrl() != null ? history.getMdUrl() : history.getMdPath())
                .build();
    }
}

