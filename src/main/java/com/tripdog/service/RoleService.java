package com.tripdog.service;

import java.util.List;

import com.tripdog.model.dto.RoleListQueryDTO;
import com.tripdog.model.vo.RoleInfoVO;
import com.tripdog.model.vo.RoleDetailVO;

public interface RoleService {

    List<RoleInfoVO> getRoleInfoList();

    List<RoleInfoVO> getRoleInfoList(RoleListQueryDTO queryDTO);

    RoleDetailVO getRoleDetailByCode(String code);

    RoleDetailVO getRoleDetailById(Long roleId);

    String getSystemPrompt(Long roleId);
}
