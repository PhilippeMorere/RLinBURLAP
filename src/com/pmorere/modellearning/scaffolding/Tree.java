package com.pmorere.modellearning.scaffolding;

import java.util.*;

/**
 * Created by philippe on 09/03/15.
 */
public class Tree<T> {
    public Node<T> root;

    public Tree(T rootData) {
        root = new Node<T>();
        root.data = rootData;
        root.children = new ArrayList<Node<T>>();
    }

    public static class Node<T> {
        public T data;
        public Node<T> parent;
        public List<Node<T>> children;

        public void addChild(Node child, T data) {
            child.parent = this;
            child.data = data;
            child.children = new ArrayList<Node<T>>();
            this.children.add(child);
        }
    }

    public static class BottomUpTraversal implements Iterator {
        private Node root;
        private Node currentNode;
        private Set<Node> seenNodes = new HashSet<Node>();

        public BottomUpTraversal(Node root) {
            this.root = root;
            this.currentNode = root;
        }

        @Override
        public boolean hasNext() {
            // Check if all root's children are done
            for (int i = 0; i < root.children.size(); i++)
                if (!nodeHasBeenSeen((Node) root.children.get(i)))
                    return true;
            return false;
        }

        @Override
        public Object next() {
            // It's the root
            if (currentNode == null)
                return null;

            // It's a leaf, it hasn't been seen
            if (!nodeHasBeenSeen(currentNode) && currentNode.children.isEmpty()) {
                setNodeSeen(currentNode);
                return currentNode;
            }

            // Find a not-seen child node
            for (int i = 0; i < currentNode.children.size(); i++)
                if (!nodeHasBeenSeen((Node) currentNode.children.get(i))) {
                    currentNode = (Node) currentNode.children.get(i);
                    return next();
                }

            // All children have been seen

            // See the current node
            if (!nodeHasBeenSeen(currentNode)) {
                setNodeSeen(currentNode);
                return currentNode;
            }

            // Look for a parent
            currentNode = currentNode.parent;
            return next();
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented.");
        }

        public boolean nodeHasBeenSeen(Node n) {
            return seenNodes.contains(n);
        }

        public void setNodeSeen(Node n) {
            seenNodes.add(n);
        }
    }
}
