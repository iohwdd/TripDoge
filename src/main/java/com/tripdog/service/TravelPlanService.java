package com.tripdog.service;

import com.tripdog.ai.langgraph.travel.TravelState;
import com.tripdog.exception.TravelPlannerGraphException;
import com.tripdog.model.dto.TravelPlanRequest;
import com.tripdog.model.dto.TravelPlanResponse;

public interface TravelPlanService {

    TravelPlanResponse runTravelPlan(Long roleId, TravelPlanRequest request);

    /**
     * 流式执行，按节点回调进度。
     */
    TravelPlanResponse runTravelPlanStream(Long roleId, TravelPlanRequest request,
                                           java.util.function.BiConsumer<String, TravelState> onNodeFinish)
        throws TravelPlannerGraphException;

}

