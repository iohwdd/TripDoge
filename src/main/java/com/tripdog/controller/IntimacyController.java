package com.tripdog.controller;

import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import com.tripdog.model.entity.IntimacyDO;
import com.tripdog.model.vo.IntimacyVO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.IntimacyService;
import com.tripdog.service.impl.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "亲密度", description = "用户与角色的亲密度接口")
@RestController
@RequestMapping("/intimacy")
@RequiredArgsConstructor
public class IntimacyController {

    private final IntimacyService intimacyService;
    private final UserSessionService userSessionService;

    @Operation(summary = "查询亲密度", description = "获取当前用户与指定角色的亲密度")
    @GetMapping("/{roleId}")
    public Result<IntimacyVO> getIntimacy(@PathVariable Long roleId) {
        UserInfoVO user = userSessionService.getCurrentUser();
        if (user == null) {
            return Result.error(ErrorCode.USER_NOT_LOGIN);
        }
        IntimacyDO data = intimacyService.getCurrent(user.getId(), roleId);
        IntimacyVO vo = new IntimacyVO();
        vo.setRoleId(roleId);
        vo.setIntimacy(data.getIntimacy());
        vo.setLastMsgTime(data.getLastMsgTime());
        vo.setLastDailyBonusDate(data.getLastDailyBonusDate());
        return Result.success(vo);
    }
}

