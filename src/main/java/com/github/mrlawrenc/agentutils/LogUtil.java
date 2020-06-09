package com.github.mrlawrenc.agentutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志封装类
 *
 * @author MrLawrenc
 * date  2020/5/27 23:59
 */
@SuppressWarnings("all")
public final class LogUtil {
    private LogUtil() {
    }

    private static Logger getLogger(String clzName) {
        return LoggerFactory.getLogger(clzName);
    }

    public static void info(String str, Object... messages) {
        getLogger(getClzName()).info(str, messages);
    }

    public static void info(String str) {
        getLogger(getClzName()).info(str);
    }

    public static void debug(String str, Object... messages) {
        Logger logger = getLogger(getClzName());
        if (logger.isDebugEnabled()) {
            logger.debug(str, messages);
        }
    }

    public static void debug(String str) {
        Logger logger = getLogger(getClzName());
        if (logger.isDebugEnabled()) {
            logger.debug(str);
        }
    }


    public static void error(String str, Object... messages) {
        Logger logger = getLogger(getClzName());
        if (logger.isErrorEnabled()) {
            logger.error(str, messages);
        }
    }

    public static void error(Throwable throwable, String str) {
        Logger logger = getLogger(getClzName());
        if (logger.isErrorEnabled()) {
            logger.error(str, throwable);
        }
    }

    public static void error(Throwable throwable, String str, String... messages) {
        Logger logger = getLogger(getClzName());
        if (logger.isErrorEnabled()) {
            for (String message : messages) {
                str = str.replace("{}", message);
            }
            logger.error(str, throwable);
        }
    }

    public static void warn(String str, Object... messages) {
        Logger logger = getLogger(getClzName());
        if (logger.isWarnEnabled()) {
            logger.warn(str, messages);
        }
    }

    public static void warn(String str) {
        Logger logger = getLogger(getClzName());
        if (logger.isDebugEnabled()) {
            logger.warn(str);
        }
    }

    /**
     * 正常调用是第四个栈是为当前调用类，而反射为第二个栈，如Hello被反射调用时的调用栈
     * <p>
     * 当前类是:java.lang.Thread
     * 当前类是:Hello
     * 当前类是:jdk.internal.reflect.NativeMethodAccessorImpl
     *
     * @return
     */
    private static String getClzName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return stackTrace.length < 3 ? "  非本地类   " : stackTrace[3].getClassName();
    }
}