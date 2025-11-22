package com.tripdog.service;

import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.entity.ChatHistoryDO;

import java.util.List;

/**
 * 会话服务接口
 */
public interface ConversationService {

    /**
     * 获取或创建用户与角色的会话
     * 如果会话不存在，则自动创建
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 会话信息
     */
    ConversationDO getOrCreateConversation(Long userId, Long roleId);

    /**
     * 查找用户与角色的会话
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 会话信息，如果不存在返回null
     */
    ConversationDO findConversationByUserAndRole(Long userId, Long roleId);

    /**
     * 重置会话上下文
     * 不删除历史记录，而是插入重置标记
     *
     * @param conversationId 会话ID
     */
    boolean resetConversationContext(String conversationId, Long userId);

    /**
     * 获取会话的上下文消息（排除重置点之前的消息）
     *
     * @param conversationId 会话ID
     * @param limit 限制消息数量
     * @return 上下文消息列表
     */
    List<ChatHistoryDO> getContextMessages(String conversationId, Integer limit);

    /**
     * 更新会话统计信息
     *
     * @param conversationId 会话ID
     * @param inputTokens 输入token数
     * @param outputTokens 输出token数
     */
    void updateConversationStats(String conversationId, Integer inputTokens, Integer outputTokens);
}
