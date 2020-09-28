package com.github.mrlawrenc.attach.util;

import com.github.mrlawrenc.attach.stack.StackNode;
import com.github.mrlawrenc.attach.statistics.Statistics;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author hz20035009-逍遥
 * date   2020/7/15 14:16
 * <p>
 * agent收集器
 */
@Slf4j
public class Collector {
    /**
     * 所有构建成功的node，每一个node都为一棵树
     */
    public static final List<StackNode.Node> NODE_TREE_LIST = new ArrayList<>();

    /**
     * 每一次执行流程会生成一个唯一id，改唯一id对应一系列统计{@link Statistics}信息和一条堆栈{@link StackNode}信息
     */
    private static final Map<String, ResultContainer> RESULT = new HashMap<>();


    static {
        //启动定时任务不断刷新 node tree
        new Timer("node-tree-update").schedule(new TimerTask() {
            @Override
            public void run() {
                Collector.updateNodeTree();
                Collector.NODE_TREE_LIST.forEach(StackNode.Node::printNodeTreeByParent);
            }
        }, 0, 1000 * 60);
    }

    public static void addStatistics(Statistics statistics) {
        ResultContainer resultContainer = getContainer(statistics.getId());
        List<Statistics> statisticsList = resultContainer.getStatisticsList();
        if (!statisticsList.contains(statistics)) {
            resultContainer.getStatisticsList().add(statistics);
        }
    }

    public static void addNode(String id, StackNode.Node node) {
        ResultContainer resultContainer = getContainer(id);
        List<StackNode.Node> nodeList = resultContainer.getNodeList();
        nodeList.add(node);
    }

    public static ResultContainer getContainer(String id) {
        ResultContainer resultContainer = RESULT.get(id);
        if (Objects.isNull(resultContainer)) {
            resultContainer = new ResultContainer();
            RESULT.put(id, resultContainer);
        }
        return resultContainer;
    }

    /**
     * 更新 总链 树
     */
    public static void updateNodeTree() {
        RESULT.values().forEach(c -> {
            List<StackNode.Node> nodeList = c.getNodeList();
            if (nodeList.size() > 0) {
                buildStack(nodeList);
            }

        });
    }

    /**
     * 将当前堆栈链，追加到总链上
     *
     * @param currentChain 某一条堆堆栈执行链
     */
    private static void buildStack(List<StackNode.Node> currentChain) {
        log.info("start build stack tree,parent size:{} current size:{}", NODE_TREE_LIST.size(), currentChain.size());

        StackNode.Node head = currentChain.get(0);
        boolean contain = false;
        for (StackNode.Node node : NODE_TREE_LIST) {
            if (node.getClassName().equals(head.getClassName()) && node.getMethodName().equals(head.getMethodName())
                    && node.getLineNum() == head.getLineNum()) {
                contain = true;
                head = node;
                break;
            }
        }
        if (!contain) {
            NODE_TREE_LIST.add(head);
        }
        log.info("parent node : {}", head);
        StackNode.Node currentParent = head;
        for (int i = 1; i < currentChain.size(); i++) {
            buildStack0(currentParent, currentChain.get(i), currentParent.getChild());
            currentParent = currentChain.get(i);
        }
        //head.printNodeTreeByParent();
    }

    private static void buildStack0(StackNode.Node parent, StackNode.Node current, List<StackNode.Node> alreadyExists) {
        if (Objects.nonNull(alreadyExists)) {
            for (StackNode.Node alreadyExistsNode : alreadyExists) {
                //若当前插入的节点已存在，则忽略
                if (alreadyExistsNode.getClassName().equals(current.getClassName())
                        && alreadyExistsNode.getMethodName().equals(current.getMethodName())
                        && alreadyExistsNode.getLineNum() == current.getLineNum()) {
                    return;
                }
            }
        }

        List<StackNode.Node> child = parent.getChild();
        if (child == null) {
            child = new ArrayList<>();
            parent.setChild(child);
        }
        child.add(current);
    }

    @Data
    public static class ResultContainer {
        /**
         * 额外统计信息
         */
        private List<Statistics> statisticsList = new ArrayList<>();
        /**
         * 堆栈统计信息
         */
        private List<StackNode.Node> nodeList = new ArrayList<>();
    }
}