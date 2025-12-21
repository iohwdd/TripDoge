package com.tripdog.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.tripdog.common.Constants;
import com.tripdog.common.middleware.RedisClient;
import com.tripdog.mapper.IntimacyMapper;
import com.tripdog.mapper.IntimacyRecordMapper;
import com.tripdog.model.entity.IntimacyDO;
import com.tripdog.model.entity.IntimacyRecordDO;
import com.tripdog.service.IntimacyService;
import com.tripdog.model.vo.IntimacyChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 亲密度服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntimacyServiceImpl implements IntimacyService {
    private static final int INTIMACY_MAX = 100;
    private static final int INTIMACY_MIN = 0;
    private static final int DAILY_FIRST_BONUS = 5;
    private static final int TEN_ROUND_BONUS = 10;
    private static final int DAILY_TEN_ROUND_LIMIT = 5; // 每日"每10轮"触发上限（+50）
    private static final int INACTIVITY_PENALTY = 10;
    private static final int INACTIVITY_DAYS_THRESHOLD = 3;

    private final IntimacyMapper intimacyMapper;
    private final IntimacyRecordMapper intimacyRecordMapper;
    private final RedisClient redisClient;

    @Override
    public IntimacyDO getCurrent(Long uid, Long roleId) {
        IntimacyDO cache = getFromCache(uid, roleId);
        if (cache != null) {
            return cache;
        }
        IntimacyDO record = intimacyMapper.selectByUserAndRole(uid, roleId);
        if (record == null) {
            record = initIntimacy(uid, roleId);
        }
        cacheToRedis(record);
        return record;
    }

    @Override
    @Transactional
    public IntimacyChange handleUserMessage(Long uid, Long roleId) {
        // 亲密度100封顶
        Integer intimacyNow = (Integer) redisClient.get(keyIntimacy(uid, roleId));
        if (intimacyNow != null && intimacyNow >= INTIMACY_MAX) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        IntimacyDO current = getCurrent(uid, roleId);
        int intimacy = defaultZero(current.getIntimacy());
        int deltaSum = 0;

        // 日首条加分（防重复）
        boolean dailyAdded = tryDailyBonus(uid, roleId, today);
        if (dailyAdded) {
            intimacy = clamp(intimacy + DAILY_FIRST_BONUS);
            persistRecord(uid, roleId, DAILY_FIRST_BONUS, intimacy, "daily_first");
            current.setLastDailyBonusDate(today);
            deltaSum += DAILY_FIRST_BONUS;
        }

        // 每10轮加分（每日最多5次）
        boolean tenRoundAdded = tryTenRoundBonus(uid, roleId, today);
        if (tenRoundAdded) {
            intimacy = clamp(intimacy + TEN_ROUND_BONUS);
            persistRecord(uid, roleId, TEN_ROUND_BONUS, intimacy, "every_10");
            deltaSum += TEN_ROUND_BONUS;
        }

        // 更新主表与缓存
        current.setIntimacy(intimacy);
        current.setLastMsgTime(now);
        updateMain(uid, roleId, intimacy, now, current.getLastDailyBonusDate());
        cacheToRedis(current);

        // 记录最后一次用户发言时间
        redisClient.set(keyLastMsg(uid, roleId), String.valueOf(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        IntimacyChange change = new IntimacyChange();
        change.setIntimacy(current);
        change.setDelta(deltaSum);
        return change;
    }

    @Override
    @Transactional
    public void applyInactivityPenalty() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(INACTIVITY_DAYS_THRESHOLD);
        List<IntimacyDO> list = intimacyMapper.listInactiveSince(threshold);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (IntimacyDO item : list) {
            int before = defaultZero(item.getIntimacy());
            if (before <= INTIMACY_MIN) {
                continue;
            }
            int after = clamp(before - INACTIVITY_PENALTY);
            // 未活跃用户继续保留 lastMsgTime 原值
            updateMain(item.getUid(), item.getRoleId(), after, item.getLastMsgTime(), item.getLastDailyBonusDate());
            persistRecord(item.getUid(), item.getRoleId(), after - before, after, "inactivity_penalty");
            item.setIntimacy(after);
            cacheToRedis(item);
        }
    }

    // === 内部方法 ===

    private boolean tryDailyBonus(Long uid, Long roleId, LocalDate today) {
        String key = keyDailyFlag(uid, roleId);
        Boolean exists = redisClient.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return false;
        }
        // 未存在，则设置标记并加分
        redisClient.set(key, "1", ttlToNextMidnightPlusBufferMinutes(60), TimeUnit.MINUTES);
        return true;
    }

    private boolean tryTenRoundBonus(Long uid, Long roleId, LocalDate today) {
        Long cntVal = redisClient.incrBy(keyRoundCount(uid, roleId), 1);
        long cnt = cntVal == null ? 0L : cntVal;
        if (cnt % 10 != 0) {
            return false;
        }
        String dayKey = keyDailyTenRound(uid, roleId, today);
        Object current = redisClient.get(dayKey);
        int used = 0;
        if (current != null) {
            try {
                used = Integer.parseInt(current.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        if (used >= DAILY_TEN_ROUND_LIMIT) {
            return false;
        }
        // 增加一次计数并设置过期
        redisClient.set(dayKey, String.valueOf(used + 1), ttlToNextMidnightPlusBufferMinutes(60), java.util.concurrent.TimeUnit.MINUTES);
        return true;
    }

    private void updateMain(Long uid, Long roleId, Integer intimacy, LocalDateTime lastMsgTime, LocalDate lastDailyBonusDate) {
        int rows = intimacyMapper.updateIntimacy(uid, roleId, intimacy, lastMsgTime, lastDailyBonusDate);
        if (rows == 0) {
            // 兜底：若不存在则插入
            IntimacyDO init = new IntimacyDO();
            init.setUid(uid);
            init.setRoleId(roleId);
            init.setIntimacy(intimacy);
            init.setLastMsgTime(lastMsgTime);
            init.setLastDailyBonusDate(lastDailyBonusDate);
            intimacyMapper.insert(init);
        }
    }

    private void persistRecord(Long uid, Long roleId, int delta, int intimacy, String reason) {
        if (delta == 0) {
            return;
        }
        IntimacyRecordDO record = new IntimacyRecordDO();
        record.setUid(uid);
        record.setRoleId(roleId);
        record.setDelta(delta);
        record.setIntimacy(intimacy);
        record.setReason(reason);
        intimacyRecordMapper.insert(record);
    }

    private IntimacyDO initIntimacy(Long uid, Long roleId) {
        IntimacyDO init = new IntimacyDO();
        init.setUid(uid);
        init.setRoleId(roleId);
        init.setIntimacy(0);
        init.setLastMsgTime(null);
        init.setLastDailyBonusDate(null);
        intimacyMapper.insert(init);
        return init;
    }

    private IntimacyDO getFromCache(Long uid, Long roleId) {
        String key = keyIntimacy(uid, roleId);
        Object val = redisClient.get(key);
        if (val instanceof Integer) {
            IntimacyDO data = new IntimacyDO();
            data.setUid(uid);
            data.setRoleId(roleId);
            data.setIntimacy((Integer) val);
            // 其他字段无法从缓存获取，返回仅携带亲密度的对象
            return data;
        }
        if (val instanceof Long) {
            IntimacyDO data = new IntimacyDO();
            data.setUid(uid);
            data.setRoleId(roleId);
            data.setIntimacy(((Long) val).intValue());
            return data;
        }
        return null;
    }

    private void cacheToRedis(IntimacyDO data) {
        if (data == null || data.getUid() == null || data.getRoleId() == null || data.getIntimacy() == null) {
            return;
        }
        redisClient.set(keyIntimacy(data.getUid(), data.getRoleId()), data.getIntimacy());
    }

    private int clamp(int val) {
        if (val < INTIMACY_MIN) return INTIMACY_MIN;
        if (val > INTIMACY_MAX) return INTIMACY_MAX;
        return val;
    }

    private int defaultZero(Integer val) {
        return Objects.requireNonNullElse(val, 0);
    }

    private long ttlToNextMidnightPlusBufferMinutes(int bufferMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = LocalDateTime.of(now.plusDays(1).toLocalDate(), LocalTime.MIDNIGHT).plusMinutes(bufferMinutes);
        return Duration.between(now, nextMidnight).toMinutes();
    }

    // === key helpers ===
    private String keyIntimacy(Long uid, Long roleId) {
        return Constants.REDIS_INTIMACY + uid + ":" + roleId;
    }

    private String keyDailyFlag(Long uid, Long roleId) {
        return Constants.REDIS_INTIMACY + "daily:flag:" + uid + ":" + roleId;
    }

    private String keyRoundCount(Long uid, Long roleId) {
        return Constants.REDIS_INTIMACY + "cnt:" + uid + ":" + roleId;
    }

    private String keyDailyTenRound(Long uid, Long roleId, LocalDate date) {
        return Constants.REDIS_INTIMACY + "daily10:cnt:" + uid + ":" + roleId + ":" + date.toString().replace("-", "");
    }

    private String keyLastMsg(Long uid, Long roleId) {
        return Constants.REDIS_INTIMACY + "lastmsg:" + uid + ":" + roleId;
    }
}


