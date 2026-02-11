package com.tripdog.model.dto;

import lombok.Data;

@Data
public class ImageGenerateReqDTO {
    /**
     * Text prompt. Required.
     */
    private String prompt;

    /**
     * Workflow template id (e.g. sdxl). Optional, default is sdxl.
     */
    private String workflowId;

    /**
     * Optional client id, if empty will be generated.
     */
    private String clientId;

    /**
     * Optional max wait time (ms) for generation.
     */
    private Long maxWaitMs;
}
