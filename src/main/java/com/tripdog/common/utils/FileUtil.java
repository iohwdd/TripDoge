package com.tripdog.common.utils;

public class FileUtil {
    public static boolean isImage(String fileName) {
        return fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".png");
    }

    public static String getFileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    public static String getAttachmentPathPrefix(String conversationId, Long userId) {
        return "conversation/" + conversationId + "/attachment/" + userId;
    }
}
