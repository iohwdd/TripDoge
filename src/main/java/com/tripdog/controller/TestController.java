package com.tripdog.controller;

import com.tripdog.ai.AssistantService;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.common.Result;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.config.ai.AiModelHolder;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

import static com.tripdog.common.Constants.ROLE_ID;
import static com.tripdog.common.Constants.USER_ID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    final AiModelHolder aiModelHolder;
    final AssistantService assistantService;

    @PostMapping("/chat")
    public Result<String> chat() {
        var model = aiModelHolder.getChatModel(AiModelHolder.QwenChat);
        return Result.success(model.chat("你是什么模型"));
    }

    @PostMapping("/stream")
    public Result<String> stream() {
        ThreadLocalUtils.set(USER_ID, 1L);
        ThreadLocalUtils.set(ROLE_ID, 1L);

        ChatAssistant assistant = assistantService.getAssistant("");
        TokenStream stream = assistant.chat("1", "你是什么模型");

        stream.onPartialResponse((partialResponse) -> {
            System.out.println(partialResponse);
        });
        return Result.success("");
    }
}
