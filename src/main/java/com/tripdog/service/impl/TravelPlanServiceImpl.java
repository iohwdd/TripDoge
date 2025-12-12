package com.tripdog.service.impl;

import com.tripdog.ai.langgraph.travel.TravelPlannerGraph;
import com.tripdog.ai.langgraph.travel.TravelState;
import com.tripdog.exception.TravelPlannerGraphException;
import com.tripdog.common.utils.MinioUtils;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.dto.TravelPlanRequest;
import com.tripdog.model.dto.TravelPlanResponse;
import com.tripdog.model.entity.SkillHistory;
import com.tripdog.service.SkillHistoryService;
import com.tripdog.service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.tripdog.common.Constants.USER_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TravelPlanServiceImpl implements TravelPlanService {

    private record TravelLock(String runId, long ts) {}
    private static final ConcurrentHashMap<String, TravelLock> LOCKS = new ConcurrentHashMap<>();
    private static final long LOCK_TTL_MS = 2 * 60 * 1000;

    private final TravelPlannerGraph travelPlannerGraph;
    private final SkillHistoryService skillHistoryService;
    private final MinioUtils minioUtils;

    @Override
    public TravelPlanResponse runTravelPlan(Long roleId, TravelPlanRequest request) {
        ThreadLocalUtils.set("roleId", roleId);
        Long userId = (Long) ThreadLocalUtils.get(USER_ID);
        String lockKey = buildLockKey(userId, roleId, request);
        String runId = UUID.randomUUID().toString();

        if (userId != null && isLocked(lockKey, runId)) {
            return buildResponse(null, null, "已有旅行规划任务进行中，请稍后再试。");
        }
        if (userId != null) {
            LOCKS.put(lockKey, new TravelLock(runId, System.currentTimeMillis()));
        }

        try {
            CompiledGraph<TravelState> graph = travelPlannerGraph.compile();
            var input = buildInput(request);
            input.put(TravelState.DESTINATION, nvl(request.getDestination()));
            input.put(TravelState.DAYS, request.getDays() == null ? 3 : request.getDays());
            input.put(TravelState.PEOPLE, nvl(request.getPeople()));
            input.put(TravelState.TAGS, request.getPreferences() == null ? java.util.List.of() : request.getPreferences());
            input.put(TravelState.RAW_REQUIREMENT, nvl(request.getRawRequirement()));
            input.put("roleId", roleId);
            Optional<TravelState> stateOpt = graph.invoke(input);
            Optional<String> mdOpt = stateOpt.map(TravelState::markdown).filter(StringUtils::hasText);

            if (mdOpt.isEmpty()) {
                return buildResponse(null, null, "规划未生成 Markdown，请重试或补充信息。");
            }

            SkillHistory history = buildHistory(request, stateOpt.get(), roleId, userId);
            MdFile mdFile = generateAndUploadMarkdown(history.getUserId(), mdOpt.get());
            history.setMdPath(mdFile.path);
            history.setMdUrl(mdFile.url);
            skillHistoryService.save(history);

            TravelPlanResponse resp = new TravelPlanResponse();
            resp.setHistoryId(history.getId());
            resp.setMdPath(history.getMdPath());
            resp.setMdUrl(history.getMdUrl());
            resp.setMessage("行程规划已生成，请前往历史记录下载。");
            return resp;
        } catch (GraphStateException e) {
            log.error("Travel plan graph execution failed", e);
            return buildResponse(null, null, "规划执行失败，请稍后重试。");
        } catch (Exception e) {
            log.error("Travel plan failed", e);
            return buildResponse(null, null, "规划生成异常，请稍后重试。");
        } finally {
            if (userId != null) {
                TravelLock last = LOCKS.get(lockKey);
                if (last != null && runId.equals(last.runId)) {
                    LOCKS.remove(lockKey);
                }
            }
        }
    }

    @Override
    public TravelPlanResponse runTravelPlanStream(Long roleId, TravelPlanRequest request,
                                                  java.util.function.BiConsumer<String, TravelState> onNodeFinish) throws TravelPlannerGraphException {
        ThreadLocalUtils.set("roleId", roleId);
        Long userId = (Long) ThreadLocalUtils.get(USER_ID);
        String lockKey = buildLockKey(userId, roleId, request);
        String runId = UUID.randomUUID().toString();

        if (userId != null && isLocked(lockKey, runId)) {
            return buildResponse(null, null, "已有旅行规划任务进行中，请稍后再试。");
        }
        if (userId != null) {
            LOCKS.put(lockKey, new TravelLock(runId, System.currentTimeMillis()));
        }

        try {
            CompiledGraph<TravelState> graph = travelPlannerGraph.compile();
            var input = buildInput(request);
            input.put(TravelState.DESTINATION, nvl(request.getDestination()));
            input.put(TravelState.DAYS, request.getDays() == null ? 3 : request.getDays());
            input.put(TravelState.PEOPLE, nvl(request.getPeople()));
            input.put(TravelState.TAGS, request.getPreferences() == null ? java.util.List.of() : request.getPreferences());
            input.put(TravelState.RAW_REQUIREMENT, nvl(request.getRawRequirement()));
            input.put("roleId", roleId);

            TravelState last = null;
            for (var output : graph.stream(input)) {
                String node = output.node();
                TravelState state = output.state();
                last = state;
                if (!"__START__".equals(node) && !"__END__".equals(node) && onNodeFinish != null) {
                    onNodeFinish.accept(node, state);
                }
            }

            Optional<TravelState> stateOpt = Optional.ofNullable(last);
            Optional<String> mdOpt = stateOpt.map(TravelState::markdown).filter(StringUtils::hasText);

            if (mdOpt.isEmpty()) {
                return buildResponse(null, null, "规划未生成 Markdown，请重试或补充信息。");
            }

            SkillHistory history = buildHistory(request, stateOpt.get(), roleId, userId);
            MdFile mdFile = generateAndUploadMarkdown(history.getUserId(), mdOpt.get());
            history.setMdPath(mdFile.path);
            history.setMdUrl(mdFile.url);
            skillHistoryService.save(history);

            TravelPlanResponse resp = new TravelPlanResponse();
            resp.setHistoryId(history.getId());
            resp.setMdPath(history.getMdPath());
            resp.setMdUrl(history.getMdUrl());
            resp.setMessage("行程规划已生成，请前往历史记录下载。");
            return resp;
        } catch (Exception e) {
            throw new TravelPlannerGraphException("stream travel plan failed", e);
        } finally {
            if (userId != null) {
                TravelLock last = LOCKS.get(lockKey);
                if (last != null && runId.equals(last.runId)) {
                    LOCKS.remove(lockKey);
                }
            }
        }
    }

    private boolean isLocked(String lockKey, String runId) {
        long now = System.currentTimeMillis();
        TravelLock last = LOCKS.get(lockKey);
        boolean valid = last != null && (now - last.ts) < LOCK_TTL_MS;
        return valid && !runId.equals(last.runId);
    }

    private String buildLockKey(Long userId, Long roleId, TravelPlanRequest req) {
        String hash = Integer.toHexString(Objects.hash(
            userId,
            roleId,
            req.getDestination(),
            req.getDays(),
            req.getPeople(),
            req.getBudget(),
            req.getPreferences()
        ));
        return (userId == null ? "anonymous" : userId) + ":" + (roleId == null ? "0" : roleId) + ":" + hash;
    }

    private java.util.Map<String, Object> buildInput(TravelPlanRequest req) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put(TravelState.DESTINATION, nvl(req.getDestination()));
        map.put(TravelState.DAYS, req.getDays() == null ? 3 : req.getDays());
        map.put(TravelState.PEOPLE, nvl(req.getPeople()));
        map.put(TravelState.TAGS, req.getPreferences() == null ? java.util.List.of() : req.getPreferences());
        map.put(TravelState.RAW_REQUIREMENT, nvl(req.getRawRequirement()));
        return map;
    }

    private SkillHistory buildHistory(TravelPlanRequest req, TravelState state, Long roleId, Long userId) {
        SkillHistory h = new SkillHistory();
        h.setUserId(userId);
        h.setRoleId(roleId);
        h.setSkill("travel");
        h.setDestination(state.destination());
        h.setDays(state.days());
        h.setPeople(state.people());
        h.setBudget(req.getBudget());
        h.setPreferences(req.getPreferences());
        return h;
    }

    private MdFile generateAndUploadMarkdown(Long userId, String markdown) throws Exception {
        byte[] bytes = markdown == null ? new byte[0] : markdown.getBytes(StandardCharsets.UTF_8);
        String objectKey = "workflow/traval/" + (userId == null ? "anonymous" : userId) + "/" + UUID.randomUUID() + ".md";
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes)) {
            minioUtils.putObject(objectKey, in, bytes.length, "text/markdown; charset=utf-8");
        }
        String url = minioUtils.getTemporaryUrlByPath(objectKey);
        return new MdFile(objectKey, url);
    }

    private TravelPlanResponse buildResponse(Long historyId, String mdUrl, String msg) {
        TravelPlanResponse resp = new TravelPlanResponse();
        resp.setHistoryId(historyId);
        resp.setMdUrl(mdUrl);
        resp.setMessage(msg);
        return resp;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private record MdFile(String path, String url) {}
}

