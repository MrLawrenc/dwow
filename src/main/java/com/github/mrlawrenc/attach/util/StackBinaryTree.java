package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.write.Writeable;
import com.github.mrlawrenc.attach.write.WriterResp;
import com.github.mrlawrenc.attach.write.impl.FileWriter;
import lombok.Data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author MrLawrenc
 * date  2020/7/8 22:49
 * 堆栈二叉树
 */
@Data
public class StackBinaryTree extends FileWriter implements Writeable {
    /**
     * 线程标识，若处在线程池循环中，则会在插桩处更新该标识
     */
    private final AtomicInteger currentThreadFlag = new AtomicInteger(Integer.MIN_VALUE);

    private StackTraceElement[] stackTraceElements;

    public void addNode(StackTraceElement[] stackTraceElement) {
        System.out.println("====>" + Arrays.toString(stackTraceElement));
        this.stackTraceElements = stackTraceElement;
        new FileWriter().write(this);
    }

    public StackBinaryTree() {
        currentThreadFlag.set(0);
    }

    public StackBinaryTree(int newValue, StackBinaryTree oldTree) {

        currentThreadFlag.set(newValue);
        for (StackTraceElement stackTraceElement : oldTree.getStackTraceElements()) {
            System.out.println(stackTraceElement.getLineNumber() + "===》" + stackTraceElement.getClassName() + "#" + stackTraceElement.getMethodName() + "  " + stackTraceElement.getFileName());
        }
    }

    private RandomAccessFile rw = null;
    private int off;

    @Override
    public WriterResp write(Writeable writeable) {
        if (Objects.isNull(rw)){
            try {
                rw = new RandomAccessFile("/stack.txt", "rw");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        byte[] bytes = "".getBytes();
        try {
            rw.write(bytes,off,bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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