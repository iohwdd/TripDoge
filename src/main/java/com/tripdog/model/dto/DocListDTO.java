package com.tripdog.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 文档列表查询请求DTO
 */
@Data
public class DocListDTO {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 页码（可选，默认 1）
     */
    private Integer page;

    /**
     * 每页大小（可选，默认 10）
     */
    private Integer pageSize;
}
