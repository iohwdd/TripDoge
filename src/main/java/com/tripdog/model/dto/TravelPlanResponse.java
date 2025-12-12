package com.tripdog.model.dto;

import lombok.Data;

@Data
public class TravelPlanResponse {
    private Long historyId;
    private String mdPath;
    private String mdUrl;
    private String message;
}

