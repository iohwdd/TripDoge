package com.tripdog.ai.langgraph.travel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单的 POI / 路线结构，供 LangGraph 状态传递使用。
 */
public class TravelStateModels {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Poi implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String type;          // sight / food / hotel
        private String district;      // 行政区或商圈
        @JsonProperty("lat")
        private Double latitude;
        @JsonProperty("lng")
        private Double longitude;
        private Double rating;
        private List<String> tags;
        private String raw;           // 原始描述（方便回显）
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String poiName;
        private String type;      // sight / food
        private String timeSlot;  // morning/afternoon/evening
        private String transport; // 打车/地铁/步行
        private String tips;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteDay implements Serializable {
        private static final long serialVersionUID = 1L;
        private int dayIndex;
        private String theme;
        @Builder.Default
        private List<RouteItem> items = new ArrayList<>();
    }
}

