package com.tripdog.ai.model.comfyui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComfyUiWorkflowTemplate {
    private static final String TEMPLATE_GLOB = "classpath*:comfyui/template/*.json";
    private static final String DEFAULT_PROMPT = "一位年轻人，靠在樱花树下";

    private static final String DEFAULT_WORKFLOW_ID = "sdxl";

    private final ObjectMapper objectMapper;

    private final Map<String, byte[]> templateStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(TEMPLATE_GLOB);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (!StringUtils.hasText(filename) || !filename.endsWith(".json")) {
                    continue;
                }
                String workflowId = filename.substring(0, filename.length() - 5);
                try (InputStream in = resource.getInputStream()) {
                    templateStore.put(workflowId, in.readAllBytes());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ComfyUI templates from: " + TEMPLATE_GLOB, e);
        }
    }

    public Map<String, Object> buildWorkflow(String workflowId, String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt is required");
        }
        String id = StringUtils.hasText(workflowId) ? workflowId : DEFAULT_WORKFLOW_ID;
        byte[] raw = templateStore.get(id);
        if (raw == null) {
            throw new IllegalArgumentException("workflowId not found: " + id);
        }
        Map<String, Object> copy;
        try {
            copy = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse workflow: " + id, e);
        }
        replacePrompt(copy, prompt);
        randomizeSeeds(copy);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private void replacePrompt(Object node, String prompt) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String str) {
                    if (DEFAULT_PROMPT.equals(str)) {
                        ((Map<Object, Object>) map).put(entry.getKey(), prompt);
                    }
                } else {
                    replacePrompt(value, prompt);
                }
            }
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                replacePrompt(item, prompt);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void randomizeSeeds(Object node) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key instanceof String keyStr && ("noise_seed".equals(keyStr) || "seed".equals(keyStr))) {
                    ((Map<Object, Object>) map).put(key, java.util.concurrent.ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
                } else {
                    randomizeSeeds(value);
                }
            }
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                randomizeSeeds(item);
            }
        }
    }
}
