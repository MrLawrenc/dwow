package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.stack.StackNode;

/**
 * @author hz20035009-逍遥
 * date   2020/7/7 16:50
 */
public class ThreadLocalUtil {
    public static InheritableThreadLocal<StackNode> globalThreadLocal = new InheritableThreadLocal<>();




    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

}