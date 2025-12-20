package com.tripdog.mapper;

import com.tripdog.model.entity.DocDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档数据访问层
 */
@Mapper
public interface DocMapper {

    /**
     * 插入文档记录
     * @param doc 文档信息
     * @return 影响行数
     */
    int insert(DocDO doc);

    /**
     * 根据文件ID查询文档
     * @param fileId 文件ID
     * @return 文档信息
     */
    DocDO selectByFileId(@Param("fileId") String fileId);

    /**
     * 根据用户ID和角色ID查询文档列表
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 文档列表
     */
    List<DocDO> selectByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 根据文件ID删除文档
     * @param fileId 文件ID
     * @return 影响行数
     */
    int deleteByFileId(@Param("fileId") String fileId);

    /**
     * 根据用户ID查询文档列表
     * @param userId 用户ID
     * @return 文档列表
     */
    List<DocDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据ID查询文档
     * @param id 文档ID
     * @return 文档信息
     */
    DocDO selectById(@Param("id") Long id);

    /**
     * 根据文件ID更新文档状态
     * @param fileId 文件ID
     * @param status 文档状态 0-解析中 1-解析成功 2-解析失败
     * @return 影响行数
     */
    int updateStatusByFileId(@Param("fileId") String fileId, @Param("status") Integer status);
}
