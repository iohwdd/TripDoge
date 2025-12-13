package com.tripdog.cron;

import com.tripdog.service.IntimacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 亲密度定时扣减任务（连续3天未聊天，每天 -10，扣到0止）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntimacyCron {

    private final IntimacyService intimacyService;

    /**
     * 每天 00:05 执行。
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void runInactivityPenalty() {
        try {
            intimacyService.applyInactivityPenalty();
        } catch (Exception e) {
            log.error("亲密度定时扣减任务执行异常", e);
        }
    }
}

