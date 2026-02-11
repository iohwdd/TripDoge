package com.tripdog.service;

import com.tripdog.model.dto.ImageGenerateReqDTO;
import com.tripdog.model.dto.ImageGenerateRespDTO;

public interface ImageGenerateService {
    ImageGenerateRespDTO generate(ImageGenerateReqDTO request);
}
