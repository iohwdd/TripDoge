package com.tripdog.service.impl;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tripdog.common.utils.MinioUtils;
import com.tripdog.model.entity.SkillHistory;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.SkillHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.util.StringUtils;

import com.tripdog.ai.AssistantService;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.common.utils.FileUploadUtils;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.dto.FileUploadDTO;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.entity.RoleDO;
import com.tripdog.model.dto.ChatReqDTO;
import com.tripdog.service.ChatService;
import com.tripdog.mapper.ChatHistoryMapper;
import com.tripdog.mapper.RoleMapper;
import com.tripdog.common.utils.RoleConfigParser;
import com.tripdog.ai.tts.QwenRealtimeTtsService;

import com.tripdog.ai.langgraph.travel.TravelState;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.tripdog.common.Constants.ROLE_ID;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 聊天服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final StreamingChatModel chatLanguageModel;
    private final ConversationServiceImpl conversationServiceImpl;
    private final ChatHistoryMapper chatHistoryMapper;
    private final RoleMapper roleMapper;
    private final AssistantService assistantService;
    private final FileUploadUtils fileUploadUtils;
    private final QwenRealtimeTtsService qwenRealtimeTtsService;
    private final SkillHistoryService skillHistoryService;
    private final MinioUtils minioUtils;
    private final UserSessionService userSessionService;
    private final com.tripdog.service.TravelPlanService travelPlanService;

    @Override
    public SseEmitter chat(Long roleId, Long userId, ChatReqDTO chatReqDTO) {
        ThreadLocalUtils.set(ROLE_ID, roleId);
        SseEmitter emitter = new SseEmitter(-1L);
        final QwenRealtimeTtsService.RealtimeTtsSession[] ttsHolder = new QwenRealtimeTtsService.RealtimeTtsSession[1];
        AtomicBoolean emitterClosed = new AtomicBoolean(false);

        try {
            // 0. 检查是否为技能触发
            String messageContent = chatReqDTO.getMessage();
            if (messageContent != null && messageContent.startsWith("[SKILL_TRIGGER:TRAVEL]")) {
                handleTravelSkill(roleId, messageContent, emitter, emitterClosed);
                return emitter;
            }

            // 1. 获取或创建会话
            ConversationDO conversation = conversationServiceImpl.getOrCreateConversation(userId, roleId);

            // 2. 获取角色信息
            RoleDO role = roleMapper.selectById(roleId);
            if (role == null) {
                emitter.completeWithError(new RuntimeException("角色不存在"));
                return emitter;
            }

            // 3. 从角色配置中提取系统提示词
            String systemPrompt = RoleConfigParser.extractSystemPrompt(role.getAiSetting());
            log.info("角色[{}]使用系统提示词: {}", role.getName(), systemPrompt);

            StringBuilder responseBuilder = new StringBuilder();
            // 使用角色专用的聊天助手，传入角色的系统提示词
            ChatAssistant assistant = assistantService.getAssistant(systemPrompt);

            ttsHolder[0] = Boolean.TRUE.equals(chatReqDTO.getStreamAudio())
                ? qwenRealtimeTtsService.startSession(delta -> sendAudioDelta(emitter, emitterClosed, delta), chatReqDTO.getVoice()).orElse(null)
                : null;

            MultipartFile file = chatReqDTO.getFile();
            TokenStream stream;
            if(file != null) {
                // todo 多模态支持
                FileUploadDTO fileUploadDTO = fileUploadUtils.upload2Minio(chatReqDTO.getFile(), userId, "/tmp");
                String imageUrl = fileUploadUtils.getUrlFromMinio(fileUploadDTO.getFileUrl());
                UserMessage message = UserMessage.from(TextContent.from(chatReqDTO.getMessage()), ImageContent.from(URI.create(imageUrl)));
                stream = assistant.chat(conversation.getConversationId(), message);
            }else {
                stream = assistant.chat(conversation.getConversationId(), chatReqDTO.getMessage());
            }

            stream.onPartialResponse((data) -> {
                if (emitterClosed.get()) {
                    return;
                }
                try {
                    responseBuilder.append(data);
                    if (ttsHolder[0] != null) {
                        ttsHolder[0].appendText(data);
                    }
                    emitter.send(SseEmitter.event()
                        .data(data)
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("message")
                    );
                } catch (Exception e) {
                    handleEmitterException(emitter, emitterClosed, e);
                }
            }).onCompleteResponse((data) -> {
                try {

                    // 8. 更新会话统计
                    conversationServiceImpl.updateConversationStats(conversation.getConversationId(), null, null);
                    if (ttsHolder[0] != null) {
                        ttsHolder[0].finish();
                        ttsHolder[0].awaitCompletion(10, TimeUnit.SECONDS);
                    }

                    if (!emitterClosed.get()) {
                    emitter.send(SseEmitter.event()
                        .data("[DONE]")
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("done"));
                    emitter.complete();
                        emitterClosed.set(true);
                    }
                } catch (Exception e) {
                    handleEmitterException(emitter, emitterClosed, e);
                } finally {
                    if (ttsHolder[0] != null) {
                        ttsHolder[0].close();
                    }
                }
            }).onError((ex) -> {
                log.error("AI聊天流处理异常", ex);
                if (ttsHolder[0] != null) {
                    ttsHolder[0].finish();
                    ttsHolder[0].close();
                }
                if (emitterClosed.compareAndSet(false, true)) {
                emitter.completeWithError(ex);
                }
            }).start();

        } catch (Exception e) {
            log.error("聊天服务处理异常", e);
            if (ttsHolder[0] != null) {
                ttsHolder[0].close();
            }
            if (emitterClosed.compareAndSet(false, true)) {
            emitter.completeWithError(e);
            }
        } finally {
            ThreadLocalUtils.remove(ROLE_ID);
        }

        return emitter;
    }

    private void handleTravelSkill(Long roleId, String prompt, SseEmitter emitter, AtomicBoolean emitterClosed) {
        try {
            // 1. 解析参数
            Map<String, Object> inputs = parseTravelPrompt(prompt);
            inputs.put("roleId", roleId);

            // 转为结构化请求，调用独立的旅行规划服务
            com.tripdog.model.dto.TravelPlanRequest req = new com.tripdog.model.dto.TravelPlanRequest();
            req.setDestination((String) inputs.get(TravelState.DESTINATION));
            req.setDays((Integer) inputs.get(TravelState.DAYS));
            req.setPeople((String) inputs.get(TravelState.PEOPLE));
            req.setPreferences((java.util.List<String>) inputs.get(TravelState.TAGS));
            req.setBudget((String) inputs.getOrDefault("budget", ""));
            req.setRawRequirement(prompt);

            var resp = travelPlanService.runTravelPlan(roleId, req);

            // 已生成 Markdown，提示用户前往历史记录下载，不输出正文
            if (resp != null && StringUtils.hasText(resp.getMdPath())) {
                sendWorkflowUpdate(emitter, emitterClosed, "generate", "finish");
                sendTextDelta(emitter, emitterClosed, "行程规划已生成，请到“历史记录”下载 Markdown 路书。");
            } else {
                sendTextDelta(emitter, emitterClosed, resp == null ? "规划生成异常，请稍后重试。" : resp.getMessage());
            }

            // 4. 结束
            if (!emitterClosed.get()) {
                emitter.send(SseEmitter.event()
                    .data("[DONE]")
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("done"));
                emitter.complete();
                emitterClosed.set(true);
            }

        } catch (Exception e) {
            log.error("Travel skill execution failed", e);
            handleEmitterException(emitter, emitterClosed, e);
        }
    }

    private void sendWorkflowUpdate(SseEmitter emitter, AtomicBoolean emitterClosed, String step, String status) {
        if (emitterClosed.get()) return;
        try {
            Map<String, String> data = Map.of(
                "step", step,
                "status", status
            );
            emitter.send(SseEmitter.event()
                .name("workflow_update") // 前端需要监听此事件
                .data(data));
        } catch (Exception e) {
            log.error("Failed to send workflow update", e);
        }
    }

    private void sendTextDelta(SseEmitter emitter, AtomicBoolean emitterClosed, String text) {
        if (emitterClosed.get()) return;
        try {
            // 模拟流式打字机效果（LangGraph目前是一次性返回结果，这里简单拆分一下或直接整段发）
            // 为了体验更好，直接整段发也可以，前端 Markdown 渲染能处理
            emitter.send(SseEmitter.event()
                .data(text)
                .name("message"));
        } catch (Exception e) {
            handleEmitterException(emitter, emitterClosed, e);
        }
    }

    private Map<String, Object> parseTravelPrompt(String prompt) {
        Map<String, Object> map = new HashMap<>();
        map.put(TravelState.RAW_REQUIREMENT, prompt);
        map.put(TravelState.DESTINATION, extract(prompt, "Dest: (.*)"));
        try {
            String daysStr = extract(prompt, "Days: (\\d+)");
            map.put(TravelState.DAYS, daysStr.isEmpty() ? 3 : Integer.parseInt(daysStr));
        } catch (Exception e) {
            map.put(TravelState.DAYS, 3);
        }
        map.put(TravelState.PEOPLE, extract(prompt, "Who: (.*)"));
        // Tags handle
        String tagsStr = extract(prompt, "Tags: (.*)");
        if (!tagsStr.isEmpty()) {
            map.put(TravelState.TAGS, java.util.Arrays.asList(tagsStr.split(",")));
        }
        return map;
    }

    private SkillHistory buildSkillHistory(Map<String, Object> inputs, TravelState state) {
        SkillHistory h = new SkillHistory();
        // 当前用户信息
        UserInfoVO user = userSessionService.getCurrentUser();
        if (user != null) {
            h.setUserId(user.getId());
        }
        // 角色信息（如需存储）
        Object roleIdObj = inputs.get("roleId");
        if (roleIdObj instanceof Long) {
            h.setRoleId((Long) roleIdObj);
        }
        h.setSkill("travel");
        h.setDestination(state.destination());
        h.setDays(state.days());
        h.setPeople(state.people());
        h.setBudget((String) inputs.getOrDefault("budget", ""));
        Object prefObj = inputs.get(TravelState.TAGS);
        if (prefObj instanceof Iterable) {
            List<String> prefs = new ArrayList<>();
            for (Object o : (Iterable<?>) prefObj) {
                if (o != null) prefs.add(o.toString());
            }
            h.setPreferences(prefs);
        }
        return h;
    }

    private MdFile generateAndUploadMarkdown(Long userId, String markdown) throws Exception {
        byte[] bytes = markdown == null ? new byte[0] : markdown.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String objectKey = "workflow/traval/" + (userId == null ? "anonymous" : userId) + "/" + java.util.UUID.randomUUID() + ".md";
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes)) {
            // 显式声明 charset，避免浏览器默认 ISO-8859-1 导致中文乱码
            minioUtils.putObject(objectKey, in, bytes.length, "text/markdown; charset=utf-8");
        }
        // 生成预签名 URL
        String url = minioUtils.getTemporaryUrlByPath(objectKey);
        return new MdFile(objectKey, url);
    }

    private record MdFile(String path, String url) {}

    private String extract(String source, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private void sendAudioDelta(SseEmitter emitter, AtomicBoolean emitterClosed, String base64Pcm) {
        if (!StringUtils.hasText(base64Pcm)) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .data(Map.of("audio_delta", base64Pcm))
                .id(String.valueOf(System.currentTimeMillis()))
                .name("audio"));
        } catch (Exception e) {
            handleEmitterException(emitter, emitterClosed, e);
        }
    }

    private void handleEmitterException(SseEmitter emitter, AtomicBoolean emitterClosed, Exception e) {
        if (e instanceof IllegalStateException || e.getCause() instanceof IOException) {
            if (emitterClosed.compareAndSet(false, true)) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
            log.warn("SSE连接已关闭，后续数据丢弃: {}", e.getMessage());
            return;
        }
        log.error("SSE发送失败", e);
        if (emitterClosed.compareAndSet(false, true)) {
            emitter.completeWithError(e);
        }
    }
}
