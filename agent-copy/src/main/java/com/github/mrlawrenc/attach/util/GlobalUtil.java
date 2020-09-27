package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.statistics.Statistics;
import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.Writer;
import com.github.mrlawrenc.attach.write.impl.FileWriter;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : MrLawrenc
 * date  2020/7/11 17:49
 */
public class GlobalUtil {
    public static final String TABS = "\t";
    public static final String EMPTY_STR = "";
    private static final Writer WRITER;

    static {
        WRITER = new FileWriter();
        Runtime.getRuntime().addShutdownHook(new Thread(WRITER::destroy));
    }

    /**
     * 输出数据
     *
     * @param writeable w
     */
    public static void write(Writeable writeable) {
        WRITER.write(writeable);
    }

    /**
     * 线程唯一标识，若处在线程池循环中，则会在插桩处更新该标识，该标识会传递给子线程
     */
    private final static AtomicInteger CURRENT_THREAD_FLAG = new AtomicInteger(Integer.MIN_VALUE);

    /**
     * 获取全局唯一id
     *
     * @return id
     */
    public static String getId() {
        return CURRENT_THREAD_FLAG.incrementAndGet() + "#" + System.currentTimeMillis();
    }


    /**
     * 创建统计类
     */
    public static <T extends Statistics> T createStatistics(Class<T> statisticsClass) {
        T statistics;
        try {
            Constructor<T> constructor = statisticsClass.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            if (Objects.isNull(ThreadLocalUtil.globalThreadLocal.get())) {
                statistics = constructor.newInstance(GlobalUtil.getId());
            } else {
                statistics = constructor.newInstance(ThreadLocalUtil.globalThreadLocal.get().getId());
            }
        } catch (Exception e) {
            throw new RuntimeException("init statistics " + statisticsClass + " fail!");
        }
        Collector.addStatistics(statistics);
        return statistics;
    }
}