package com.tripdog.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 角色配置解析工具
 * 用于解析角色的AI设置和角色设定JSON配置
 *
 * @author: iohw
 * @date: 2025/9/25
 */
@Slf4j
public class RoleConfigParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从AI设置JSON中提取系统提示词
     *
     * @param aiSettingJson AI设置的JSON字符串
     * @return 系统提示词，如果解析失败则返回默认提示词
     */
    public static String extractSystemPrompt(String aiSettingJson) {
        if (aiSettingJson == null || aiSettingJson.trim().isEmpty()) {
            return getDefaultSystemPrompt();
        }

        try {
            JsonNode aiSetting = objectMapper.readTree(aiSettingJson);
            JsonNode systemPromptNode = aiSetting.get("system_prompt");

            if (systemPromptNode != null && !systemPromptNode.isNull()) {
                return systemPromptNode.asText();
            }
        } catch (Exception e) {
            log.error("解析AI设置JSON失败: {}", aiSettingJson, e);
        }

        return getDefaultSystemPrompt();
    }

    /**
     * 从AI设置JSON中提取temperature参数
     *
     * @param aiSettingJson AI设置的JSON字符串
     * @return temperature值，如果解析失败则返回默认值0.7
     */
    public static double extractTemperature(String aiSettingJson) {
        if (aiSettingJson == null || aiSettingJson.trim().isEmpty()) {
            return 0.7;
        }

        try {
            JsonNode aiSetting = objectMapper.readTree(aiSettingJson);
            JsonNode temperatureNode = aiSetting.get("temperature");

            if (temperatureNode != null && temperatureNode.isNumber()) {
                return temperatureNode.asDouble();
            }
        } catch (Exception e) {
            log.error("解析temperature参数失败: {}", aiSettingJson, e);
        }

        return 0.7;
    }

    /**
     * 从AI设置JSON中提取max_tokens参数
     *
     * @param aiSettingJson AI设置的JSON字符串
     * @return max_tokens值，如果解析失败则返回默认值2048
     */
    public static int extractMaxTokens(String aiSettingJson) {
        if (aiSettingJson == null || aiSettingJson.trim().isEmpty()) {
            return 2048;
        }

        try {
            JsonNode aiSetting = objectMapper.readTree(aiSettingJson);
            JsonNode maxTokensNode = aiSetting.get("max_tokens");

            if (maxTokensNode != null && maxTokensNode.isNumber()) {
                return maxTokensNode.asInt();
            }
        } catch (Exception e) {
            log.error("解析max_tokens参数失败: {}", aiSettingJson, e);
        }

        return 2048;
    }

    /**
     * 从角色设定JSON中提取性格特征
     *
     * @param roleSettingJson 角色设定的JSON字符串
     * @return 性格特征数组，如果解析失败则返回空数组
     */
    public static String[] extractPersonality(String roleSettingJson) {
        if (roleSettingJson == null || roleSettingJson.trim().isEmpty()) {
            return new String[0];
        }

        try {
            JsonNode roleSetting = objectMapper.readTree(roleSettingJson);
            JsonNode personalityNode = roleSetting.get("personality");

            if (personalityNode != null && personalityNode.isArray()) {
                String[] personality = new String[personalityNode.size()];
                for (int i = 0; i < personalityNode.size(); i++) {
                    personality[i] = personalityNode.get(i).asText();
                }
                return personality;
            }
        } catch (Exception e) {
            log.error("解析personality参数失败: {}", roleSettingJson, e);
        }

        return new String[0];
    }

    /**
     * 从角色设定JSON中提取沟通风格
     *
     * @param roleSettingJson 角色设定的JSON字符串
     * @return 沟通风格描述
     */
    public static String extractCommunicationStyle(String roleSettingJson) {
        if (roleSettingJson == null || roleSettingJson.trim().isEmpty()) {
            return "友好自然";
        }

        try {
            JsonNode roleSetting = objectMapper.readTree(roleSettingJson);
            JsonNode styleNode = roleSetting.get("communication_style");

            if (styleNode != null && !styleNode.isNull()) {
                return styleNode.asText();
            }
        } catch (Exception e) {
            log.error("解析communication_style参数失败: {}", roleSettingJson, e);
        }

        return "友好自然";
    }

    /**
     * 从角色设定JSON中提取专长领域
     *
     * @param roleSettingJson 角色设定的JSON字符串
     * @return 专长领域数组
     */
    public static String[] extractSpecialties(String roleSettingJson) {
        if (roleSettingJson == null || roleSettingJson.trim().isEmpty()) {
            return new String[0];
        }

        try {
            JsonNode roleSetting = objectMapper.readTree(roleSettingJson);
            JsonNode specialtiesNode = roleSetting.get("specialties");

            if (specialtiesNode != null && specialtiesNode.isArray()) {
                String[] specialties = new String[specialtiesNode.size()];
                for (int i = 0; i < specialtiesNode.size(); i++) {
                    specialties[i] = specialtiesNode.get(i).asText();
                }
                return specialties;
            }
        } catch (Exception e) {
            log.error("解析specialties参数失败: {}", roleSettingJson, e);
        }

        return new String[0];
    }

    /**
     * 从角色设定JSON中提取表情符号
     *
     * @param roleSettingJson 角色设定的JSON字符串
     * @return 表情符号
     */
    public static String extractEmoji(String roleSettingJson) {
        if (roleSettingJson == null || roleSettingJson.trim().isEmpty()) {
            return "🤖";
        }

        try {
            JsonNode roleSetting = objectMapper.readTree(roleSettingJson);
            JsonNode emojiNode = roleSetting.get("emoji");

            if (emojiNode != null && !emojiNode.isNull()) {
                return emojiNode.asText();
            }
        } catch (Exception e) {
            log.error("解析emoji参数失败: {}", roleSettingJson, e);
        }

        return "🤖";
    }

    /**
     * 从角色设定JSON中提取口头禅
     *
     * @param roleSettingJson 角色设定的JSON字符串
     * @return 口头禅数组
     */
    public static String[] extractCatchphrases(String roleSettingJson) {
        if (roleSettingJson == null || roleSettingJson.trim().isEmpty()) {
            return new String[0];
        }

        try {
            JsonNode roleSetting = objectMapper.readTree(roleSettingJson);
            JsonNode catchphrasesNode = roleSetting.get("catchphrases");

            if (catchphrasesNode != null && catchphrasesNode.isArray()) {
                String[] catchphrases = new String[catchphrasesNode.size()];
                for (int i = 0; i < catchphrasesNode.size(); i++) {
                    catchphrases[i] = catchphrasesNode.get(i).asText();
                }
                return catchphrases;
            }
        } catch (Exception e) {
            log.error("解析catchphrases参数失败: {}", roleSettingJson, e);
        }

        return new String[0];
    }

    /**
     * 从AI设置JSON中提取top_p参数
     *
     * @param aiSettingJson AI设置的JSON字符串
     * @return top_p值，如果解析失败则返回默认值0.9
     */
    public static double extractTopP(String aiSettingJson) {
        if (aiSettingJson == null || aiSettingJson.trim().isEmpty()) {
            return 0.9;
        }

        try {
            JsonNode aiSetting = objectMapper.readTree(aiSettingJson);
            JsonNode topPNode = aiSetting.get("top_p");

            if (topPNode != null && topPNode.isNumber()) {
                return topPNode.asDouble();
            }
        } catch (Exception e) {
            log.error("解析top_p参数失败: {}", aiSettingJson, e);
        }

        return 0.9;
    }

    public static int extractSkillExecLimitMonth(String aiSettingJson) {
        if (aiSettingJson == null || aiSettingJson.trim().isEmpty()) {
            return 10;
        }

        try {
            JsonNode aiSetting = objectMapper.readTree(aiSettingJson);
            JsonNode node = aiSetting.get("skill_exec_limit_month");
            if (node != null && node.isNumber()) {
                return node.asInt();
            }
        } catch (Exception e) {
            log.error("解析max_tokens参数失败: {}", aiSettingJson, e);
        }

        return 10;
    }

    /**
     * 获取默认系统提示词
     *
     * @return 默认系统提示词
     */
    private static String getDefaultSystemPrompt() {
        return "你是一个友好的AI助手，乐于帮助用户解决问题。请用友好、专业的语调回答用户的问题。";
    }
}
