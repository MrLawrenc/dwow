package com.github.mrlawrenc.attach.util;
 
/**
 * 堆栈二叉树
 */
public class StackBinaryTree {
    private char data; // 结点的数据域
    private StackBinaryTree rightSubtree; // 右子树
    private StackBinaryTree leftSubtree; // 左子树
 
    public StackBinaryTree(char data) {
        this.data = data;
        this.leftSubtree = null;
        this.rightSubtree = null;
    }
 
    public void makeRightSubtree(char data) {
        this.rightSubtree = new StackBinaryTree(data);
    }
 
    public void makeLeftSubTree(char data) {
        this.leftSubtree = new StackBinaryTree(data);
    }
 
    public void makeSubtree(char leftData, char rightData) {
        this.leftSubtree = new StackBinaryTree(leftData);
        this.rightSubtree = new StackBinaryTree(rightData);
    }
 
    // 中序遍历
    public void inorderTraversal() {
        if (this.leftSubtree != null) {
            this.leftSubtree.inorderTraversal();
        }
        System.out.print("[" + this.data + "]");
        if (this.rightSubtree != null) {
            this.rightSubtree.inorderTraversal();
        }
    }
 
    // 先序遍历
    public void preOrderTraversal() {
        System.out.print("[" + this.data + "]");
        if (this.leftSubtree != null) {
            this.leftSubtree.preOrderTraversal();
        }
        if (this.rightSubtree != null) {
            this.rightSubtree.preOrderTraversal();
        }
    }
 
    // 后序遍历
    public void postOrderTraversal() {
        if (this.leftSubtree != null) {
            this.leftSubtree.postOrderTraversal();
        }
        if (this.rightSubtree != null) {
            this.rightSubtree.postOrderTraversal();
        }
        System.out.print("[" + this.data + "]");
    }
 
    public StackBinaryTree getRightSubtree() {
        return rightSubtree;
    }
 
    public StackBinaryTree getLeftSubtree() {
        return leftSubtree;
    }
 
    public static void main(String[] args) {
        StackBinaryTree root = new StackBinaryTree('-');
        root.makeLeftSubTree('+');
        root.makeRightSubtree('/');
        root.getLeftSubtree().makeLeftSubTree('a');
        root.getLeftSubtree().makeRightSubtree('*');
        root.getRightSubtree().makeLeftSubTree('e');
        root.getRightSubtree().makeRightSubtree('f');
        root.getLeftSubtree().getRightSubtree().makeSubtree('b', '-');
        root.getLeftSubtree().getRightSubtree().getRightSubtree().makeSubtree('c', 'd');
 
        System.out.print("前序遍历：");
        root.preOrderTraversal();
        System.out.print("\n中序遍历：");
        root.inorderTraversal();
        System.out.print("\n后序遍历：");
        root.postOrderTraversal();
    }
}