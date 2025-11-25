package com.tripdog.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/**
 * 临时文件清理服务
 * 定期清理残留的临时文件，防止磁盘空间耗尽
 */
@Slf4j
@Service
public class TempFileCleanupService {

    /**
     * 临时文件目录
     */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir", "/tmp");
    
    /**
     * 临时文件前缀（用于识别项目创建的临时文件）
     */
    private static final String TEMP_FILE_PREFIX = "tripdog_";
    
    /**
     * 临时文件最大保留时间（小时）
     * 超过此时间的临时文件将被清理
     */
    private static final long MAX_TEMP_FILE_AGE_HOURS = 24;

    /**
     * 定期清理临时文件
     * 每6小时执行一次
     */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000) // 6小时
    public void cleanupTempFiles() {
        try {
            Path tempDir = Paths.get(TEMP_DIR);
            if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
                log.warn("临时文件目录不存在或不是目录: {}", TEMP_DIR);
                return;
            }

            Instant cutoffTime = Instant.now().minus(MAX_TEMP_FILE_AGE_HOURS, ChronoUnit.HOURS);
            int cleanedCount = 0;
            long totalSize = 0;

            try (Stream<Path> paths = Files.list(tempDir)) {
                for (Path path : paths.toList()) {
                    File file = path.toFile();
                    
                    // 只清理项目创建的临时文件（以tripdog_开头）
                    if (!file.getName().startsWith(TEMP_FILE_PREFIX)) {
                        continue;
                    }

                    // 检查文件最后修改时间
                    FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                    if (lastModifiedTime.toInstant().isBefore(cutoffTime)) {
                        try {
                            long fileSize = Files.size(path);
                            if (Files.deleteIfExists(path)) {
                                cleanedCount++;
                                totalSize += fileSize;
                                log.debug("清理过期临时文件: {}, 大小: {} bytes", path, fileSize);
                            }
                        } catch (Exception e) {
                            log.warn("清理临时文件失败: {}, error={}", path, e.getMessage());
                        }
                    }
                }
            }

            if (cleanedCount > 0) {
                log.info("临时文件清理完成: 清理数量={}, 释放空间={} MB", 
                    cleanedCount, totalSize / (1024 * 1024));
            } else {
                log.debug("临时文件清理完成: 无需清理的文件");
            }
        } catch (Exception e) {
            log.error("临时文件清理服务执行异常", e);
        }
    }

    /**
     * 手动清理指定文件
     * 
     * @param filePath 文件路径
     * @return 是否清理成功
     */
    public boolean cleanupFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                boolean deleted = Files.deleteIfExists(path);
                if (deleted) {
                    log.info("手动清理临时文件成功: {}", filePath);
                    return true;
                } else {
                    log.warn("手动清理临时文件失败，文件不存在: {}", filePath);
                    return false;
                }
            } else {
                log.debug("临时文件不存在，无需清理: {}", filePath);
                return true; // 文件不存在视为清理成功
            }
        } catch (Exception e) {
            log.error("手动清理临时文件异常: filePath={}, error={}", filePath, e.getMessage(), e);
            return false;
        }
    }
}



