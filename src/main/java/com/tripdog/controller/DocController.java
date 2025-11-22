package com.tripdog.controller;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import com.tripdog.common.utils.FileUploadUtils;
import com.tripdog.common.utils.FileValidationUtils;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.config.MinioConfig;
import com.tripdog.model.dto.DocDelDTO;
import com.tripdog.service.impl.UserSessionService;
import com.tripdog.service.impl.VectorDataService;
import com.tripdog.model.dto.DocDeleteResultDTO;
import com.tripdog.model.dto.DocDownloadDTO;
import com.tripdog.model.dto.DocListDTO;
import com.tripdog.model.dto.FileUploadDTO;
import com.tripdog.model.dto.UploadDTO;
import com.tripdog.model.entity.DocDO;
import com.tripdog.model.vo.DocVO;
import com.tripdog.model.vo.DocUploadResultVO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.DocService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.tripdog.common.Constants.FILE_ID;
import static com.tripdog.common.Constants.FILE_NAME;
import static com.tripdog.common.Constants.ROLE_ID;
import static com.tripdog.common.Constants.UPLOAD_TIME;
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

    private final EmbeddingStoreIngestor ingestor;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final DocService docService;
    private final UserSessionService userSessionService;
    private final VectorDataService vectorDataService;
    private final FileUploadUtils fileUploadUtils;

    @PostMapping("/parse")
    @Operation(summary = "文档上传并解析",
              description = "上传文件到MinIO存储，然后解析文档内容并创建向量嵌入用于AI检索")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文档上传并解析成功"),
        @ApiResponse(responseCode = "10001", description = "参数错误"),
        @ApiResponse(responseCode = "10105", description = "用户未登录"),
        @ApiResponse(responseCode = "10000", description = "系统异常")
    })
    public Result<DocUploadResultVO> upload(UploadDTO uploadDTO) {
        // 直接从用户会话服务获取用户信息
        UserInfoVO userInfoVO = userSessionService.getCurrentUser();
        if (userInfoVO == null) {
             return Result.error(ErrorCode.USER_NOT_LOGIN);
        }
        MultipartFile file = uploadDTO.getFile();
        
        // 验证文件大小和类型（在业务层再次验证，确保安全）
        try {
            FileValidationUtils.validateDocumentFile(file);
        } catch (IllegalArgumentException e) {
            log.warn("文件验证失败: {}", e.getMessage());
            return Result.error(ErrorCode.PARAM_ERROR, e.getMessage());
        }
        
        String fileId = UUID.randomUUID().toString();
        String objectKey = null;
        boolean docRecordCreated = false;

        // 设置元数据到ThreadLocal，用于向量存储时添加
        ThreadLocalUtils.set(ROLE_ID, uploadDTO.getRoleId());
        ThreadLocalUtils.set(FILE_ID, fileId);
        ThreadLocalUtils.set(FILE_NAME, file.getOriginalFilename());
        ThreadLocalUtils.set(UPLOAD_TIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 上传文件到MinIO（不再同时上传到本地，避免双重写入）
            FileUploadDTO fileUploadDTO = fileUploadUtils.upload2Minio(
                file,
                userInfoVO.getId(),
                "/doc"
            );
            objectKey = fileUploadDTO.getObjectKey();

            // 保存文档信息到数据库
            DocDO docDO = new DocDO();
            docDO.setFileId(fileId);
            docDO.setUserId(userInfoVO.getId());
            docDO.setRoleId(uploadDTO.getRoleId());
            docDO.setFileUrl(fileUploadDTO.getObjectKey());
            docDO.setFileName(file.getOriginalFilename());
            docDO.setFileSize((double) file.getSize());

            if (!docService.saveDoc(docDO)) {
                log.error("保存文档信息到数据库失败: {}", docDO);
                cleanupUploadedDocument(objectKey, fileId, false);
                return Result.error(ErrorCode.SYSTEM_ERROR, "保存文档信息失败，已回滚上传");
            }
            docRecordCreated = true;

            // 从MinIO下载文件到本地临时目录用于解析（避免双重写入）
            // 注意：这里需要先从MinIO下载，因为Apache Tika需要本地文件路径
            // 临时方案：先上传到本地临时目录用于解析，解析完成后删除
            FileUploadDTO localFileDTO = FileUploadUtils.upload2Local(file, "/tmp");
            File localFile = new File(localFileDTO.getFilePath());

            boolean vectorizationSuccess = false;
            String vectorizationError = null;
            Boolean isConfigError = null;
            
            try {
                // 解析文档并创建向量嵌入（允许失败，不影响上传）
                try {
                    DocumentParser parser = new ApacheTikaDocumentParser();
                    Document doc = loadDocument(localFile.getAbsolutePath(), parser);
                    ingestor.ingest(doc);
                    vectorizationSuccess = true;
                    log.info("文档向量化成功: fileId={}, fileName={}", fileId, file.getOriginalFilename());
                } catch (Exception e) {
                    vectorizationError = e.getMessage();
                    String errorMsg = e.getMessage();
                    
                    // 判断是否为配置错误（向量存储未配置、连接失败等）
                    // 配置错误：向量存储不可用、连接失败等
                    // 临时错误：解析失败、网络超时等
                    if (errorMsg != null && (
                        errorMsg.contains("Connection") || 
                        errorMsg.contains("连接") ||
                        errorMsg.contains("unavailable") ||
                        errorMsg.contains("不可用") ||
                        errorMsg.contains("not configured") ||
                        errorMsg.contains("未配置"))) {
                        isConfigError = true;
                        log.warn("文档向量化失败（配置错误，文件已上传但无法进行AI检索）: fileId={}, fileName={}, error={}", 
                            fileId, file.getOriginalFilename(), vectorizationError);
                    } else {
                        isConfigError = false;
                        log.warn("文档向量化失败（临时错误，文件已上传但无法进行AI检索）: fileId={}, fileName={}, error={}", 
                            fileId, file.getOriginalFilename(), vectorizationError);
                    }
                    // 不抛出异常，允许上传成功，但记录警告
                }

                // 返回文档信息和向量化状态
                DocVO docVO = docService.getDocByFileId(fileId);
                
                // 构建上传结果
                DocUploadResultVO uploadResult = DocUploadResultVO.builder()
                    .doc(docVO)
                    .vectorizationSuccess(vectorizationSuccess)
                    .vectorizationError(vectorizationError)
                    .isConfigError(isConfigError)
                    .build();
                
                if (vectorizationSuccess) {
                    return Result.success(uploadResult);
                } else {
                    if (Boolean.FALSE.equals(isConfigError)) {
                        log.warn("文档解析失败，执行回滚: fileId={}, fileName={}", fileId, file.getOriginalFilename());
                        cleanupUploadedDocument(objectKey, fileId, true);
                        return Result.error(ErrorCode.SYSTEM_ERROR, "文档解析失败，上传已回滚，请稍后重试");
                    }

                    // 向量化失败（配置问题），返回警告信息
                    String message = "文档上传成功，但向量化功能未配置，无法进行AI检索";
                    return Result.success(message, uploadResult);
                }
            } finally {
                // 清理本地临时文件（确保清理失败不影响主流程，但记录日志）
                try {
                    FileUploadUtils.deleteLocalFile(localFile);
                    log.debug("临时文件清理成功: {}", localFile.getAbsolutePath());
                } catch (Exception e) {
                    // 清理失败不影响主流程，但记录警告日志
                    log.warn("临时文件清理失败，文件可能残留: file={}, error={}", 
                        localFile.getAbsolutePath(), e.getMessage());
                    // 记录到待清理列表，由定时任务补偿清理
                    // 注意：这里不抛出异常，避免影响主流程
                }
            }
        } catch (Exception e) {
            log.error("文档上传处理异常，将回滚已上传文件: fileId={}, objectKey={}, error={}",
                fileId, objectKey, e.getMessage(), e);
            cleanupUploadedDocument(objectKey, fileId, docRecordCreated);
            return Result.error(ErrorCode.SYSTEM_ERROR, "文档上传失败，已回滚，请稍后重试");
        } finally {
            ThreadLocalUtils.remove(ROLE_ID);
            ThreadLocalUtils.remove(FILE_ID);
            ThreadLocalUtils.remove(FILE_NAME);
            ThreadLocalUtils.remove(UPLOAD_TIME);
        }
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
            InputStream inputStream = null;
            try {
                inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(docVO.getFileUrl())
                        .build()
                );
                byte[] bytes = inputStream.readAllBytes();

                // 设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment",
                    URLEncoder.encode(docVO.getFileName(), StandardCharsets.UTF_8));
                headers.setContentLength(bytes.length);

                return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
            } catch (MinioException e) {
                log.error("MinIO下载失败: fileId={}, fileUrl={}, error={}", 
                    downloadDTO.getFileId(), docVO.getFileUrl(), e.getMessage(), e);
                
                // 检查是否是文件不存在
                String errorMsg = e.getMessage();
                boolean isNotFound = errorMsg != null && 
                    (errorMsg.contains("NoSuchKey") || 
                     errorMsg.contains("does not exist") ||
                     errorMsg.contains("Object does not exist"));
                
                if (isNotFound) {
                    log.warn("文件在MinIO中不存在: fileId={}, fileUrl={}", downloadDTO.getFileId(), docVO.getFileUrl());
                    // 返回404，设置正确的Content-Type
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    String errorResponse = ErrorCode.NO_FOUND_FILE.getMessage();
                    return ResponseEntity.status(404)
                        .headers(headers)
                        .body(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
                
                // MinIO连接失败或其他错误，返回503
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                String errorResponse = ErrorCode.FILE_STORAGE_UNAVAILABLE.getMessage();
                return ResponseEntity.status(503)
                    .headers(headers)
                    .body(errorResponse.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("文档下载异常: fileId={}, error={}", downloadDTO.getFileId(), e.getMessage(), e);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                String errorResponse = ErrorCode.FILE_DOWNLOAD_FAILED.getMessage();
                return ResponseEntity.status(500)
                    .headers(headers)
                    .body(errorResponse.getBytes(StandardCharsets.UTF_8));
            } finally {
                // 确保资源关闭
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        log.warn("关闭输入流失败", e);
                    }
                }
            }

        } catch (RuntimeException e) {
            // 处理用户未登录等业务异常
            if (e.getMessage() != null && e.getMessage().contains("用户未登录")) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                return ResponseEntity.status(401)
                    .headers(headers)
                    .body(ErrorCode.USER_NOT_LOGIN.getMessage().getBytes(StandardCharsets.UTF_8));
            }
            log.error("文档下载业务异常", e);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return ResponseEntity.status(400)
                .headers(headers)
                .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("文档下载未知异常", e);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            String errorResponse = ErrorCode.SYSTEM_ERROR.getMessage();
            return ResponseEntity.status(500)
                .headers(headers)
                .body(errorResponse.getBytes(StandardCharsets.UTF_8));
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
    public Result<DocDeleteResultDTO> delete(@RequestBody DocDelDTO docDelDTO) {
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

            // 删除操作：MinIO -> 向量数据 -> 数据库
            // 即使某个步骤失败，也继续执行后续步骤，避免数据不一致
            
            // 1. 从MinIO删除文件（允许失败）
            boolean minioDeleted = false;
            String minioError = null;
            try {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(docVO.getFileUrl())
                        .build()
                );
                minioDeleted = true;
                log.info("MinIO文件删除成功: fileId={}, fileUrl={}", fileId, docVO.getFileUrl());
            } catch (MinioException e) {
                // 检查是否是文件不存在（可能已经删除）
                String errorMsg = e.getMessage();
                if (errorMsg != null && 
                    (errorMsg.contains("NoSuchKey") || 
                     errorMsg.contains("does not exist") ||
                     errorMsg.contains("Object does not exist"))) {
                    log.warn("MinIO文件不存在（可能已删除）: fileId={}, fileUrl={}", fileId, docVO.getFileUrl());
                    minioDeleted = true; // 文件不存在视为删除成功
                } else {
                    minioError = e.getMessage();
                    log.warn("MinIO文件删除失败（继续执行其他删除操作）: fileId={}, fileUrl={}, error={}", 
                        fileId, docVO.getFileUrl(), minioError);
                }
            } catch (Exception e) {
                minioError = e.getMessage();
                log.warn("MinIO文件删除异常（继续执行其他删除操作）: fileId={}, fileUrl={}, error={}", 
                    fileId, docVO.getFileUrl(), minioError);
            }

            // 2. 删除对应的向量数据（允许失败）
            boolean vectorDeleted = false;
            String vectorError = null;
            try {
                vectorDataService.deleteByDocumentId(fileId);
                vectorDeleted = true;
                log.info("向量数据删除成功: fileId={}", fileId);
            } catch (Exception e) {
                vectorError = e.getMessage();
                log.warn("向量数据删除失败（继续执行数据库删除）: fileId={}, error={}", fileId, vectorError);
            }

            // 3. 从数据库删除记录（必须成功）
            boolean dbDeleted = docService.deleteDoc(fileId);
            if (!dbDeleted) {
                log.error("删除数据库文档记录失败: fileId={}", fileId);
                return Result.error(ErrorCode.SYSTEM_ERROR, "数据库删除失败，文档可能已被删除");
            }
            log.info("数据库文档记录删除成功: fileId={}", fileId);

            // 构建删除结果
            boolean allSuccess = minioDeleted && vectorDeleted && dbDeleted;
            DocDeleteResultDTO deleteResult = DocDeleteResultDTO.builder()
                .minioDeleted(minioDeleted)
                .vectorDeleted(vectorDeleted)
                .dbDeleted(dbDeleted)
                .allSuccess(allSuccess)
                .message(buildDeleteMessage(minioDeleted, vectorDeleted, dbDeleted, minioError, vectorError))
                .build();

            if (allSuccess) {
                return Result.success("文档删除成功", deleteResult);
            } else {
                // 部分成功，返回警告状态（使用自定义code，前端可以根据code判断）
                Result<DocDeleteResultDTO> result = Result.error(ErrorCode.DOC_DELETE_PARTIAL_SUCCESS.getCode(), 
                    deleteResult.getMessage());
                result.setData(deleteResult);
                return result;
            }

        } catch (Exception e) {
            log.error("文档删除异常: fileId={}", docDelDTO.getFileId(), e);
            return Result.error(ErrorCode.SYSTEM_ERROR);
        }
    }
    
    /**
     * 构建删除结果消息
     */
    private String buildDeleteMessage(boolean minioDeleted, boolean vectorDeleted, 
                                     boolean dbDeleted, String minioError, String vectorError) {
        StringBuilder message = new StringBuilder();
        message.append("文档删除完成。");
        
        if (dbDeleted) {
            message.append("数据库记录已删除。");
        } else {
            message.append("数据库记录删除失败。");
        }
        
        if (minioDeleted) {
            message.append("MinIO文件已删除。");
        } else {
            message.append("MinIO文件删除失败");
            if (minioError != null) {
                message.append(": ").append(minioError);
            }
            message.append("。");
        }
        
        if (vectorDeleted) {
            message.append("向量数据已删除。");
        } else {
            message.append("向量数据删除失败");
            if (vectorError != null) {
                message.append(": ").append(vectorError);
            }
            message.append("。");
        }
        
        return message.toString();
    }

    private void cleanupUploadedDocument(String objectKey, String fileId, boolean docRecordCreated) {
        if (docRecordCreated && StringUtils.hasText(fileId)) {
            try {
                boolean removed = docService.deleteDoc(fileId);
                log.debug("回滚文档记录: fileId={}, success={}", fileId, removed);
            } catch (Exception ex) {
                log.warn("回滚文档记录失败: fileId={}, error={}", fileId, ex.getMessage());
            }
        }

        if (StringUtils.hasText(objectKey)) {
            try {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectKey)
                        .build()
                );
                log.debug("回滚MinIO文件成功: objectKey={}", objectKey);
            } catch (Exception ex) {
                log.warn("回滚MinIO文件失败: objectKey={}, error={}", objectKey, ex.getMessage());
            }
        }
    }
}
