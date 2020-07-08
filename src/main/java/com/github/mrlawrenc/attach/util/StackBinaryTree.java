package com.github.mrlawrenc.attach.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 堆栈二叉树
 */
public class StackBinaryTree {
    public static InheritableThreadLocal<StackBinaryTree> testLocal = new InheritableThreadLocal<>();
    /**
     * 线程标识，若处在线程池循环中，则会在插桩处更新该标识
     */
    private final AtomicInteger currentThreadFlag = new AtomicInteger(Integer.MIN_VALUE);

    public void addNode(StackTraceElement stackTraceElement) {
        System.out.println("stack:"+stackTraceElement);
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

    public void t() {
        String src = "{" +
                "com.github.mrlawrenc.attach.util.StackBinaryTree binaryTree = com.github.mrlawrenc.attach.util.StackBinaryTree.testLocal.get();\n" +
                "        if (Objects.nonNull(binaryTree)){\n" +
                "            //代表为子堆栈节点，需要加入\n" +
                "        }" +
                "" +
                "}";


    }
}