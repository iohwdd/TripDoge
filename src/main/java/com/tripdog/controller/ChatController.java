package com.tripdog.controller;

import java.util.List;

import com.tripdog.common.Constants;
import com.tripdog.common.PageVO;
import com.tripdog.common.middleware.RedisClient;
import com.tripdog.exception.LimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import com.tripdog.model.dto.ChatReqDTO;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.entity.ChatHistoryDO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.ChatService;
import com.tripdog.service.impl.ConversationServiceImpl;
import com.tripdog.service.direct.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import static com.tripdog.common.Constants.REDIS_CHAT_LIMIT_RPM;
import static com.tripdog.common.Constants.REDIS_CHAT_LIMIT_RULE;

/**
 * 聊天控制器
 * 实现一个用户对同一角色只有一个持久会话的逻辑
 */
@Slf4j
@Tag(name = "智能对话", description = "与AI角色进行对话的相关接口，支持SSE流式响应")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final ConversationServiceImpl conversationServiceImpl;
    private final UserSessionService userSessionService;
    private final RedisClient redisClient;

    /**
     * 与指定角色聊天
     * @param roleId 角色ID
     */
    @Operation(summary = "与AI角色对话", description = "与指定的AI角色进行实时对话，返回SSE流式响应")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功建立SSE连接，返回流式对话内容"),
            @ApiResponse(responseCode = "10200", description = "角色不存在"),
            @ApiResponse(responseCode = "10105", description = "用户未登录")
    })
    @PostMapping(value = "/{roleId}", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chat(@Parameter(description = "角色ID", required = true) @PathVariable Long roleId,
                          ChatReqDTO req) {
        // 从用户会话服务获取当前登录用户信息
        UserInfoVO userInfoVO = userSessionService.getCurrentUser();
        if (userInfoVO == null) {
            throw new RuntimeException(ErrorCode.USER_NOT_LOGIN.getMessage());
        }
        Long userId = userInfoVO.getId();

        // 限流检查
        String limitRpmKey = REDIS_CHAT_LIMIT_RPM + userId;
        try {
            int rmpLimit = (Integer) redisClient.hget(REDIS_CHAT_LIMIT_RULE, Constants.SYS_RPM);
            long requestWindow = redisClient.zcount(limitRpmKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            if(requestWindow >= rmpLimit) {
                throw new LimitException(ErrorCode.LIMIT_BY_RPM.getMessage());
            }
            return chatService.chat(roleId, userId, req);
        }catch (LimitException e){
            log.warn("uid:{}, request chat failed, err:{}", userId, e.getMessage());
            throw new RuntimeException(ErrorCode.LIMIT_BY_RPM.getMessage());
        } finally {
            redisClient.zremrangebyscore(limitRpmKey, Double.NEGATIVE_INFINITY, getCurrentMinuteTimeStamp());
        }
    }

    /**
     *  当前11.42:30，则返回 11.42:00 的时间戳
     * @return 当前分钟的整数时间戳
     */
    private int getCurrentMinuteTimeStamp() {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long secondsInMinute = currentTimeSeconds % 60;
        return (int) (currentTimeSeconds - secondsInMinute);
    }

    /**
     * 重置会话上下文
     * @param roleId 角色ID
     */
    @Operation(summary = "重置会话上下文", description = "清空与指定角色的对话历史，重新开始对话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "重置成功"),
            @ApiResponse(responseCode = "10300", description = "对话不存在"),
            @ApiResponse(responseCode = "10105", description = "用户未登录")
    })
    @PostMapping("/{roleId}/reset")
    public Result<Void> resetContext(@Parameter(description = "角色ID", required = true) @PathVariable Long roleId) {
        // 从用户会话服务获取当前登录用户信息
        UserInfoVO userInfoVO = userSessionService.getCurrentUser();
        if (userInfoVO == null) {
            throw new RuntimeException(ErrorCode.USER_NOT_LOGIN.getMessage());
        }

        Long userId = userInfoVO.getId();
        ConversationDO conversation = conversationServiceImpl.findConversationByUserAndRole(userId, roleId);
        if (conversation == null) {
            return Result.error(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        conversationServiceImpl.resetConversationContext(conversation.getConversationId());

        return Result.success();
    }

    /**
     * 获取会话历史
     * @param roleId 角色ID
     */
    @Operation(summary = "获取对话历史", description = "获取与指定角色的聊天历史记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取历史记录"),
            @ApiResponse(responseCode = "10105", description = "用户未登录")
    })
    @PostMapping("/{roleId}/history")
    public Result<PageVO<ChatHistoryDO>> getHistory(@Parameter(description = "角色ID", required = true) @PathVariable Long roleId,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        // 从用户会话服务获取当前登录用户信息
        UserInfoVO userInfo = userSessionService.getCurrentUser();
        if(userInfo == null) {
            return Result.error(ErrorCode.USER_NOT_LOGIN);
        }
        ConversationDO conversation = conversationServiceImpl.findConversationByUserAndRole(userInfo.getId(), roleId);
        if (conversation == null) {
            return Result.success(null);
        }

        List<ChatHistoryDO> history = conversationServiceImpl.getContextMessages(conversation.getConversationId(), null);
        return Result.success(PageVO.of(history, page, pageSize));
    }

}
