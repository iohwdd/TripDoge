package com.tripdog.ai.langgraph.travel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

/**
 * 旅行规划状态，在 LangGraph4j 各节点间传递。
 */
public class TravelState extends AgentState {

    public static final String DESTINATION = "destination";
    public static final String DAYS = "days";
    public static final String PEOPLE = "people";
    public static final String TAGS = "tags";
    public static final String SEARCH_RESULTS = "searchResults";
    public static final String SELECTED_POIS = "selectedPois";
    public static final String ROUTES = "routes";
    public static final String MARKDOWN = "markdownReport";
    public static final String RAW_REQUIREMENT = "rawRequirement";

    @SuppressWarnings("rawtypes")
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
        DESTINATION, Channels.base(() -> ""),
        DAYS, Channels.base(() -> 0),
        PEOPLE, Channels.base(() -> ""),
        TAGS, Channels.base(ArrayList::new),
        SEARCH_RESULTS, Channels.base(ArrayList::new),
        SELECTED_POIS, Channels.base(ArrayList::new),
        ROUTES, Channels.base(ArrayList::new),
        MARKDOWN, Channels.base(() -> ""),
        RAW_REQUIREMENT, Channels.base(() -> "")
    );

    public TravelState(Map<String, Object> initData) {
        super(initData);
    }

    public String destination() {
        return (String) value(DESTINATION).orElse(null);
    }

    public Integer days() {
        return (Integer) value(DAYS).orElse(null);
    }

    public String people() {
        return (String) value(PEOPLE).orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<String> tags() {
        return (List<String>) value(TAGS).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<TravelStateModels.Poi> searchResults() {
        return (List<TravelStateModels.Poi>) value(SEARCH_RESULTS).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<TravelStateModels.Poi> selectedPois() {
        return (List<TravelStateModels.Poi>) value(SELECTED_POIS).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<TravelStateModels.RouteDay> routes() {
        return (List<TravelStateModels.RouteDay>) value(ROUTES).orElse(List.of());
    }

    public String markdown() {
        return (String) value(MARKDOWN).orElse(null);
    }

    public String rawRequirement() {
        return (String) value(RAW_REQUIREMENT).orElse(null);
    }

    public static Map<String, Object> withSearchResults(List<TravelStateModels.Poi> pois) {
        return Map.of(SEARCH_RESULTS, new ArrayList<>(pois));
    }

    public static Map<String, Object> withSelectedPois(List<TravelStateModels.Poi> pois) {
        return Map.of(SELECTED_POIS, new ArrayList<>(pois));
    }

    public static Map<String, Object> withRoutes(List<TravelStateModels.RouteDay> routes) {
        return Map.of(ROUTES, new ArrayList<>(routes));
    }

    public static Map<String, Object> withMarkdown(String md) {
        return Map.of(MARKDOWN, md);
    }
}

