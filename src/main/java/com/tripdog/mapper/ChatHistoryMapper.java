package com.tripdog.mapper;

import com.tripdog.model.entity.ChatHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天历史记录表 Mapper 接口
 */
@Mapper
public interface ChatHistoryMapper {

    /**
     * 插入聊天记录
     */
    int insert(ChatHistoryDO chatHistory);

    /**
     * 根据ID删除聊天记录
     */
    int deleteById(String id);

    /**
     * 更新聊天记录
     */
    int updateById(ChatHistoryDO chatHistory);

    /**
     * 根据ID查询聊天记录
     */
    ChatHistoryDO selectById(String id);

    List<ChatHistoryDO> selectAllById(@Param("conversationId") String conversationId);

    List<ChatHistoryDO> selectLatestLimitById(@Param("conversationId") String conversationId, @Param("limit")  int limit);

    /**
     * 动态条件查询聊天记录
     */
    List<ChatHistoryDO> selectChatHistoryList(ChatHistoryDO chatHistory);

    /**
     * 获取会话最近N条消息作为AI上下文
     */
    List<ChatHistoryDO> selectRecentMessages(@Param("conversationId") String conversationId,
                                             @Param("limit") Integer limit);

    /**
     * 根据会话ID删除所有聊天记录
     */
    int deleteByConversationId(@Param("conversationId") String conversationId);
}
