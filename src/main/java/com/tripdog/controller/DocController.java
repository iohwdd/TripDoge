package com.tripdog.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tripdog.common.enums.DocParseStatus;
import com.tripdog.common.utils.FileUtil;
import com.tripdog.exception.BussinessException;
import com.tripdog.exception.DocumentParseException;
import com.tripdog.model.dto.*;
import com.tripdog.service.direct.CloudFileService;
import jakarta.annotation.PostConstruct;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import com.tripdog.common.utils.FileUploadUtils;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.config.MinioConfig;
import com.tripdog.service.direct.UserSessionService;
import com.tripdog.service.direct.VectorDataService;
import com.tripdog.model.entity.DocDO;
import com.tripdog.model.vo.DocVO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.DocService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static com.tripdog.common.Constants.*;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 * 文档管理控制器
 * @author: iohw
 * @date: 2025/9/26 15:14
 * @description: 文档上传、下载、删除、列表查询等功能
 */
@RestController
@RequestMapping("/doc")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文档管理", description = "文档上传、解析、下载、删除和向量化相关接口")
public class DocController {
    private final UserSessionService userSessionService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final DocService docService;
    private final VectorDataService vectorDataService;

    @PostMapping(path = "/parse", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "文档上传并解析",
              description = "上传文件到MinIO存储，然后解析文档内容并创建向量嵌入用于AI检索")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文档上传并解析成功"),
        @ApiResponse(responseCode = "10001", description = "参数错误"),
        @ApiResponse(responseCode = "10105", description = "用户未登录"),
        @ApiResponse(responseCode = "10000", description = "系统异常")
    })
    public SseEmitter upload(UploadDTO uploadDTO) {
        return docService.docParse(uploadDTO);
    }


    @PostMapping("/list")
    @Operation(summary = "查询文档列表",
              description = "根据用户ID和角色ID查询文档列表，如果不传角色ID则查询用户所有文档")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "10105", description = "用户未登录")
    })
    public Result<List<DocVO>> list(@RequestBody DocListDTO docListDTO) {
        // 从用户会话服务获取当前登录用户信息
        UserInfoVO userInfoVO = userSessionService.getCurrentUser();
        if (userInfoVO == null) {
            throw new RuntimeException(ErrorCode.USER_NOT_LOGIN.getMessage());
        }

        List<DocVO> docs;
        if (docListDTO != null && docListDTO.getRoleId() != null) {
            // 查询指定角色的文档
            docs = docService.getDocsByUserIdAndRoleId(userInfoVO.getId(), docListDTO.getRoleId());
        } else {
            // 查询用户所有文档
            docs = docService.getDocsByUserId(userInfoVO.getId());
        }

        return Result.success(docs);
    }

    @PostMapping("/download")
    @Operation(summary = "下载文档",
              description = "根据文件ID下载文档")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "下载成功"),
        @ApiResponse(responseCode = "10001", description = "参数错误"),
        @ApiResponse(responseCode = "10105", description = "用户未登录"),
        @ApiResponse(responseCode = "10404", description = "文件不存在")
    })
    public ResponseEntity<byte[]> download(@RequestBody @Validated DocDownloadDTO downloadDTO) {
        try {
            // 从用户会话服务获取当前登录用户信息
            UserInfoVO userInfoVO = userSessionService.getCurrentUser();
            if (userInfoVO == null) {
                 throw new RuntimeException(ErrorCode.USER_NOT_LOGIN.getMessage());
            }

            // 查询文档信息
            DocVO docVO = docService.getDocByFileId(downloadDTO.getFileId());
            if (docVO == null) {
                return ResponseEntity.notFound().build();
            }

            // 检查文档所有权
            if (!userInfoVO.getId().equals(docVO.getUserId())) {
                return ResponseEntity.badRequest().build();
            }

            // 从MinIO下载文件
            InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(docVO.getFileUrl())
                    .build()
            );
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                URLEncoder.encode(docVO.getFileName(), StandardCharsets.UTF_8));
            headers.setContentLength(bytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);

        } catch (Exception e) {
            log.error("文档下载异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/delete")
    @Operation(summary = "删除文档",
              description = "根据文件ID删除文档（包括MinIO中的文件、数据库记录和对应的向量数据）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "10105", description = "用户未登录"),
        @ApiResponse(responseCode = "10404", description = "文件不存在")
    })
    public Result<String> delete(@RequestBody DocDelDTO docDelDTO) {
        try {
            // 直接从用户会话服务获取用户信息
            UserInfoVO userInfoVO = userSessionService.getCurrentUser();
            if (userInfoVO == null) {
                 return Result.error(ErrorCode.USER_NOT_LOGIN);
            }

            String fileId = docDelDTO.getFileId();
            // 查询文档信息
            DocVO docVO = docService.getDocByFileId(fileId);
            if (docVO == null) {
                return Result.error(ErrorCode.NOT_FOUND);
            }

            // 检查文档所有权
            if (!userInfoVO.getId().equals(docVO.getUserId())) {
                return Result.error(ErrorCode.NO_AUTH);
            }

            // 从MinIO删除文件
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(docVO.getFileUrl())
                    .build()
            );

            // 删除对应的向量数据
            vectorDataService.deleteByDocumentId(fileId);

            // 从数据库删除记录
            if (!docService.deleteDoc(fileId)) {
                log.error("删除数据库文档记录失败: {}", fileId);
                return Result.error(ErrorCode.SYSTEM_ERROR);
            }

            return Result.success("文档删除成功");

        } catch (Exception e) {
            log.error("文档删除异常", e);
            return Result.error(ErrorCode.SYSTEM_ERROR);
        }
    }
}
