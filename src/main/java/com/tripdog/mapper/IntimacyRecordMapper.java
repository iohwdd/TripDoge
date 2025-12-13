package com.tripdog.mapper;

import com.tripdog.model.entity.IntimacyRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IntimacyRecordMapper {

    int insert(IntimacyRecordDO record);

    List<IntimacyRecordDO> listByUserAndRole(@Param("uid") Long uid,
                                             @Param("roleId") Long roleId,
                                             @Param("limit") Integer limit);
}

