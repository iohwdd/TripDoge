package com.tripdog.service.impl;

import com.tripdog.common.Constants;
import com.tripdog.common.ErrorCode;
import com.tripdog.mapper.UserMapper;
import com.tripdog.model.converter.UserConverter;
import com.tripdog.model.dto.UserLoginDTO;
import com.tripdog.model.dto.UserRegisterDTO;
import com.tripdog.model.entity.UserDO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.EmailService;
import com.tripdog.service.UserService;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    @Value("${minio.bucket-name}")
    private String bucketName;

    private final MinioClient minioClient;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDO selectByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer count = userMapper.countByEmail(email);
        return count != null && count > 0;
    }

    @Override
    public boolean createUser(UserDO user) {
        // 设置默认状态为激活
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        int result = userMapper.insert(user);
        return result > 0;
    }

    @Override
    public UserDO selectById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public boolean updateUser(UserDO user) {
        int result = userMapper.updateById(user);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(UserRegisterDTO registerDTO) {
        // 1. 验证验证码
        if (!emailService.verifyCode(registerDTO.getEmail(), registerDTO.getCode())) {
            throw new RuntimeException(ErrorCode.EMAIL_CODE_ERROR.getMessage());
        }

        // 2. 检查邮箱是否已存在
        if (existsByEmail(registerDTO.getEmail())) {
            throw new RuntimeException(ErrorCode.USER_EMAIL_EXISTS.getMessage());
        }

        // 3. 转换为DO对象并加密密码
        UserDO userDO = UserConverter.INSTANCE.toUserDO(registerDTO);
        userDO.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        userDO.setAvatarUrl(Constants.DEFAULT_AVATAR);
        
        // 4. 创建用户
        boolean success = createUser(userDO);
        if (!success) {
            throw new RuntimeException(ErrorCode.USER_REGISTER_FAILED.getMessage());
        }

        // 5. 返回用户信息
        return UserConverter.INSTANCE.toUserInfoVO(userDO);
    }

    @Override
    public UserInfoVO login(UserLoginDTO loginDTO) {
        // 1. 根据邮箱查询用户
        UserDO userDO = selectByEmail(loginDTO.getEmail());
        if (userDO == null) {
            log.warn("LOGIN_FAIL reason=USER_NOT_FOUND email={}", loginDTO.getEmail());
            throw new RuntimeException(ErrorCode.USER_NOT_FOUND.getMessage());
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), userDO.getPassword())) {
            log.warn("LOGIN_FAIL reason=PASSWORD_ERROR email={}", loginDTO.getEmail());
            throw new RuntimeException(ErrorCode.USER_PASSWORD_ERROR.getMessage());
        }

        // 3. 检查用户状态
        if (userDO.getStatus() != 1) {
            log.warn("LOGIN_FAIL reason=USER_DISABLED email={}", loginDTO.getEmail());
            throw new RuntimeException(ErrorCode.USER_LOGIN_FAILED.getMessage());
        }

        // 转化头像url
        String avatarKey = userDO.getAvatarUrl();
        if (avatarKey == null || avatarKey.isEmpty()) {
            userDO.setAvatarUrl(Constants.DEFAULT_AVATAR);
        } else {
            try {
                String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(avatarKey)
                        .expiry(60 * 60)
                        .build()
                );
                userDO.setAvatarUrl(url);
            } catch (Exception e) {
                userDO.setAvatarUrl(Constants.DEFAULT_AVATAR);
            }
        }

        // 4. 返回用户信息
        return UserConverter.INSTANCE.toUserInfoVO(userDO);
    }

}
