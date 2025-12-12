package com.tripdog.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TravelPlanRequest {
    private String destination;
    private Integer days;
    private String people;
    private String budget;
    private List<String> preferences;
    private String rawRequirement;
}

