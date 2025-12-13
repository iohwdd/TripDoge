package com.tripdog.model.vo;

import com.tripdog.model.entity.IntimacyDO;
import lombok.Data;

@Data
public class IntimacyChange {
    private IntimacyDO intimacy;
    /**
     * 本次变更量，可正可负。
     */
    private Integer delta;
}

