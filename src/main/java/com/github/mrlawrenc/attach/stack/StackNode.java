package com.github.mrlawrenc.attach.stack;

import com.github.mrlawrenc.attach.util.Collector;
import com.github.mrlawrenc.attach.util.GlobalUtil;
import com.github.mrlawrenc.attach.util.ThreadLocalUtil;
import com.github.mrlawrenc.attach.write.Writeable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     * 若进入if逻辑，则代表该线程是线程池复用的线程
     */
    public StackNode() {
        if (Objects.nonNull(ThreadLocalUtil.globalThreadLocal.get())) {
            Collector.updateNodeTree();
            log.info("parent node size:" + Collector.NODE_TREE_LIST.size());
            //输出当前总链信息
            Collector.NODE_TREE_LIST.forEach(Node::printNodeTreeByParent);

            String key = ThreadLocalUtil.globalThreadLocal.get().getId();
            //ALL_NODE.remove(key);
            log.info("thread({}) reuse,remove nodeChain({}).", Thread.currentThread().getName(), key);
        }

        this.id = GlobalUtil.getId();
        // 0为当前方法 ； 1为实例化处，通常在monitor实现类 ； 2才是真的的调用处
        Node parentNode = new Node(null, 1).createStackInfo(new Throwable().getStackTrace()[2]);
        Collector.addNode(id, parentNode);
        log.info("{} new flow start. add new parentNode {}", Thread.currentThread().getName(), parentNode);

    }

    /**
     * 会被注入字节码的类调用
     *
     * @param stackTraceElement 堆栈信息
     */
    @Deprecated
    public void addNode(StackTraceElement[] stackTraceElement) {

    }

    /**
     * 会在被注入字节码的类中调用，用于统计堆栈信息
     */
    public void addNode() {
        String id = ThreadLocalUtil.globalThreadLocal.get().getId();
        List<Node> currentNodeChain = Collector.getContainer(id).getNodeList();
        long parentId = currentNodeChain.get(currentNodeChain.size() - 1).getId();
        Node node = new Node(parentId, parentId * 10 + 1).createStackInfo(new Throwable().getStackTrace()[1]);
        currentNodeChain.add(node);
    }


    /**
     * 根据当前节点找到tree中匹配的顶层节点
     *
     * @param node 当前节点
     * @return tree的顶层节点
     */
    private Node findParentNode(Node node) {
        int idx = -1;
        for (int i = 0; i < Collector.NODE_TREE_LIST.size(); i++) {
            Node pNode = Collector.NODE_TREE_LIST.get(i);
            if (pNode.className.equals(node.getClassName()) && pNode.methodName.equals(node.methodName)
                    && pNode.getLineNum() == node.getLineNum()) {
                idx = i;
            }
        }

        if (idx == -1) {
            Collector.NODE_TREE_LIST.add(node);
            return node;
        }
        return Collector.NODE_TREE_LIST.get(idx);
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
            this.methodName = element.getMethodName();
            this.lineNum = element.getLineNumber();
            this.className = element.getClassName();
            this.fileName = element.getFileName();

            this.stackInfo = LocalDateTime.now() + " " + Thread.currentThread().getName() + " " +
                    this.lineNum + " " + this.className + "#" + this.methodName + "  " + this.fileName;
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