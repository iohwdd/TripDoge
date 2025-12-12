package com.tripdog.mapper;

import com.tripdog.model.entity.TravelPlanerHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TravelPlanerHistoryMapper {

    int insert(TravelPlanerHistory record);

    TravelPlanerHistory selectById(@Param("id") Long id);

    TravelPlanerHistory selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    List<TravelPlanerHistory> listByUser(@Param("userId") Long userId);

    List<TravelPlanerHistory> listByUserAndRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}

