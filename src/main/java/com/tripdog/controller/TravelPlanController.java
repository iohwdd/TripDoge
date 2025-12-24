package com.tripdog.controller;

import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.dto.TravelPlanRequest;
import com.tripdog.model.dto.TravelPlanResponse;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.ConversationService;
import com.tripdog.service.TravelPlanService;
import com.tripdog.service.direct.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

import static com.tripdog.common.Constants.*;

@RestController
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelPlanController {
    private final ConversationService conversationService;
    private final TravelPlanService travelPlanService;
    private final UserSessionService userSessionService;

    /**
     * SSE 版本：前端可订阅进度与完成事件。
     */
    @PostMapping(value = "/plan", produces = "text/event-stream")
    public SseEmitter planStream(@RequestParam(required = false) Long roleId,
                                 @RequestBody TravelPlanRequest req) {
        UserInfoVO user = userSessionService.getCurrentUser();
        if (user == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data(ErrorCode.USER_NOT_LOGIN.getMessage()));
            } catch (Exception ignored) {
            }
            emitter.complete();
            return emitter;
        }

        if (req.getRawRequirement() == null) {
            req.setRawRequirement(buildRawRequirement(req));
        }

        SseEmitter emitter = new SseEmitter(0L); // 不超时，避免长耗时中断
        CompletableFuture.runAsync(() -> {
            try {
                // 子线程补写 ThreadLocal，避免异步丢失
                ThreadLocalUtils.set(USER_ID, user.getId());
                ThreadLocalUtils.set(ROLE_ID, roleId);
                ConversationDO conv = conversationService.findConversationByUserAndRole(user.getId(), roleId);
                ThreadLocalUtils.set(CONVERSATION_ID, conv.getConversationId());
                // 进度开始
                emitter.send(SseEmitter.event().name("progress").data(java.util.Map.of("status", "running")));

                TravelPlanResponse resp = travelPlanService.runTravelPlanStream(roleId, req, (node, state) -> {
                    try {
                        emitter.send(SseEmitter.event().name("workflow_update").data(java.util.Map.of(
                            "step", node,
                            "status", "finish",
                            "timestamp", System.currentTimeMillis()
                        )));
                    } catch (Exception ignored) {}
                });

                // 下发完成事件
                emitter.send(SseEmitter.event().name("done").data(resp));
                emitter.send(SseEmitter.event().name("message").data(resp.getMessage()));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("规划生成异常，请稍后重试。"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            } finally {
                ThreadLocalUtils.remove(USER_ID);
                ThreadLocalUtils.remove(ROLE_ID);
            }
        });
        return emitter;
    }

    @PostMapping(value = "/plan", consumes = "application/json", produces = "application/json")
    public Result<TravelPlanResponse> planJson(@RequestParam(required = false) Long roleId,
                                               @RequestBody TravelPlanRequest req) {
        UserInfoVO user = userSessionService.getCurrentUser();
        if (user == null) {
            return Result.error(ErrorCode.USER_NOT_LOGIN);
        }
        if (req.getRawRequirement() == null) {
            req.setRawRequirement(buildRawRequirement(req));
        }
        try {
            ThreadLocalUtils.set(USER_ID, user.getId());
            ThreadLocalUtils.set(ROLE_ID, roleId == null ? 0L : roleId);
            ConversationDO conv = conversationService.findConversationByUserAndRole(user.getId(), roleId);
            ThreadLocalUtils.set(CONVERSATION_ID, conv.getConversationId());
            TravelPlanResponse resp = travelPlanService.runTravelPlan(roleId, req);
            return Result.success(resp);
        } finally {
            ThreadLocalUtils.remove(USER_ID);
            ThreadLocalUtils.remove(ROLE_ID);
        }
    }

    private String buildRawRequirement(TravelPlanRequest req) {
        return "Destination: " + safe(req.getDestination()) +
            "; Days: " + (req.getDays() == null ? "3" : req.getDays()) +
            "; People: " + safe(req.getPeople()) +
            "; Budget: " + safe(req.getBudget()) +
            "; Preferences: " + (req.getPreferences() == null ? "" : String.join(",", req.getPreferences()));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

