package com.tripdog.mapper;

import com.tripdog.model.entity.IntimacyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface IntimacyMapper {

    IntimacyDO selectByUserAndRole(@Param("uid") Long uid, @Param("roleId") Long roleId);

    int insert(IntimacyDO record);

    int updateIntimacy(@Param("uid") Long uid,
                       @Param("roleId") Long roleId,
                       @Param("intimacy") Integer intimacy,
                       @Param("lastMsgTime") LocalDateTime lastMsgTime,
                       @Param("lastDailyBonusDate") LocalDate lastDailyBonusDate);

    List<IntimacyDO> listInactiveSince(@Param("threshold") LocalDateTime threshold);
}

