package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.write.Writeable;
import lombok.Data;

import java.io.RandomAccessFile;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author MrLawrenc
 * date  2020/7/8 22:49
 * 堆栈二叉树
 */
@Data
public class StackNode implements Writeable {
    /**
     * 线程标识，若处在线程池循环中，则会在插桩处更新该标识
     */
    private final AtomicInteger currentThreadFlag = new AtomicInteger(Integer.MIN_VALUE);

    private StackTraceElement[] currentStackElements;

    /**
     * 会被注入字节码的类调用
     *
     * @param stackTraceElement 堆栈信息
     */
    @Deprecated
    public void addNode(StackTraceElement[] stackTraceElement) {
        System.out.println("====>" + Arrays.toString(stackTraceElement));
        this.currentStackElements = stackTraceElement;
        GlobalUtil.write(this);
    }

    /**
     * 会在被注入字节码的类中调用，用于统计堆栈信息
     */
    public void addNode() {
        this.currentStackElements = Thread.currentThread().getStackTrace();
        GlobalUtil.write(this);
    }

    public StackNode() {
        currentThreadFlag.set(0);
    }

    public StackNode(int newValue, StackNode oldTree) {

      /*  currentThreadFlag.set(newValue);
        for (StackTraceElement stackTraceElement : oldTree.getStackTraceElements()) {
            System.out.println(stackTraceElement.getLineNumber() + "===》" + stackTraceElement.getClassName() + "#" + stackTraceElement.getMethodName() + "  " + stackTraceElement.getFileName());
        }*/
    }

    private RandomAccessFile rw = null;
    private int off;


    /**
     * 堆栈信息节点
     */
    @Data
    public static class Node implements Serializable {
        private String className;
        private String methodName;
        private String fileName;
        private long lineNum;

        private String stackInfo;

        private Long parentId;
        private long id;
        private List<Node> child;

        public Node(Long parentId, long id) {
            this.parentId = parentId;
            this.id = id;
        }

        public void createStackInfo(StackTraceElement element) {
            this.stackInfo = LocalDateTime.now() + " " + Thread.currentThread().getName() + " " +
                    element.getLineNumber() + " " + element.getClassName() + "#" + element.getMethodName() + "  " + element.getFileName() + "\n";
        }

        public static void main(String[] args) {
            Node node = new Node(null, 1);
            node.setStackInfo("我是root节点" + node.getId());


            List<Node> child1 = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Node temp = new Node(node.getId(), node.getId() * 10 + i);
                temp.setStackInfo("我是一级子节点" + temp.getId());
                child1.add(temp);
                if (i == 2) {
                    List<Node> child2 = new ArrayList<>();
                    for (int j = 0; j < 2; j++) {
                        Node node1 = new Node(temp.getId(), temp.getId() * 10 + j);
                        node1.setStackInfo("我是二级子节点" + node1.getId());
                        child2.add(node1);

                    }
                    temp.setChild(child2);
                }
            }
            node.setChild(child1);

            List<Node> child2 = new ArrayList<>();
            Node node1 = node.getChild().get(node.getChild().size() - 1);
            for (int i = 0; i < 2; i++) {
                Node node2 = new Node(node.getParentId(), node1.getId() + i+1);
                node2.setStackInfo("我是一级子节点" + node2.getId());
                child2.add(node2);
            }
            node.getChild().addAll(child2);

            GlobalUtil.printNodeTreeByParent(node);
        }
    }

}