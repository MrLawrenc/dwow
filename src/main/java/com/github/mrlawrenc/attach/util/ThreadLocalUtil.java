package com.github.mrlawrenc.attach.util;

/**
 * @author hz20035009-逍遥
 * date   2020/7/7 16:50
 */
public class ThreadLocalUtil {

    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(String idx) {
        THREAD_LOCAL.set(idx);
    }

    public static String get() {
        return THREAD_LOCAL.get();
    }
}