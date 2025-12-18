package com.tripdog.controller;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tripdog.ai.AssistantService;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.common.Result;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.config.ai.AiModelHolder;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.Duration;

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

    @PostMapping("/openapi")
    public SseEmitter openapi() {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            try {
                OpenAIClient aiClient = OpenAIOkHttpClient.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                        .build();

                List<ChatCompletionContentPart> parts = List.of(
                        ChatCompletionContentPart.ofText(
                                ChatCompletionContentPartText.builder()
                                        .text("分析一下这张图片")
                                        .build()
                        ),
                        ChatCompletionContentPart.ofImageUrl(
                                ChatCompletionContentPartImage.builder()
                                        .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                                .url("https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg")
                                                .build())
                                        .build()
                        )
                );

                ChatCompletionUserMessageParam userMessage = ChatCompletionUserMessageParam.builder()
                        .content(ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(parts))
                        .build();

                ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                        .model("qwen-vl-max")
                        .messages(List.of(ChatCompletionMessageParam.ofUser(userMessage)))
                        .build();

                aiClient.chat().completions().createStreaming(params)
                        .stream()
                        .forEach(chunk -> {
                            chunk.choices().stream()
                                    .findFirst()
                                    .flatMap(choice -> choice.delta().content())
                                    .ifPresent(content -> {
                                        try {
                                            System.out.print(content);
                                            emitter.send(content);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                        });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }
}
