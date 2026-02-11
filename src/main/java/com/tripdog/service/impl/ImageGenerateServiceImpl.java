package com.tripdog.service.impl;

import com.tripdog.ai.model.comfyui.ComfyUiClient;
import com.tripdog.ai.model.comfyui.ComfyUiWorkflowTemplate;
import com.tripdog.common.utils.MinioUtils;
import com.tripdog.model.dto.ImageGenerateReqDTO;
import com.tripdog.model.dto.ImageGenerateRespDTO;
import com.tripdog.service.ImageGenerateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerateServiceImpl implements ImageGenerateService {
    private final ComfyUiClient comfyUiClient;
    private final ComfyUiWorkflowTemplate workflowTemplate;
    private final MinioUtils minioUtils;

    @Override
    public ImageGenerateRespDTO generate(ImageGenerateReqDTO request) {
        var workflow = workflowTemplate.buildWorkflow(request.getWorkflowId(), request.getPrompt());
        var result = comfyUiClient.generate(workflow, request.getClientId(), request.getMaxWaitMs());
        List<String> urls = new ArrayList<>();
        List<String> keys = new ArrayList<>();

        for (ComfyUiClient.ComfyUiImageRef ref : result.images()) {
            ResponseEntity<byte[]> response = comfyUiClient.fetchImage(ref);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                continue;
            }
            String ext = extractExt(ref.filename());
            String objectKey = "comfyui/" + result.promptId() + "/" + UUID.randomUUID() + ext;
            String contentType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : MediaType.IMAGE_PNG_VALUE;
            minioUtils.putObject(objectKey, new ByteArrayInputStream(body), body.length, contentType);
            keys.add(objectKey);
            urls.add(minioUtils.getTemporaryUrlByPath(objectKey));
        }

        ImageGenerateRespDTO resp = new ImageGenerateRespDTO();
        resp.setPromptId(result.promptId());
        resp.setImageUrls(urls);
        resp.setObjectKeys(keys);
        return resp;
    }

    private String extractExt(String filename) {
        if (filename == null) {
            return ".png";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return ".png";
        }
        return filename.substring(idx);
    }
}
