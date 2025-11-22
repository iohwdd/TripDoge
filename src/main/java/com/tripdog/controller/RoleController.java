package com.tripdog.controller;

import com.tripdog.common.Constants;

import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import lombok.extern.slf4j.Slf4j;
import com.tripdog.mapper.ConversationMapper;
import com.tripdog.model.dto.RoleListQueryDTO;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.vo.RoleInfoVO;
import com.tripdog.model.vo.RoleDetailVO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.ConversationService;
import com.tripdog.service.RoleService;
import com.tripdog.service.impl.UserSessionService;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 */
@Tag(name = "角色管理", description = "AI角色列表和对话管理相关接口")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    @Value("${minio.bucket-name}")
    private String bucketName;
    private final MinioClient minioClient;
    private final RoleService roleService;
    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;
    private final UserSessionService userSessionService;


    /**
     * 获取用户与角色对话列表
     */
    @Operation(summary = "获取角色对话列表", description = "获取用户与所有AI角色的对话列表，包含角色信息和会话ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取角色列表"),
            @ApiResponse(responseCode = "10105", description = "用户未登录")
    })
    @PostMapping("/list")
    public Result<List<RoleInfoVO>> getActiveRoles(@RequestBody(required = false) RoleListQueryDTO queryDTO) {
        // 从用户会话服务获取当前登录用户信息
        UserInfoVO userInfo = userSessionService.getCurrentUser();
        if(userInfo == null) {
            return Result.error(ErrorCode.USER_NOT_LOGIN);
        }
        // 检查所有角色是否已创建好对话
        List<RoleInfoVO> roleInfoList = queryDTO == null ? roleService.getRoleInfoList() : roleService.getRoleInfoList(queryDTO);
        roleInfoList.forEach(roleInfoVO -> {
            ConversationDO conversation = conversationService.findConversationByUserAndRole(
                userInfo.getId(), roleInfoVO.getId());
            if(conversation == null) {
                conversation = conversationService.getOrCreateConversation(userInfo.getId(), roleInfoVO.getId());
            }
            roleInfoVO.setConversationId(conversation.getConversationId());
            // 转化头像url，如果 MinIO 不可用则回退到默认地址
            roleInfoVO.setAvatarUrl(resolveAvatarUrl(roleInfoVO.getAvatarUrl(), "ROLE_LIST", roleInfoVO.getId()));
        });

        return Result.success(roleInfoList);
    }

    /**
     * 获取角色详情
     *
     * @param roleId 角色ID
     * @return 角色详情信息
     */
    @Operation(summary = "获取角色详情", description = "根据角色ID获取指定AI角色的详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取角色详情"),
        @ApiResponse(responseCode = "10202", description = "角色不存在")
    })
    @PostMapping("/{roleId}/detail")
    public Result<RoleDetailVO> getRoleDetail(@PathVariable Long roleId) {
        RoleDetailVO roleDetail = roleService.getRoleDetailById(roleId);
        if (roleDetail == null) {
            return Result.error(ErrorCode.NOT_FOUND_ERROR, "角色不存在");
        }
        return Result.success(roleDetail);
    }

    /**
     * 获取公开角色列表（无需登录）
     * 用于未登录用户查看可用的AI角色
     */
    @Operation(summary = "获取公开角色列表", description = "获取所有可用的AI角色列表，无需登录即可访问")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取角色列表")
    })
    @GetMapping("/public/list")
    public Result<List<RoleInfoVO>> getPublicRoles() {
        // 获取角色列表（不包含会话信息）
        List<RoleInfoVO> roleInfoList = roleService.getRoleInfoList();
        // 处理头像URL（如果MinIO可用）
        roleInfoList.forEach(roleInfoVO -> roleInfoVO.setAvatarUrl(
            resolveAvatarUrl(roleInfoVO.getAvatarUrl(), "PUBLIC_ROLE_LIST", roleInfoVO.getId())));
        return Result.success(roleInfoList);
    }

    private String resolveAvatarUrl(String avatarKey, String scene, Long roleId) {
        if (avatarKey == null || avatarKey.isEmpty()) {
            return Constants.DEFAULT_AVATAR;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(avatarKey)
                    .expiry(60 * 60)
                    .build()
            );
        } catch (Exception e) {
            log.warn("生成角色头像URL失败，使用默认头像: scene={}, roleId={}, avatarKey={}, error={}",
                scene, roleId, avatarKey, e.getMessage());
            return Constants.DEFAULT_AVATAR;
        }
    }
}
