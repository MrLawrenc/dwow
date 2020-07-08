package com.github.mrlawrenc.attach.util;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author MrLawrenc
 * date  2020/7/8 22:49
 * 堆栈二叉树
 */
@Data
public class StackBinaryTree {
    /**
     * 线程标识，若处在线程池循环中，则会在插桩处更新该标识
     */
    private final AtomicInteger currentThreadFlag = new AtomicInteger(Integer.MIN_VALUE);

    public void addNode(StackTraceElement stackTraceElement) {
        System.out.println("收到堆栈信息:" + Thread.currentThread() + " stack : " + stackTraceElement);
    }

    public StackBinaryTree() {
        currentThreadFlag.set(0);
    }

    private static class Node {
        private String className;
        private String methodName;
        private long lineNum;

        private Node up;
        //private Node left;
        private Node down;
        //private Node right;
    }

}