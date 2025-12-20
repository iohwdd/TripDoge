package com.tripdog.service.direct;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量数据管理服务*
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDataService {

    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 根据用户ID删除所有向量数据
     *
     * @param userId 用户ID
     */
    public void deleteByUserId(Long userId) {
        try {
            Filter userFilter = new IsEqualTo("userId", userId);
            embeddingStore.removeAll(userFilter);
            log.info("删除用户 {} 的向量数据", userId);
        } catch (Exception e) {
            log.error("删除用户 {} 的向量数据失败", userId, e);
            throw new RuntimeException("删除用户向量数据失败", e);
        }
    }

    /**
     * 根据角色ID删除所有向量数据
     *
     * @param roleId 角色ID
     */
    public void deleteByRoleId(Long roleId) {
        try {
            Filter roleFilter = new IsEqualTo("roleId", roleId);
            embeddingStore.removeAll(roleFilter);
            log.info("删除角色 {} 的向量数据", roleId);
        } catch (Exception e) {
            log.error("删除角色 {} 的向量数据失败", roleId, e);
            throw new RuntimeException("删除角色向量数据失败", e);
        }
    }

    /**
     * 根据用户ID和角色ID删除向量数据
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 删除的向量数量
     */
    public void deleteByUserIdAndRoleId(Long userId, Long roleId) {
        try {
            // 使用AND条件组合多个过滤器
            Filter userFilter = new IsEqualTo("userId", userId);
            Filter roleFilter = new IsEqualTo("roleId", roleId);
            Filter combinedFilter = Filter.and(userFilter, roleFilter);

            embeddingStore.removeAll(combinedFilter);
            log.info("删除用户 {} 和角色 {} 的向量数据", userId, roleId);
        } catch (Exception e) {
            log.error("删除用户 {} 和角色 {} 的向量数据失败", userId, roleId, e);
            throw new RuntimeException("删除用户和角色向量数据失败", e);
        }
    }

    /**
     * 根据文档ID删除向量数据（如果你在元数据中存储了文档ID）
     *
     * @param fileId 文档ID
     * @return 删除的向量数量
     */
    public void deleteByDocumentId(String fileId) {
        try {
            Filter docFilter = new IsEqualTo("fileId", fileId);
            embeddingStore.removeAll(docFilter);
            log.info("删除文档 {} 的向量数据", fileId);
        } catch (Exception e) {
            log.error("删除文档 {} 的向量数据失败", fileId, e);
            throw new RuntimeException("删除文档向量数据失败", e);
        }
    }

    /**
     * 根据自定义元数据字段删除向量数据
     *
     */
    public void deleteByMetadata(Filter filter) {
        try {
            embeddingStore.removeAll(filter);
            log.info("删除元数据 {} 的向量数据", filter.toString());
        } catch (Exception e) {
            log.info("删除元数据 {} 的向量数据失败", filter.toString());
            throw new RuntimeException("删除向量数据失败", e);
        }
    }

    /**
     * 批量删除指定的向量ID
     *
     * @param embeddingIds 向量ID列表
     * @return 删除的向量数量
     */
    public int deleteByIds(List<String> embeddingIds) {
        try {
            int deletedCount = 0;
            for (String embeddingId : embeddingIds) {
                embeddingStore.remove(embeddingId);
                deletedCount++;
            }
            log.info("批量删除向量数据，共删除 {} 条记录", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("批量删除向量数据失败", e);
            throw new RuntimeException("批量删除向量数据失败", e);
        }
    }

    /**
     * 清空所有向量数据
     *
     * @return 删除的向量数量
     */
    public void deleteAll() {
        try {
            // 删除所有数据，不使用任何过滤器
            embeddingStore.removeAll();
            log.warn("清空所有向量数据，共删除 {} 条记录");
        } catch (Exception e) {
            log.error("清空所有向量数据失败", e);
            throw new RuntimeException("清空向量数据失败", e);
        }
    }
}
