package com.tripdog.service;

import com.tripdog.model.entity.IntimacyDO;
import com.tripdog.model.vo.IntimacyChange;

/**
 * 亲密度相关服务。
 */
public interface IntimacyService {

    /**
     * 获取当前亲密度（不存在则初始化为0）。
     */
    IntimacyDO getCurrent(Long uid, Long roleId);

    /**
     * 处理用户发送消息时的亲密度增长（含日首条、每10轮、封顶、日封顶）。
     */
    IntimacyChange handleUserMessage(Long uid, Long roleId);

    /**
     * 每日定时扣减逻辑（连续3天未聊每日-10，扣到0止）。
     */
    void applyInactivityPenalty();
}

