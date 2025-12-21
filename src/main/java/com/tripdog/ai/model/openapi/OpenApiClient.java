package com.tripdog.ai.model.openapi;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import com.tripdog.ai.CustomerChatMemoryProvider;
import com.tripdog.ai.tts.QwenRealtimeTtsService;
import com.tripdog.common.utils.FileUtil;
import com.tripdog.common.utils.MinioUtils;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.dto.OpenApiChatDTO;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.tripdog.common.Constants.USER_ID;

/**
 * OpenAPI
 * 大模型接口统一规范
 */
@Component
@RequiredArgsConstructor
public class OpenApiClient {
    public static final String QWEN = "qwen";

    private final QwenRealtimeTtsService qwenRealtimeTtsService;
    private final CustomerChatMemoryProvider chatMemoryProvider;
    private final MinioUtils minioUtils;
    private Map<String, OpenAIClient> clients;
    private Map<MultipartFile, String> urlMap;
    private Executor asyncExecutor;

    @PostConstruct
    public void init() {
        clients = new HashMap<>();
        urlMap = new HashMap<>();
        asyncExecutor = Executors.newFixedThreadPool(10);
        OpenAIClient qwen = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
        clients.put(QWEN, qwen);
    }

    public SseEmitter chat(OpenApiChatDTO dto) {
        SseEmitter emitter = dto.getEmitter();
        AtomicBoolean emitterClosed = dto.getEmitterClosed();
        AtomicReference<Long> uid = new AtomicReference<>((Long) ThreadLocalUtils.get(USER_ID));
        // 异步处理，不阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 多模态处理记忆维护
                ChatMemory chatMemory = chatMemoryProvider.get(dto.getConversationId());
                OpenAIClient aiClient = clients.getOrDefault(dto.getBrand(), clients.get(QWEN));
                StringBuilder sb = new StringBuilder();

                // 处理文件上传 todo 多文件支持
                String attachmentPath = null;
                String imageUrl = null;
                String attachmentName = dto.getFile().getOriginalFilename();
                if (dto.getFile() != null) {
                    attachmentPath = FileUtil.getAttachmentPathPrefix(dto.getConversationId(), uid.get()) + "/" + UUID.randomUUID() + FileUtil.getFileSuffix(attachmentName);
                    minioUtils.putObject(attachmentPath, dto.getFile());
                    imageUrl = minioUtils.getTemporaryUrlByPath(attachmentPath);
                }

                UserMessage userMessage = null;
                if (StringUtils.hasText(attachmentPath)) {
                    // 构建包含文本和图片的多部分用户消息
                    List<Content> contents = new ArrayList<>();
                    String content = StringUtils.hasText(dto.getContent()) ? dto.getContent() : "分析这种图片";
                    contents.add(TextContent.from(content));
                    Image image = Image.builder().url(attachmentPath).revisedPrompt(attachmentName).build();
                    contents.add(ImageContent.from(image));
                    userMessage = UserMessage.from(contents);
                } else {
                    // 仅文本消息
                    userMessage = UserMessage.from(dto.getContent());
                }
                chatMemory.add(userMessage);

                // 构建 OpenAI API 请求
                ChatCompletionUserMessageParam requestMessage = ChatCompletionUserMessageParam.builder()
                        .content(ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(buildOpenAIChatParts(dto, imageUrl)))
                        .build();
                ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                        .model("qwen-vl-max")
                        .messages(List.of(ChatCompletionMessageParam.ofUser(requestMessage)))
                        .build();

                // 流式处理响应
                boolean[] streamFailed = {false};
                try {
                    aiClient.chat().completions().createStreaming(params)
                            .stream()
                            .forEach(chunk -> {
                                if (streamFailed[0] || (emitterClosed != null && emitterClosed.get())) {
                                    return;
                                }
                                try {
                                    chunk.choices().stream()
                                            .findFirst()
                                            .flatMap(choice -> choice.delta().content())
                                            .ifPresent(content -> {
                                                try {
                                                    sb.append(content);
                                                    emitter.send(content);
                                                    // TTS处理必须在send之后，确保发送成功
                                                    if(dto.getTtsHolder() != null) {
                                                        dto.getTtsHolder().appendText(content);
                                                    }
                                                } catch (IOException e) {
                                                    streamFailed[0] = true;
                                                    if (emitterClosed != null) {
                                                        emitterClosed.set(true);
                                                    }
                                                    throw new RuntimeException("SSE发送失败", e);
                                                }
                                            });
                                } catch (Exception e) {
                                    streamFailed[0] = true;
                                    if (emitterClosed != null) {
                                        emitterClosed.set(true);
                                    }
                                }
                            });

                    // 如果流处理失败，直接返回
                    if (streamFailed[0]) {
                        cleanupTtsResources(dto);
                        return;
                    }
                } catch (Exception e) {
                    cleanupTtsResources(dto);
                    if (emitterClosed != null && emitterClosed.compareAndSet(false, true)) {
                        emitter.completeWithError(e);
                    }
                    return;
                } finally {
                    try {
                        // 确保完整响应被保存到TTS
                        if(dto.getTtsHolder() != null) {
                            dto.getTtsHolder().finish();
                        }
                    } catch (Exception e) {
                        // 日志记录但不中断流程
                    }
                }

                // 保存完整对话到记忆
                chatMemory.add(AiMessage.from(sb.toString()));
                if (emitterClosed == null || !emitterClosed.get()) {
                    emitter.complete();
                }

            } catch (Exception e) {
                if (emitterClosed != null && emitterClosed.compareAndSet(false, true)) {
                    emitter.completeWithError(e);
                }
            } finally {
                cleanupTtsResources(dto);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());

        return emitter;
    }

    private List<ChatCompletionContentPart> buildOpenAIChatParts(OpenApiChatDTO dto, String imageUrl) {
        List<ChatCompletionContentPart> parts = new ArrayList<>();
        String content = dto.getContent();
        String extractedImageUrl = extractImageUrl(content);

        ChatCompletionContentPart textPart = ChatCompletionContentPart.ofText(
                ChatCompletionContentPartText.builder()
                        .text(content)
                        .build()
        );
        parts.add(textPart);

        if (StringUtils.hasText(extractedImageUrl)) {
            ChatCompletionContentPart imagePart = ChatCompletionContentPart.ofImageUrl(
                    ChatCompletionContentPartImage.builder()
                            .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                    .url(extractedImageUrl)
                                    .build())
                            .build()
            );
            parts.add(imagePart);
        } else if (StringUtils.hasText(imageUrl)) {
            // 使用上传的图片 URL
            ChatCompletionContentPart imagePart = ChatCompletionContentPart.ofImageUrl(
                    ChatCompletionContentPartImage.builder()
                            .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                    .url(imageUrl)
                                    .build())
                            .build()
            );
            parts.add(imagePart);
        }

        return parts;
    }

    private String extractImageUrl(String str) {
        // todo 正则提取图片url
        return "";
    }

    /**
     * 清理 TTS 相关资源
     */
    private void cleanupTtsResources(OpenApiChatDTO dto) {
        if (dto.getTtsHolder() != null) {
            try {
                dto.getTtsHolder().finish();
                dto.getTtsHolder().awaitCompletion(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 日志记录但不中断流程
            } finally {
                try {
                    dto.getTtsHolder().close();
                } catch (Exception e) {
                    // 日志记录
                }
            }
        }
        if (dto.getTtsKey() != null) {
            try {
                qwenRealtimeTtsService.stopSession(dto.getTtsKey());
            } catch (Exception e) {
                // 日志记录
            }
        }
    }
}
