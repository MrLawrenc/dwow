package com.github.mrlawrenc.attach.util;

/**
 * @author : MrLawrenc
 * date  2020/7/9 20:52
 * <p>
 * 雪花算法
 */
public class Snow {
    private static long lastTime;
    private static long sequence;

    public static long nextId() {
        long timeStamp = System.currentTimeMillis();

        if (lastTime==timeStamp){
            sequence=(sequence+1)&sequence;
        }

        return 1;
    }
}