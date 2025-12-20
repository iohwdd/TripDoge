package com.tripdog.service.impl;

import com.tripdog.ai.CustomerChatMemoryProvider;
import com.tripdog.common.Constants;
import com.tripdog.common.middleware.RedisClient;
import com.tripdog.mapper.ConversationMapper;
import com.tripdog.mapper.ChatHistoryMapper;
import com.tripdog.mapper.RoleMapper;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.entity.ChatHistoryDO;
import com.tripdog.model.entity.RoleDO;
import com.tripdog.model.builder.ConversationBuilder;
import com.tripdog.service.ConversationService;
import com.tripdog.service.RoleService;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会话服务类
 * 实现一个用户对同一角色只有一个持久会话的逻辑
 */
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationMapper conversationMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final RoleMapper roleMapper;
    private final RoleService roleService;
    private final CustomerChatMemoryProvider chatMemoryProvider;
    private final RedisClient redisClient;

    /**
     * 获取或创建用户与角色的会话
     * 如果会话不存在，则自动创建
     */
    @Override
    @Transactional
    public ConversationDO getOrCreateConversation(Long userId, Long roleId) {
        // 先尝试查找已存在的会话
        ConversationDO conversation = findConversationByUserAndRole(userId, roleId);
        if (conversation != null) {
            return conversation;
        }


        // 会话不存在，创建新会话
        return createNewConversation(userId, roleId);
    }

    /**
     * 查找用户与角色的会话
     */
    @Override
    public ConversationDO findConversationByUserAndRole(Long userId, Long roleId) {
        ConversationDO queryParam = new ConversationDO();
        queryParam.setUserId(userId);
        queryParam.setRoleId(roleId);

        List<ConversationDO> conversations = conversationMapper.selectConversationList(queryParam);
        return conversations.isEmpty() ? null : conversations.get(0);
    }

    /**
     * 创建新会话
     */
    private ConversationDO createNewConversation(Long userId, Long roleId) {
        // 获取角色信息
        RoleDO role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在: " + roleId);
        }
        // 创建会话
        ConversationDO conversation = ConversationBuilder.buildNewConversation(userId, roleId, role.getName());
        conversationMapper.insert(conversation);
        chatMemoryProvider.createMemory(conversation.getConversationId());

        String counterKey = Constants.REDIS_SUMMARY + conversation.getConversationId();
        // 初始化滚动摘要触发计数器
        redisClient.set(counterKey, 0);
        return conversation;
    }

    /**
     * 重置会话上下文，开启新话题
     */
    @Override
    @Transactional
    public void resetConversationContext(String conversationId) {
        ChatMemory chatMemory = chatMemoryProvider.get(conversationId);
        ChatMessage systemMessage = chatMemory.messages().removeFirst();
        chatMemory.clear();
        chatMemory.add(systemMessage);

        // 更新会话信息 - 根据conversationId查找会话
        ConversationDO existingConversation = conversationMapper.selectByConversationId(conversationId);
        if (existingConversation != null) {
            existingConversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(existingConversation);
        }
    }

    /**
     * 获取会话的上下文消息（排除重置点之前的消息）
     */
    @Override
    public List<ChatHistoryDO> getContextMessages(String conversationId, Integer limit) {
        // 获取所有消息
        ChatHistoryDO queryParam = new ChatHistoryDO();
        queryParam.setConversationId(conversationId);
        List<ChatHistoryDO> allMessages = chatHistoryMapper.selectChatHistoryList(queryParam);

        // 找到最后一个重置点
        int lastResetIndex = -1;
        for (int i = allMessages.size() - 1; i >= 0; i--) {
            if ("[CONTEXT_RESET]".equals(allMessages.get(i).getContent())) {
                lastResetIndex = i;
                break;
            }
        }

        // 获取重置点之后的消息
        List<ChatHistoryDO> contextMessages;
        if (lastResetIndex >= 0) {
            contextMessages = allMessages.subList(lastResetIndex + 1, allMessages.size());
        } else {
            contextMessages = allMessages;
        }

        // 限制消息数量
        if (limit != null && contextMessages.size() > limit) {
            int start = contextMessages.size() - limit;
            contextMessages = contextMessages.subList(start, contextMessages.size());
        }

        return contextMessages;
    }

    /**
     * 更新会话统计信息
     */
    @Override
    @Transactional
    public void updateConversationStats(String conversationId, Integer inputTokens, Integer outputTokens) {
        ConversationDO conversation = conversationMapper.selectByConversationId(conversationId);
        if (conversation != null) {
            conversation.setMessageCount(conversation.getMessageCount() + 1);
            conversation.setTotalInputTokens(conversation.getTotalInputTokens() + (inputTokens != null ? inputTokens : 0));
            conversation.setTotalOutputTokens(conversation.getTotalOutputTokens() + (outputTokens != null ? outputTokens : 0));
            conversation.setLastMessageAt(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());

            conversationMapper.updateById(conversation);
        }
    }
}
