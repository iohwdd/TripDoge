package com.tripdog.service.impl;

import java.util.List;

import com.tripdog.common.utils.MinioUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tripdog.mapper.RoleMapper;
import com.tripdog.model.converter.RoleConverter;
import com.tripdog.model.entity.RoleDO;
import com.tripdog.model.vo.RoleInfoVO;
import com.tripdog.model.vo.RoleDetailVO;
import com.tripdog.service.RoleService;
import com.tripdog.common.utils.RoleConfigParser;

import lombok.RequiredArgsConstructor;

/**
 * @author: iohw
 * @date: 2025/9/24 20:31
 * @description:
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    @Value("${minio.bucket-name}")
    private String bucketName;

    final RoleMapper roleMapper;
    final MinioUtils minioUtils;

    @Override
    public List<RoleInfoVO> getRoleInfoList() {
        List<RoleDO> roleDOS = roleMapper.selectActiveRoles();
        return RoleConverter.INSTANCE.toRoleInfoVOList(roleDOS);
    }

    @Override
    public RoleDetailVO getRoleDetailByCode(String code) {
        RoleDO roleDO = roleMapper.selectByCode(code);
        if (roleDO == null) {
            return null;
        }
        return convertToRoleDetailVO(roleDO);
    }

    @Override
    public RoleDetailVO getRoleDetailById(Long roleId) {
        RoleDO roleDO = roleMapper.selectById(roleId);
        if (roleDO == null) {
            return null;
        }
        roleDO.setAvatarUrl(minioUtils.getTemporaryUrlByPath(roleDO.getAvatarUrl()));

        return convertToRoleDetailVO(roleDO);
    }

    @Override
    public String getSystemPrompt(Long roleId) {
        RoleDO roleDO = roleMapper.selectById(roleId);
        if (roleDO == null) {
            return RoleConfigParser.extractSystemPrompt(null);
        }
        return RoleConfigParser.extractSystemPrompt(roleDO.getAiSetting());
    }

    /**
     * 将RoleDO转换为RoleDetailVO
     *
     * @param roleDO 角色实体
     * @return 角色详情VO
     */
    private RoleDetailVO convertToRoleDetailVO(RoleDO roleDO) {
        return RoleConverter.INSTANCE.toRoleDetailVO(roleDO);
    }
}
