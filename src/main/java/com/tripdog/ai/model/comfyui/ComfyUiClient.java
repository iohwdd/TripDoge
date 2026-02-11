package com.tripdog.ai.model.comfyui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComfyUiClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${comfyui.base-url:http://127.0.0.1:8188}")
    private String baseUrl;

    @Value("${comfyui.poll-interval-ms:1000}")
    private long pollIntervalMs;

        @Value("${comfyui.poll-max-wait-ms:180000}")
    private long pollMaxWaitMs;

    public record ComfyUiImageRef(String filename, String subfolder, String type) {}

    public record ComfyUiResult(String promptId, List<ComfyUiImageRef> images) {}

    public ComfyUiResult generate(Map<String, Object> workflow, String clientId, Long maxWaitMs) {
        if (workflow == null || workflow.isEmpty()) {
            throw new IllegalArgumentException("workflow is required");
        }
        String realClientId = (clientId == null || clientId.isBlank()) ? UUID.randomUUID().toString() : clientId;
        String promptId = submitPrompt(workflow, realClientId);
        long timeoutMs = maxWaitMs != null && maxWaitMs > 0 ? maxWaitMs : pollMaxWaitMs;
        waitForQueue(promptId, timeoutMs);
        List<ComfyUiImageRef> images = pollForImages(promptId, timeoutMs);
        return new ComfyUiResult(promptId, images);
    }

    public ResponseEntity<byte[]> fetchImage(ComfyUiImageRef ref) {
        String url = buildViewUrl(ref);
        return restTemplate.getForEntity(url, byte[].class);
    }

    private String submitPrompt(Map<String, Object> workflow, String clientId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", workflow);
        payload.put("client_id", clientId);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.postForObject(baseUrl + "/prompt", payload, Map.class);
        if (resp == null || resp.get("prompt_id") == null) {
            throw new IllegalStateException("ComfyUI /prompt response missing prompt_id");
        }
        return String.valueOf(resp.get("prompt_id"));
    }

    private List<ComfyUiImageRef> pollForImages(String promptId, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                List<ComfyUiImageRef> images = fetchImagesFromHistory(promptId);
                if (!CollectionUtils.isEmpty(images)) {
                    return images;
                }
            } catch (Exception e) {
                log.warn("ComfyUI history poll failed: {}", e.getMessage());
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("ComfyUI generation timeout");
    }

    private void waitForQueue(String promptId, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                if (!isInQueue(promptId)) {
                    return;
                }
            } catch (Exception e) {
                log.warn("ComfyUI queue poll failed: {}", e.getMessage());
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean isInQueue(String promptId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> queue = restTemplate.getForObject(baseUrl + "/queue", Map.class);
        if (queue == null || queue.isEmpty()) {
            return false;
        }
        if (containsPrompt(queue.get("queue_running"), promptId)) {
            return true;
        }
        if (containsPrompt(queue.get("queue_pending"), promptId)) {
            return true;
        }
        if (containsPrompt(queue.get("running"), promptId)) {
            return true;
        }
        return containsPrompt(queue.get("pending"), promptId);
    }

    private boolean containsPrompt(Object listObj, String promptId) {
        if (!(listObj instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            if (item instanceof List<?> tuple && !tuple.isEmpty()) {
                Object first = tuple.get(0);
                if (promptId.equals(String.valueOf(first))) {
                    return true;
                }
            } else if (item instanceof Map<?, ?> map) {
                Object id = map.get("prompt_id");
                if (id != null && promptId.equals(String.valueOf(id))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ComfyUiImageRef> fetchImagesFromHistory(String promptId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> history = restTemplate.getForObject(baseUrl + "/history/" + promptId, Map.class);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        Object entry = history.get(promptId);
        if (!(entry instanceof Map<?, ?> entryMap)) {
            return List.of();
        }
        Object outputsObj = entryMap.get("outputs");
        Map<String, String> nodeTypeMap = buildNodeTypeMap(entryMap.get("prompt"));
        if (!(outputsObj instanceof Map<?, ?> outputs)) {
            return List.of();
        }

        List<ComfyUiImageRef> images = new ArrayList<>();
        for (Map.Entry<?, ?> outputEntry : outputs.entrySet()) {
            String nodeId = String.valueOf(outputEntry.getKey());
            String classType = nodeTypeMap.get(nodeId);
            if (!(outputEntry.getValue() instanceof Map<?, ?> outputMap)) {
                continue;
            }
            Object imagesObj = outputMap.get("images");
            if (!(imagesObj instanceof List<?> imageList)) {
                continue;
            }
            for (Object imgObj : imageList) {
                if (!(imgObj instanceof Map<?, ?> imgMap)) {
                    continue;
                }
                String type = asString(imgMap.get("type"));
                if (type != null && !"output".equals(type)) {
                    continue;
                }
                if (classType != null && !"SaveImage".equals(classType)) {
                    continue;
                }
                String filename = asString(imgMap.get("filename"));
                String subfolder = asString(imgMap.get("subfolder"));
                if (filename != null) {
                    images.add(new ComfyUiImageRef(filename, subfolder, type));
                }
            }
        }
        return images;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> buildNodeTypeMap(Object promptObj) {
        if (!(promptObj instanceof Map<?, ?> promptMap)) {
            return Map.of();
        }
        Map<String, String> nodeTypeMap = new HashMap<>();
        for (Map.Entry<?, ?> entry : promptMap.entrySet()) {
            String nodeId = String.valueOf(entry.getKey());
            if (entry.getValue() instanceof Map<?, ?> nodeMap) {
                Object classType = nodeMap.get("class_type");
                if (classType != null) {
                    nodeTypeMap.put(nodeId, String.valueOf(classType));
                }
            }
        }
        return nodeTypeMap;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String buildViewUrl(ComfyUiImageRef ref) {
        StringBuilder sb = new StringBuilder(baseUrl).append("/view?filename=")
            .append(urlEncode(ref.filename()));
        if (ref.subfolder() != null && !ref.subfolder().isBlank()) {
            sb.append("&subfolder=").append(urlEncode(ref.subfolder()));
        }
        if (ref.type() != null && !ref.type().isBlank()) {
            sb.append("&type=").append(urlEncode(ref.type()));
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
