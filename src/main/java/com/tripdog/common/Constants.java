package com.tripdog.common;

/**
 * @author: iohw
 * @date: 2025/9/24 21:49
 * @description:
 */
public class Constants {
    // redis key 前缀
    public static final String REDIS_KEY_PREFIX = "tripdoge:";
    public static final String REDIS_SUMMARY = REDIS_KEY_PREFIX + "chat:summary_threshold_count:";
    public static final String REDIS_INTIMACY = REDIS_KEY_PREFIX + "intimacy:";

    public static final String USER_SESSION_KEY = "loginUser";
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String SYSTEM = "system";
    public static final String TOOL = "tool";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String SUMMARY_TAG = "summary_tag";
    public static final String ROLE_ID = "role_id";
    public static final String USER_ID = "user_id";
    public static final String FILE_ID = "file_id";
    public static final String FILE_NAME = "origin_file_name";
    public static final String UPLOAD_TIME = "upload_time";
    public static final String INJECT_TEMPLATE = "\n文档/文件/附件的内容如下，你可以基于下面的内容回答:\n";
    public static final String DEFAULT_AVATAR = "avatar/default_avatar.jpg";
}
