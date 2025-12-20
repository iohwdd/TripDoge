package com.tripdog.service;

import com.tripdog.model.entity.DocDO;
import com.tripdog.model.vo.DocVO;

import java.util.List;

/**
 * 文档服务接口
 */
public interface DocService {

    /**
     * 保存文档信息
     * @param doc 文档信息
     * @return 是否成功
     */
    boolean saveDoc(DocDO doc);

    /**
     * 根据用户ID和角色ID查询文档列表
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 文档列表
     */
    List<DocVO> getDocsByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * 根据用户ID查询文档列表
     * @param userId 用户ID
     * @return 文档列表
     */
    List<DocVO> getDocsByUserId(Long userId);

    /**
     * 根据文件ID查询文档
     * @param fileId 文件ID
     * @return 文档信息
     */
    DocVO getDocByFileId(String fileId);

    /**
     * 根据文档ID查询文档
     * @param id 文档ID
     * @return 文档信息
     */
    DocVO getDocById(Long id);

    /**
     * 删除文档
     * @param fileId 文件ID
     * @return 是否成功
     */
    boolean deleteDoc(String fileId);

    /**
     * 更新文档状态
     * @param fileId 文件ID
     * @param status 文档状态 0-解析中 1-解析成功 2-解析失败
     * @return 是否成功
     */
    boolean updateDocStatus(String fileId, Integer status);
}
