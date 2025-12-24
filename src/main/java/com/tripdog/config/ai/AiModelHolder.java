package com.tripdog.config.ai;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author: iohw
 * @date: 2025/9/22 22:58
 * @description: AI相关配置类
 */
@Configuration
public class AiModelHolder {
    public static final String QwenChat = "QwenChatModel";
    public static final String QwenStreamingChat = "QwenStreamingChatModel";
    public static final String ZhipuAiChat = "ZhipuAiChatModel";
    public static final String ZhipuAiStreamingChat = "ZhipuAiStreamingChatModel";

    @Value("${DASHSCOPE_API_KEY}")
    private String dashscopeApiKey;
    @Value("${ZHIPU_API_KEY}")
    private String zhipuApiKey;

    private Map<String, ChatModel> chatModels;
    private Map<String, StreamingChatModel> streamingChatModels;

    @PostConstruct
    public void init() {
        chatModels = new HashMap<>();
        streamingChatModels = new HashMap<>();
        QwenStreamingChatModel qwenStreamingChatModel = QwenStreamingChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName("qwen3-max")
                .build();
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName("qwen3-max")
                .build();
        ZhipuAiStreamingChatModel zhipuAiStreamingChatModel = ZhipuAiStreamingChatModel.builder()
                .model("glm-4.7")
                .apiKey(zhipuApiKey)
                .build();
        ZhipuAiChatModel zhipuAiChatModel = ZhipuAiChatModel.builder()
                .model("glm-4.7")
                .apiKey(zhipuApiKey)
                .build();
        chatModels.put(QwenChat, qwenChatModel);
        chatModels.put(ZhipuAiChat, zhipuAiChatModel);
        streamingChatModels.put(QwenStreamingChat, qwenStreamingChatModel);
        streamingChatModels.put(ZhipuAiStreamingChat, zhipuAiStreamingChatModel);
    }

    public ChatModel getChatModel(String key) {
        return chatModels.get(key);
    }

    public StreamingChatModel getStreamingChatModel(String key) {
        return streamingChatModels.get(key);
    }

    public ChatModel getDefaultChat() {
        return chatModels.get(QwenChat);
    }

    public StreamingChatModel getDefaultStreaming() {
        return streamingChatModels.get(QwenStreamingChat);
    }
}
