package com.tripdog.service.impl;

import com.tripdog.common.enums.DocParseStatus;
import com.tripdog.common.utils.FileUploadUtils;
import com.tripdog.common.utils.FileUtil;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.exception.DocumentParseException;
import com.tripdog.mapper.DocMapper;
import com.tripdog.model.dto.DocParseDTO;
import com.tripdog.model.dto.FileUploadDTO;
import com.tripdog.model.dto.UploadDTO;
import com.tripdog.model.entity.DocDO;
import com.tripdog.model.vo.DocVO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.DocService;
import com.tripdog.service.direct.CloudFileService;
import com.tripdog.service.direct.UserSessionService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.tripdog.common.Constants.*;
import static com.tripdog.common.Constants.UPLOAD_TIME;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static io.prometheus.metrics.model.snapshots.Exemplar.TRACE_ID;

/**
 * 文档服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocServiceImpl implements DocService {
    private final String THREAD_POOL_NAME = "doc-pool";
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private final DocMapper docMapper;
    private final UserSessionService userSessionService;
    private final CloudFileService cloudFileService;
    private final EmbeddingStoreIngestor ingestor;

    private ThreadPoolExecutor docParseExecutor;

    @PostConstruct
    public void init() {
        docParseExecutor = new ThreadPoolExecutor(
                5,
                10,
                30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(30),
                (r) -> new Thread(r, THREAD_POOL_NAME + "-" + threadCounter.addAndGet(1)),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    @Override
    public SseEmitter docParse(UploadDTO uploadDTO) {
        long start = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(0L);
        // 验证参数
        if (uploadDTO == null || uploadDTO.getFile() == null) {
            sendErrorAndComplete(emitter, "参数错误");
            return emitter;
        }

        if (!FileUtil.isTextFile(uploadDTO.getFile().getOriginalFilename())) {
            sendErrorAndComplete(emitter, "不支持的文件类型，只支持文本文件");
            return emitter;
        }

        UserInfoVO userInfoVO = userSessionService.getCurrentUser();
        if (userInfoVO == null) {
            sendErrorAndComplete(emitter, "用户未登录");
            return emitter;
        }

        MultipartFile file = uploadDTO.getFile();
        DocParseDTO docParseDTO = DocParseDTO.builder()
                .file(file)
                .userId(userInfoVO.getId())
                .roleId(uploadDTO.getRoleId())
                .build();
        try {
            // 异步解析文档
            String traceId = MDC.get(TRACE_ID);
            docParseExecutor.execute(() -> {
                try {
                    MDC.put(TRACE_ID, traceId);
                    parseDocumentAsync(emitter, docParseDTO);
                    log.info("fileName: {}, fileSize: {}, doc parse time cost: {}s", file.getOriginalFilename(), FileUtil.formatFileSize(file.getSize()), (System.currentTimeMillis() - start) / 1000);
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    throw new DocumentParseException(e);
                } finally {
                    emitter.complete();
                }
            });
            return emitter;
        } catch (Exception e) {
            log.error("文档上传处理异常", e);
            sendErrorAndComplete(emitter, "文档上传异常");
            return emitter;
        } finally {
            ThreadLocalUtils.remove(ROLE_ID);
            ThreadLocalUtils.remove(FILE_ID);
            ThreadLocalUtils.remove(FILE_NAME);
            ThreadLocalUtils.remove(UPLOAD_TIME);
        }
    }

    private void parseDocumentAsync(SseEmitter emitter, DocParseDTO dto) throws IOException {
        Long userId = dto.getUserId();
        Long roleId = dto.getRoleId();
        MultipartFile file = dto.getFile();
        // 上传文件到MinIO
        String objectKey = "doc/" + userId + "/" + UUID.randomUUID() + FileUtil.getFileSuffix(file.getOriginalFilename());
        cloudFileService.putObject(file, objectKey);

        // 保存文档信息到数据库
        String fileId = UUID.randomUUID().toString();
        DocDO docDO = new DocDO();
        docDO.setUserId(userId);
        docDO.setRoleId(roleId);
        docDO.setFileUrl(objectKey);
        docDO.setFileName(file.getOriginalFilename());
        docDO.setFileSize(file.getSize());
        docDO.setStatus(DocParseStatus.PARSING.getStatus());
        docDO.setFileId(fileId);
        if (!saveDoc(docDO)) {
            log.error("保存文档信息到数据库失败: {}", docDO);
            sendErrorAndComplete(emitter, "文档数据保存失败");
        }
        // 设置切片元数据
        ThreadLocalUtils.set(FILE_ID, fileId);
        ThreadLocalUtils.set(ROLE_ID, docDO.getRoleId());
        ThreadLocalUtils.set(USER_ID, userId);
        ThreadLocalUtils.set(FILE_NAME, file.getOriginalFilename());
        ThreadLocalUtils.set(UPLOAD_TIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        FileUploadDTO localFileDTO = null;
        File localFile = null;
        try {
            // 上传到本地临时目录用于解析
            localFileDTO = FileUploadUtils.upload2Local(file, "/tmp");
            localFile = new File(localFileDTO.getFilePath());

            // 发送解析中的消息
            emitter.send(SseEmitter.event().name("progress").data("parsing"));
            // 解析文档并创建向量嵌入
            DocumentParser parser = new ApacheTikaDocumentParser();
            Document doc = loadDocument(localFile.getAbsolutePath(), parser);
            ingestor.ingest(doc);

            // 成功完成
            updateDocStatus(fileId, DocParseStatus.SUCCESS.getStatus());
            emitter.send(SseEmitter.event().name("progress").data("success"));
            emitter.send(SseEmitter.event().name("done").data(""));
        } catch (Exception e) {
            log.warn("userid: {}, fileName: {}, doc parse error: {}", docDO.getUserId(), file.getOriginalFilename(), e.getMessage());
            updateDocStatus(fileId, DocParseStatus.FAIL.getStatus());
            emitter.send(SseEmitter.event().name("progress").data("fail"));
        } finally {
            // 清理本地临时文件
            if (localFile != null) {
                FileUtil.deleteLocalFile(localFile);
            }
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String errorMsg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(errorMsg));
            emitter.complete();
        } catch (IOException e) {
            log.warn("Failed to send error event: {}", e.getMessage());
        }
    }

    @Override
    public boolean saveDoc(DocDO doc) {
        return docMapper.insert(doc) > 0;
    }

    @Override
    public List<DocVO> getDocsByUserIdAndRoleId(Long userId, Long roleId) {
        List<DocDO> docs = docMapper.selectByUserIdAndRoleId(userId, roleId);
        return docs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocVO> getDocsByUserId(Long userId) {
        List<DocDO> docs = docMapper.selectByUserId(userId);
        return docs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public DocVO getDocByFileId(String fileId) {
        DocDO doc = docMapper.selectByFileId(fileId);
        return doc != null ? convertToVO(doc) : null;
    }

    @Override
    public DocVO getDocById(Long id) {
        DocDO doc = docMapper.selectById(id);
        return doc != null ? convertToVO(doc) : null;
    }

    @Override
    public boolean deleteDoc(String fileId) {
        return docMapper.deleteByFileId(fileId) > 0;
    }

    @Override
    public boolean updateDocStatus(String fileId, Integer status) {
        return  docMapper.updateStatusByFileId(fileId, status) == 1;
    }


    /**
     * 转换为VO对象
     */
    private DocVO convertToVO(DocDO doc) {
        DocVO vo = new DocVO();
        vo.setId(doc.getId());
        vo.setFileId(doc.getFileId());
        vo.setUserId(doc.getUserId());
        vo.setRoleId(doc.getRoleId());
        vo.setFileUrl(doc.getFileUrl());
        vo.setFileName(doc.getFileName());
        vo.setFileSize(doc.getFileSize());
        vo.setFileSizeFormatted(FileUtil.formatFileSize(doc.getFileSize()));
        vo.setStatus(doc.getStatus());
        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());
        return vo;
    }



}
