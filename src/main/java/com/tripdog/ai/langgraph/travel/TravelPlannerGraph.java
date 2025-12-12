package com.tripdog.ai.langgraph.travel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tripdog.ai.assistant.TravelPlaningAssistant;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdog.ai.AssistantService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 旅行规划工作流（LangGraph4j）。
 * 将用户需求 -> POI 检索 -> 筛选 -> 路线 -> Markdown 路书。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TravelPlannerGraph {

    private final AssistantService assistantService;
    public static final String SEARCH = "search";
    public static final String FILTER = "filter";
    public static final String ROUTE = "route";
    public static final String GENERATE = "generate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompiledGraph<TravelState> compile() throws GraphStateException {
        return new StateGraph<>(TravelState.SCHEMA, TravelState::new)
            .addNode("search", node_async(searchNode()))
            .addNode("filter", node_async(filterNode()))
            .addNode("route", node_async(routeNode()))
            .addNode("generate", node_async(generateNode()))
            .addEdge(START, "search")
            .addEdge("search", "filter")
            .addEdge("filter", "route")
            .addEdge("route", "generate")
            .addEdge("generate", END)
            .compile();
    }

    /**
     * Node A: 调用百度地图 MCP，产出候选 POI（景点 + 餐饮）。
     */
    private NodeAction<TravelState> searchNode() {
        return state -> {
            TravelPlaningAssistant travelAssistant = assistantService.getTravelPlaningAssistant();
            String userQuery = buildSearchPrompt(state);

            String result = travelAssistant.chat(userQuery);
            List<TravelStateModels.Poi> pois = parsePoisFromJson(result);
            return TravelState.withSearchResults(pois);
        };
    }

    /**
     * Node B: 简单筛选（留给 LLM 更深入筛选，可以在此处再调一轮模型）。
     */
    private NodeAction<TravelState> filterNode() {
        return state -> {
            List<TravelStateModels.Poi> results = state.searchResults();
            // 基于标签/评分的简单过滤示例；缺省直接透传。
            List<String> prefs = state.tags();
            double minRating = 3.8;
            List<TravelStateModels.Poi> filtered = results.stream()
                .filter(p -> p.getRating() == null || p.getRating() >= minRating)
                .filter(p -> prefs.isEmpty() || p.getTags() == null || p.getTags().isEmpty()
                        || p.getTags().stream().anyMatch(t -> prefs.contains(t)))
                .limit(20)
                .collect(Collectors.toList());
            return TravelState.withSelectedPois(filtered.isEmpty() ? results : filtered);
        };
    }

    /**
     * Node C: 按天分配路线（示例：每 3 个 POI 一天，可按经纬度改进聚类）。
     */
    private NodeAction<TravelState> routeNode() {
        return state -> {
            // 选用已筛选的 POI；如果为空，回退到原始检索结果
            List<TravelStateModels.Poi> pois = state.selectedPois();
            if (pois == null || pois.isEmpty()) {
                pois = state.searchResults();
            }

            int days = state.days() != null && state.days() > 0 ? state.days() : 3;
            days = Math.max(1, days);
            List<TravelStateModels.RouteDay> routes = new ArrayList<>();

            if (pois == null || pois.isEmpty()) {
                // 仍然没有 POI，生成兜底占位路线，避免后续生成节点拿到空数组
                for (int day = 0; day < days; day++) {
                    List<TravelStateModels.RouteItem> items = new ArrayList<>();
                    items.add(TravelStateModels.RouteItem.builder()
                        .poiName("待规划")
                        .type("sight")
                        .timeSlot("morning")
                        .transport("待确认")
                        .tips("请补充目的地/偏好/行程信息后重试")
                        .build());
                    routes.add(TravelStateModels.RouteDay.builder()
                        .dayIndex(day + 1)
                        .theme("Day " + (day + 1) + " · " + nvl(state.destination()))
                        .items(items)
                        .build());
                }
                return TravelState.withRoutes(routes);
            }

            int perDay = Math.max(1, (int) Math.ceil((double) pois.size() / days));
            for (int day = 0; day < days; day++) {
                int start = day * perDay;
                int end = Math.min(start + perDay, pois.size());
                if (start >= end) {
                    break;
                }
                List<TravelStateModels.RouteItem> items = new ArrayList<>();
                for (int idx = start; idx < end; idx++) {
                    TravelStateModels.Poi poi = pois.get(idx);
                    items.add(TravelStateModels.RouteItem.builder()
                        .poiName(poi.getName())
                        .type(poi.getType())
                        .timeSlot(timeSlot(idx - start))
                        .transport("打车/步行视距离")
                        .tips("预留休息时间，避免过度紧凑")
                        .build());
                }
                routes.add(TravelStateModels.RouteDay.builder()
                    .dayIndex(day + 1)
                    .theme("Day " + (day + 1) + " · " + nvl(state.destination()))
                    .items(items)
                    .build());
            }
            return TravelState.withRoutes(routes);
        };
    }

    /**
     * Node D: 生成 Markdown 路书，调用 LLM 润色。
     */
    private NodeAction<TravelState> generateNode() {
        return state -> {
            // 幂等保护：如果已有 markdown，直接返回，避免重复生成导致潜在循环
            if (StringUtils.hasText(state.markdown())) {
                return TravelState.withMarkdown(state.markdown());
            }
            TravelPlaningAssistant travelAssistant = assistantService.getTravelPlaningAssistant();
            String prompt = buildMarkdownPrompt(state);

            String result = travelAssistant.chat(prompt);
            return TravelState.withMarkdown(result);
        };
    }

    // ===== helpers =====

    private String buildSearchPrompt(TravelState state) {
        return """
            你是一名旅行数据规划员，使用内置高德/地图工具检索。
            请严格返回 JSON 数组，不要输出 Markdown，不要额外解释。
            每个元素字段: name,type(sight/food),district,lat,lng,rating,tags,raw。
            目的地: %s
            天数: %s
            人群: %s
            偏好: %s
            其他需求原文: %s
            需要含景点与餐饮候选，总计不超过 25 条。
            示例:
            [
              {"name":"XX景点","type":"sight","district":"XX区","lat":0,"lng":0,"rating":4.5,"tags":["亲子"],"raw":"原始描述"},
              {"name":"XX餐厅","type":"food","district":"XX区","lat":0,"lng":0,"rating":4.2,"tags":["海鲜"],"raw":"原始描述"}
            ]
            """.formatted(
            nvl(state.destination()),
            (state.days() == null || state.days() <= 0) ? "3" : state.days(),
            nvl(state.people()),
            state.tags().isEmpty() ? "无" : String.join(",", state.tags()),
            nvl(state.rawRequirement())
        );
    }

    private String buildMarkdownPrompt(TravelState state) {
        // 将路线与原始 POI 候选一并提供，避免空路线时丢失检索结果
        Map<String, Object> payload = Map.of(
            "destination", nvl(state.destination()),
            "days", state.days(),
            "people", nvl(state.people()),
            "tags", state.tags(),
            "routes", state.routes() == null ? List.of() : state.routes(),
            "poiCandidates", state.searchResults() == null ? List.of() : state.searchResults()
        );

        return """
            你是一名旅行定制师，请基于下列数据生成 Markdown 路书：
            - 禁止再调用任何外部工具/MCP/搜索，仅使用下方提供的数据
            - 如果 routes 为空，可用 poiCandidates 自行编排行程
            - 需要按天列出早/午/晚安排
            - 为每个 POI 提供一句玩法/理由
            - 提供交通建议和注意事项

            输入数据(JSON):
            %s

            输出仅 Markdown，不要额外解释。
            """.formatted(safeJson(payload));
    }

    private List<TravelStateModels.Poi> parsePoisFromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        // 1) 清理代码块标记
        String cleaned = json
            .replace("```json", "")
            .replace("```", "")
            .trim();

        // 2) 尝试在返回体中定位 "results":[ ... ]，避免工具调用日志干扰
        int resultsIdx = cleaned.indexOf("\"results\"");
        if (resultsIdx >= 0) {
            int firstBracket = cleaned.indexOf('[', resultsIdx);
            int lastBracket = cleaned.indexOf(']', firstBracket);
            if (firstBracket > 0 && lastBracket > firstBracket) {
                cleaned = cleaned.substring(firstBracket, lastBracket + 1);
            }
        } else {
            // 3) 回退：提取首个 [ ... ] 段落
            int l = cleaned.indexOf('[');
            int r = cleaned.lastIndexOf(']');
            if (l >= 0 && r > l) {
                cleaned = cleaned.substring(l, r + 1);
            } else if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                cleaned = "[" + cleaned + "]";
            }
        }

        try {
            // 优先按 JSON AST 方式提取，兼容 {"results":[...]} 或 {"data":{"results":[...]}} 等返回
            JsonNode root = objectMapper.readTree(cleaned);
            if (root.isObject()) {
                if (root.has("results")) {
                    root = root.get("results");
                } else if (root.has("data")) {
                    root = root.get("data");
                    if (root.has("results")) {
                        root = root.get("results");
                    }
                }
            }
            if (root.isArray()) {
                return objectMapper.convertValue(root, new TypeReference<List<TravelStateModels.Poi>>() {});
            }

            // 回退：直接按列表反序列化
            return objectMapper.readValue(cleaned, new TypeReference<List<TravelStateModels.Poi>>() {});
        } catch (Exception e) {
            log.warn("解析 POI JSON 失败，返回空列表。cleaned={} | err={}", cleaned, e.getMessage());
            return List.of();
        }
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String timeSlot(int idx) {
        return switch (idx) {
            case 0 -> "morning";
            case 1 -> "afternoon";
            default -> "evening";
        };
    }

    /**
     * 演示如何在外部运行图并获得最终 state。
     */
    public TravelState runOnce(Map<String, Object> input) throws GraphStateException {
        var graph = compile();
        TravelState finalState = null;
        for (var s : graph.stream(input)) {
            finalState = s.state();
        }
        return finalState;
    }
}

