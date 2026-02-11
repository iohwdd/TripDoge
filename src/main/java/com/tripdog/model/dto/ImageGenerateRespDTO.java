package com.tripdog.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageGenerateRespDTO {
    private String promptId;
    private List<String> imageUrls;
    private List<String> objectKeys;
}
