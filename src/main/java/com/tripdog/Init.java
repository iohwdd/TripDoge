package com.tripdog;

import com.tripdog.common.Constants;
import com.tripdog.common.middleware.RedisClient;
import com.tripdog.common.utils.SystemSettingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class Init implements CommandLineRunner {
    private final SystemSettingUtils systemSettingUtils;
    private final RedisClient redisClient;

    @Override
    public void run(String... args) throws Exception {
        if (redisClient.hasKey(Constants.REDIS_CHAT_LIMIT_RULE)) {
            return;
        }
        String rpm = SystemSettingUtils.get(Constants.SYS_RPM);
        redisClient.hset(Constants.REDIS_CHAT_LIMIT_RULE, Constants.SYS_RPM, Integer.valueOf(rpm));
    }
}
