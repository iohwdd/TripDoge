package com.tripdog.controller;

import com.tripdog.common.Result;
import com.tripdog.model.dto.FileGetDTO;
import com.tripdog.model.vo.FileVO;
import com.tripdog.service.direct.CloudFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    @Autowired
    private CloudFileService cloudFileService;

    /**
     * 获取文件下载 URL
     * 该 URL 包含 Content-Disposition: attachment，浏览器访问会直接下载
     */
    @PostMapping("/get")
    public Result<FileVO> get(@RequestBody FileGetDTO dto) {
        return Result.success(FileVO.builder()
                .url(cloudFileService.getFileDownloadUrl(dto.getObjectKey(), dto.getFileName()))
                .build());
    }

}
