package com.tripdog.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 聊天历史记录表
 */
@Data
public class ChatHistoryDO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 消息角色：user/assistant/system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 检索增强后内容
     */
    private String enhancedContent;

    /**
     * 工具调用请求
     */
    private String toolCall;

    /**
     * 工具调用结果
     */
    private String toolExecResult;

    /**
     * 附件路径（文件URL或MinIO路径）
     */
    private String attachmentPath;

    /**
     * 附件名称
     */
    private String attachmentName;

    /**
     * 消息创建时间
     */
    private LocalDateTime createdAt;
}
