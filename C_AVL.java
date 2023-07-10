import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import java.util.concurrent.locks.*;
import java.util.stream.*;

/**
 * This AVL tree is a monitor
 * Conditional variables
 */

public class C_AVL {
    public int item; // test variable
    private AVLNode<Integer> root;
    private Condition writeAvailable, readNext;

    public void rotate(AVLNode<Integer> node, int invert) {
        // 0 for rotating left
        // 1 for rotating right
        AVLNode<Integer> leftChild = node.getChild(0);
        AVLNode<Integer> rightChild = node.getChild(1);

        if (invert == 1) {
            node.setChild(leftChild.getChild(1), 0);
            leftChild.setChild(node, 1);
            updateHeight(leftChild);
        } else {
            node.setChild(rightChild.getChild(0), 1);
            rightChild.setChild(node, 0);
            updateHeight(rightChild);
        }
        updateHeight(node);
    }

    public void updateHeight(AVLNode<Integer> node) {
        assert(node != null);
        AVLNode<Integer> left = node.getChild(0), right = node.getChild(1);
        node.setHeight(1 + Math.max(obtainHeight(left), obtainHeight(right)));
    }

    public int obtainHeight(AVLNode<Integer> node) {
        return (node == null) ? -1 : node.getHeight();
    }

    public int checkBal(AVLNode<Integer> node) {
        return (node == null) ? 0 : obtainHeight(node.getChild(1)) - obtainHeight(node.getChild(0));
    }

    public AVLNode<Integer> mostLeftChild(AVLNode<Integer> node) {
        AVLNode<Integer> left = node.getChild(0);
        if (left == null) {
            return node;
        }
        return mostLeftChild(left);
    }

    public AVLNode<Integer> rebalance(AVLNode<Integer> node) {
        updateHeight(node);
        int bal = checkBal(node);
        if (bal > 1) {
            if (obtainHeight(node.getChild(1).getChild(1)) > obtainHeight(node.getChild(1).getChild(0))) {
                rotate(node, 0);
            } else {
                rotate(node.getChild(1), 1);
                rotate(node, 0);
            }
        } else if (bal < -1) {
            if (obtainHeight(node.getChild(0).getChild(0)) > obtainHeight(node.getChild(0).getChild(1))) {
                rotate(node, 1);
            } else {
                rotate(node.getChild(0), 0);
                rotate(node, 1);
            }
        }

        return node;
    }

    public AVLNode<Integer> insert(AVLNode<Integer> node, Integer item) {
        if (node == null) {
            return new AVLNode<Integer>(item, 0);
        } else if (node.getItem() == item) {
            node.addCount();
            return node;
        } else if (node.getItem() != item) {
            // custom comparator
            // TODO: check int
            if (item > node.getItem()) {
                node = node.getChild(1);
            } else {
                node = node.getChild(0);
            }
        } else {
            throw new RuntimeException("uhhh node can't be inserted fml");
        }
        return null;
    }

    public AVLNode<Integer> delete(AVLNode<Integer> node, Integer item) {
        if (node == null) {
            return node;
        } else if (node.getItem() != item) {
            // custom comparator
            // TODO: check int
            if (item > node.getItem()) {
                node = node.getChild(1);
            } else {
                node = node.getChild(0);
            }
        } else {
            if (node.reduceCount()) {
                if (node.getChild(0) == null || node.getChild(1) == null) {
                    node = (node.getChild(0) == null) ? node.getChild(1) : node.getChild(0);
                } else {
                    AVLNode<Integer> mostLeftChild = mostLeftChild(node.getChild(1));
                    node.setItem(mostLeftChild.getItem());
                    node.setCount(mostLeftChild.getCount());

                    node.setChild(delete(node.getChild(1), node.getItem()), 1);
                }
            }
        }
        if (node != null) {
            node = rebalance(node);
        }
        return node;
    }

    public AVLNode<Integer> search(Integer item) {
        AVLNode<Integer> curr = root;
        while (curr != null) {
            if (curr.getItem() == item) break;
            // need something like a custom operator, this is a template for int
            curr = (item > curr.getItem()) ? curr.getChild(1) : curr.getChild(0);
        }
        return curr;
    }
}

