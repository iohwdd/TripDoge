package com.tripdog.service.impl;

import com.tripdog.mapper.DocMapper;
import com.tripdog.model.entity.DocDO;
import com.tripdog.model.vo.DocVO;
import com.tripdog.service.DocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档服务实现类
 */
@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {

    private final DocMapper docMapper;

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
        vo.setFileSizeFormatted(formatFileSize(doc.getFileSize()));
        vo.setStatus(doc.getStatus());
        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());
        return vo;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long sizeInBytes) {
        if (sizeInBytes == null || sizeInBytes <= 0) {
            return "0 B";
        }

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));

        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }

        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(sizeInBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
