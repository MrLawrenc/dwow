package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.write.Writeable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author MrLawrenc
 * date  2020/7/8 22:49
 * 堆栈二叉树
 */
@Data
@Slf4j
public class StackNode implements Writeable {
    /**
     * 每次流程唯一标识
     */
    private String id;
    /**
     * 线程唯一标识，若处在线程池循环中，则会在插桩处更新该标识，该标识会传递给子线程
     */
    private final AtomicInteger currentThreadFlag = new AtomicInteger(Integer.MIN_VALUE);


    /**
     * 存储每一次流程堆栈
     */
    private static final Map<String, StackNode> ALL_NODE = new ConcurrentHashMap<>();
    /**
     * 某一次堆栈流程的信息
     */
    private List<Node> nodeChain;

    public StackNode() {
        this.id = GlobalUtil.getId();
        ALL_NODE.put(this.id, this);
        this.nodeChain = Collections.synchronizedList(new ArrayList<>());
        // 0为当前方法 ； 1为实例化处，通常在monitor实现类 ； 2才是真的的调用处
        Node parentNode = new Node(null, 1).createStackInfo(new Throwable().getStackTrace()[2]);
        this.nodeChain.add(parentNode);
    }

    /**
     * 会被注入字节码的类调用
     *
     * @param stackTraceElement 堆栈信息
     */
    @Deprecated
    public void addNode(StackTraceElement[] stackTraceElement) {
        System.out.println("====>" + Arrays.toString(stackTraceElement));
        StackTraceElement[] currentStackElements = stackTraceElement;
        GlobalUtil.write(this);
    }

    /**
     * 会在被注入字节码的类中调用，用于统计堆栈信息
     */
    public void addNode() {
        if (true) {
            long parentId = this.nodeChain.get(this.nodeChain.size() - 1).getId();
            Node parentNode = new Node(parentId, parentId * 10 + 1).createStackInfo(new Throwable().getStackTrace()[1]);
            this.nodeChain.add(parentNode);
            System.out.println("====================");
            for (Node node : this.nodeChain) {
                System.out.println(node);
            }
            System.out.println("====================");
        }

        StackTraceElement[] currentStackElements = Thread.currentThread().getStackTrace();
        GlobalUtil.write(this);
    }

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

        public Node createStackInfo(StackTraceElement element) {
            this.stackInfo = LocalDateTime.now() + " " + Thread.currentThread().getName() + " " +
                    element.getLineNumber() + " " + element.getClassName() + "#" + element.getMethodName() + "  " + element.getFileName() + "\n";
            return this;
        }


        /**
         * 以当前node顶层节点，打印出当前node的树形结构
         */
        public void printNodeTreeByParent() {
            printNodeTree(this, GlobalUtil.EMPTY_STR);
        }

        private void printNodeTree(StackNode.Node parentNode, String str) {
            System.out.println(str + parentNode.getStackInfo());
            List<StackNode.Node> child = parentNode.getChild();
            if (Objects.nonNull(child) && child.size() > 0) {
                child.forEach(c -> printNodeTree(c, str + GlobalUtil.TABS));
            }
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
                Node node2 = new Node(node.getParentId(), node1.getId() + i + 1);
                node2.setStackInfo("我是一级子节点" + node2.getId());
                child2.add(node2);
            }
            node.getChild().addAll(child2);

            node.printNodeTreeByParent();
        }
    }

}