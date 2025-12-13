package com.tripdog.ai.tts;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeAudioFormat;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeCallback;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeConfig;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

/**
 * 提供对阿里云 DashScope Qwen 实时 TTS 的简易封装。
 */
@Service
@Slf4j
public class QwenRealtimeTtsService {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
    private final Map<String, RealtimeTtsSession> sessionHolder = new ConcurrentHashMap<>();

    @Value("${tts.qwen.enabled:true}")
    private boolean enabled;

    @Value("${tts.qwen.url:wss://dashscope.aliyuncs.com/api-ws/v1/realtime}")
    private String endpoint;

    @Value("${tts.qwen.model:qwen3-tts-flash-realtime}")
    private String model;

    @Value("${tts.qwen.voice:Cherry}")
    private String defaultVoice;

    @Value("${tts.qwen.mode:server_commit}")
    private String defaultMode;

    @Value("${tts.qwen.format:PCM_24000HZ_MONO_16BIT}")
    private String responseFormat;

    @Value("${DASHSCOPE_API_KEY:}")
    private String dashscopeApiKey;

    /**
     * 尝试开启一个新的实时 TTS 会话。
     *
     * @param audioConsumer 音频数据回调（Base64 PCM）
     * @param voice         指定音色（为空则使用默认）
     * @return 会话包装，若配置缺失或创建失败则返回 empty
     */
    public Optional<RealtimeTtsSession> startSession(Consumer<String> audioConsumer, String voice) {
        if (!enabled) {
            log.debug("Qwen realtime TTS is disabled, skip session creation.");
            return Optional.empty();
        }
        if (!StringUtils.hasText(dashscopeApiKey)) {
            log.warn("DashScope API Key is empty, unable to enable realtime TTS.");
            return Optional.empty();
        }
        if (audioConsumer == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new RealtimeTtsSession(audioConsumer, voice));
        } catch (Exception e) {
            log.error("Failed to start Qwen realtime TTS session.", e);
            return Optional.empty();
        }
    }

    /**
     * 尝试开启一个新的实时 TTS 会话（使用默认音色）。
     */
    public Optional<RealtimeTtsSession> startSession(Consumer<String> audioConsumer) {
        return startSession(audioConsumer, null);
    }

    /**
     * 以 key 为标识启动新会话，若已有同 key 会话则先终止。
     */
    public Optional<RealtimeTtsSession> startOrReplaceSession(String key, Consumer<String> audioConsumer, String voice) {
        stopSession(key);
        Optional<RealtimeTtsSession> session = startSession(audioConsumer, voice);
        session.ifPresent(s -> sessionHolder.put(key, s));
        return session;
    }

    public void stopSession(String key) {
        if (key == null) return;
        RealtimeTtsSession session = sessionHolder.remove(key);
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.warn("Failed to close existing TTS session for key {}", key, e);
            }
        }
    }

    public class RealtimeTtsSession implements AutoCloseable {

        private final QwenTtsRealtime client;
        private final Consumer<String> audioConsumer;
        private final ExecutorService executor;
        private final AtomicBoolean finishRequested = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final CountDownLatch finishedLatch = new CountDownLatch(1);

        private RealtimeTtsSession(Consumer<String> audioConsumer, String voice) throws NoApiKeyException, InterruptedException {
            this.audioConsumer = audioConsumer;
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "qwen-tts-feed-" + THREAD_COUNTER.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });

            QwenTtsRealtimeParam param = QwenTtsRealtimeParam.builder()
                .model(model)
                .url(endpoint)
                .apikey(dashscopeApiKey)
                .build();

            this.client = new QwenTtsRealtime(param, new InternalCallback());
            this.client.connect();

            QwenTtsRealtimeAudioFormat audioFormat = resolveFormat();
            QwenTtsRealtimeConfig config = QwenTtsRealtimeConfig.builder()
                .voice(StringUtils.hasText(voice) ? voice : defaultVoice)
                .responseFormat(audioFormat)
                .mode(defaultMode)
                .build();
            this.client.updateSession(config);
        }

        /**
         * 追加一段文本进入 TTS 队列。
         */
        public void appendText(String text) {
            if (!StringUtils.hasText(text) || closed.get()) {
                return;
            }
            executor.submit(() -> {
                try {
                    client.appendText(text);
                } catch (Exception ex) {
                    log.error("Failed to append text to Qwen TTS session.", ex);
                }
            });
        }

        /**
         * 请求结束本次 TTS 对话。
         */
        public void finish() {
            if (finishRequested.compareAndSet(false, true)) {
                executor.submit(() -> {
                    try {
                        client.finish();
                    } catch (Exception ex) {
                        log.warn("Failed to finish Qwen TTS session gracefully.", ex);
                        finishedLatch.countDown();
                    }
                });
            }
        }

        /**
         * 等待服务端宣告会话结束，避免文本结束但音频还未推送完。
         */
        public void awaitCompletion(long timeout, TimeUnit unit) {
            try {
                if (!finishedLatch.await(timeout, unit)) {
                    log.warn("Timeout waiting for Qwen TTS session completion.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void close() {
            finish();
            if (closed.compareAndSet(false, true)) {
                finishedLatch.countDown();
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    executor.shutdownNow();
                }

                try {
                    client.close();
                } catch (Exception ex) {
                    log.warn("Failed to close Qwen TTS client.", ex);
                }
            }
        }

        private QwenTtsRealtimeAudioFormat resolveFormat() {
            if (!StringUtils.hasText(responseFormat)) {
                return QwenTtsRealtimeAudioFormat.PCM_24000HZ_MONO_16BIT;
            }
            try {
                return QwenTtsRealtimeAudioFormat.valueOf(responseFormat);
            } catch (IllegalArgumentException ex) {
                log.warn("Unsupported TTS audio format: {}, fallback to PCM_24000HZ_MONO_16BIT", responseFormat);
                return QwenTtsRealtimeAudioFormat.PCM_24000HZ_MONO_16BIT;
            }
        }

        private class InternalCallback extends QwenTtsRealtimeCallback {
            @Override
            public void onOpen() {
                log.debug("Qwen realtime TTS session opened.");
            }

            @Override
            public void onEvent(JsonObject message) {
                if (message == null || !message.has("type")) {
                    return;
                }
                String type = message.get("type").getAsString();
                switch (type) {
                    case "response.audio.delta" -> handleAudioDelta(message);
                    case "response.error" -> log.warn("Qwen TTS returned error event: {}", message);
                    case "session.finished" -> handleFinished();
                    default -> { }
                }
            }

            private void handleAudioDelta(JsonObject message) {
                if (!message.has("delta")) {
                    return;
                }
                String delta = message.get("delta").getAsString();
                if (!StringUtils.hasText(delta)) {
                    return;
                }
                try {
                    audioConsumer.accept(delta);
                } catch (Exception ex) {
                    log.error("Failed to deliver TTS audio delta to consumer.", ex);
                }
            }

            @Override
            public void onClose(int code, String reason) {
                log.debug("Qwen realtime TTS connection closed. code={}, reason={}", code, reason);
                finishedLatch.countDown();
            }

            private void handleFinished() {
                log.debug("Qwen TTS session finished event received.");
                finishedLatch.countDown();
            }
        }
    }
}


