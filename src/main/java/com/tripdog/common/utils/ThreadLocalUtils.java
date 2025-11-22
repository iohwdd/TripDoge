package com.tripdog.common.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: iohw
 * @date: 2025/9/26 14:36
 * @description:
 */
public class ThreadLocalUtils {
    private static final ThreadLocal<Map<String, Object>> threadLocal = ThreadLocal.withInitial(HashMap::new);

    public static Object get(final String key) {
        return threadLocal.get().get(key);
    }

    public static void set(final String key, final Object value) {
        threadLocal.get().put(key, value);
    }

    public static void remove(final String key) {
        threadLocal.get().remove(key);
    }

    public static void clear() {
        threadLocal.get().clear();
    }

}
