package com.tripdog.model.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class DocParseDTO {
    Long roleId;
    Long userId;
    MultipartFile file;
}
