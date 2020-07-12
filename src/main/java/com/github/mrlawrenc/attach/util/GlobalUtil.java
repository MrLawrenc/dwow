package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.Writer;
import com.github.mrlawrenc.attach.write.impl.FileWriter;

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

    public static String getId() {
        return CURRENT_THREAD_FLAG.incrementAndGet() + "#" + System.currentTimeMillis();
    }
}