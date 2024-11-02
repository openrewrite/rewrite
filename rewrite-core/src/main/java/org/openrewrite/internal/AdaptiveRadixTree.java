/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;

import java.util.Arrays;

@Incubating(since = "8.38.0")
public class AdaptiveRadixTree<V> {
    private transient int size = 0;

    @Nullable
    private Node<V> root;

    private static abstract class Node<V> {
        protected byte[] partialKey;

        protected Node(byte[] partialKey) {
            this.partialKey = partialKey;
        }

        abstract @Nullable V search(byte[] key, int depth);
        abstract Node<V> insert(byte[] key, int depth, V value);

        abstract Node<V> copy(); // New abstract method

        protected boolean matchesPartialKey(byte[] key, int depth) {
            if (depth + partialKey.length > key.length) return false;
            for (int i = 0; i < partialKey.length; i++) {
                if (key[depth + i] != partialKey[i]) return false;
            }
            return true;
        }

        // Helper method to find common prefix length
        protected static int findCommonPrefixLength(byte[] key1, int start1, byte[] key2, int start2) {
            int maxLength = Math.min(key1.length - start1, key2.length);
            int i = 0;
            while (i < maxLength && key1[start1 + i] == key2[i]) {
                i++;
            }
            return i;
        }
    }

    private static class LeafNode<V> extends Node<V> {
        private final V value;

        LeafNode(byte[] partialKey, V value) {
            super(partialKey);
            this.value = value;
        }

        @Override
        @Nullable
        V search(byte[] key, int depth) {
            // Only return the value if we match the entire key
            if (!matchesPartialKey(key, depth)) return null;
            return depth + partialKey.length == key.length ? value : null;
        }

        @Override
        Node<V> insert(byte[] key, int depth, V value) {
            if (matchesPartialKey(key, depth) && depth + partialKey.length == key.length) {
                return new LeafNode<>(partialKey, value);
            }

            int commonPrefix = findCommonPrefixLength(key, depth, partialKey, 0);

            // Create a new Node4 with the common prefix
            byte[] commonKey = Arrays.copyOfRange(key, depth, depth + commonPrefix);
            Node4<V> newNode = new Node4<>(commonKey);

            // Add the existing leaf node
            byte[] remainingOldKey = Arrays.copyOfRange(partialKey, commonPrefix, partialKey.length);
            if (remainingOldKey.length > 0) {
                newNode.addChild(remainingOldKey[0], new LeafNode<>(
                        Arrays.copyOfRange(remainingOldKey, 1, remainingOldKey.length),
                        this.value
                ));
            } else {
                newNode.value = this.value;
            }

            // Add the new leaf node
            byte[] remainingNewKey = Arrays.copyOfRange(key, depth + commonPrefix, key.length);
            if (remainingNewKey.length > 0) {
                newNode.addChild(remainingNewKey[0], new LeafNode<>(
                        Arrays.copyOfRange(remainingNewKey, 1, remainingNewKey.length),
                        value
                ));
            } else {
                newNode.value = value;
            }

            return newNode;
        }

        @Override
        Node<V> copy() {
            return new LeafNode<>(Arrays.copyOf(partialKey, partialKey.length), value);
        }
    }

    // Base class for all internal nodes
    private static abstract class InternalNode<V> extends Node<V> {
        protected @Nullable V value; // Value stored at this node (if any)

        protected InternalNode(byte[] partialKey) {
            super(partialKey);
        }

        abstract @Nullable Node<V> getChild(byte key);
        abstract void addChild(byte key, Node<V> child);

        @Override
        @Nullable
        V search(byte[] key, int depth) {
            if (!matchesPartialKey(key, depth)) return null;
            depth += partialKey.length;

            // We've reached the end of the search key
            if (depth == key.length) {
                return value;  // Could be null if no value was stored at this point
            }

            // If there's more key to search but we've found a value, keep searching
            Node<V> child = getChild(key[depth]);
            return child != null ? child.search(key, depth + 1) : null;
        }

        @Override
        Node<V> insert(byte[] key, int depth, V value) {
            if (!matchesPartialKey(key, depth)) {
                int commonPrefix = findCommonPrefixLength(key, depth, partialKey, 0);

                // Create new node with common prefix
                byte[] commonKey = Arrays.copyOfRange(key, depth, depth + commonPrefix);
                Node4<V> newNode = new Node4<>(commonKey);

                // Add current node with remaining key
                byte[] remainingCurrentKey = Arrays.copyOfRange(partialKey, commonPrefix, partialKey.length);
                if (remainingCurrentKey.length > 0) {
                    // Current node becomes child of new node
                    InternalNode<V> currentNodeCopy = this.cloneWithNewKey(
                            Arrays.copyOfRange(remainingCurrentKey, 1, remainingCurrentKey.length)
                    );
                    try {
                        newNode.addChild(remainingCurrentKey[0], currentNodeCopy);
                    } catch (NodeGrowthException e) {
                        newNode = (Node4<V>)e.getNewNode();
                    }
                } else {
                    // Current node's value and children become new node's
                    newNode.value = this.value;
                    for (int i = 0; i < 256; i++) {
                        Node<V> child = getChild((byte)i);
                        if (child != null) {
                            try {
                                newNode.addChild((byte)i, child);
                            } catch (NodeGrowthException e) {
                                newNode = (Node4<V>)e.getNewNode();
                            }
                        }
                    }
                }

                // Add new value
                byte[] remainingNewKey = Arrays.copyOfRange(key, depth + commonPrefix, key.length);
                if (remainingNewKey.length > 0) {
                    try {
                        newNode.addChild(remainingNewKey[0], new LeafNode<>(
                                Arrays.copyOfRange(remainingNewKey, 1, remainingNewKey.length),
                                value
                        ));
                    } catch (NodeGrowthException e) {
                        return e.getNewNode();
                    }
                } else {
                    newNode.value = value;
                }

                return newNode;
            }

            depth += partialKey.length;

            if (depth == key.length) {
                this.value = value;
                return this;
            }

            byte nextByte = key[depth];
            Node<V> child = getChild(nextByte);

            if (child == null) {
                byte[] remainingKey = Arrays.copyOfRange(key, depth + 1, key.length);
                Node<V> newChild = new LeafNode<>(remainingKey, value);

                try {
                    addChild(nextByte, newChild);
                    return this;
                } catch (NodeGrowthException e) {
                    return e.getNewNode();
                }
            }

            Node<V> newChild = child.insert(key, depth + 1, value);
            if (newChild != child) {
                try {
                    addChild(nextByte, newChild);
                } catch (NodeGrowthException e) {
                    InternalNode<V> larger = e.getNewNode();
                    larger.partialKey = this.partialKey;
                    larger.value = this.value;
                    return larger;
                }
            }
            return this;
        }

        abstract InternalNode<V> cloneWithNewKey(byte[] newKey);
    }

    private static class Node4<V> extends InternalNode<V> {
        private byte[] keys;
        private Node<V>[] children;
        private int size;

        @SuppressWarnings("unchecked")
        Node4(byte[] partialKey) {
            super(partialKey);
            this.keys = new byte[4];
            this.children = (Node<V>[]) new Node[4];
            this.size = 0;
        }

        @Override
        @Nullable Node<V> getChild(byte key) {
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) return children[i];
            }
            return null;
        }

        @Override
        void addChild(byte key, Node<V> child) {
            // Check if we're replacing an existing child
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    children[i] = child;
                    return;
                }
            }

            // If we're at capacity, grow
            if (size >= 4) {
                InternalNode<V> larger = grow();
                larger.addChild(key, child);
                throw new NodeGrowthException(larger);
            }

            // Find insertion point while maintaining sorted order
            int pos = 0;
            while (pos < size && (keys[pos] & 0xFF) < (key & 0xFF)) pos++;

            // Shift elements to make room for new entry
            if (pos < size) {
                System.arraycopy(keys, pos, keys, pos + 1, size - pos);
                System.arraycopy(children, pos, children, pos + 1, size - pos);
            }

            keys[pos] = key;
            children[pos] = child;
            size++;
        }

        private InternalNode<V> grow() {
            Node16<V> node = new Node16<>(partialKey);
            node.value = this.value;
            for (int i = 0; i < size; i++) {
                node.addChild(keys[i], children[i]);
            }
            return node;
        }

        @Override
        Node<V> insert(byte[] key, int depth, V value) {
            if (!matchesPartialKey(key, depth)) {
                return super.insert(key, depth, value);
            }

            depth += partialKey.length;

            if (depth == key.length) {
                this.value = value;
                return this;
            }

            byte nextByte = key[depth];
            Node<V> child = getChild(nextByte);

            if (child == null) {
                // Need to add a new child
                if (size >= 4) {
                    // If we're full, grow first
                    InternalNode<V> larger = grow();
                    return larger.insert(key, depth - partialKey.length, value);
                }

                byte[] remainingKey = Arrays.copyOfRange(key, depth + 1, key.length);
                Node<V> newChild = new LeafNode<>(remainingKey, value);
                addChild(nextByte, newChild);
                return this;
            }

            Node<V> newChild = child.insert(key, depth + 1, value);
            if (newChild != child) {
                try {
                    addChild(nextByte, newChild);
                } catch (NodeGrowthException e) {
                    return e.getNewNode();
                }
            }
            return this;
        }

        @Override
        InternalNode<V> cloneWithNewKey(byte[] newKey) {
            Node4<V> clone = new Node4<>(newKey);
            clone.value = this.value;
            System.arraycopy(this.keys, 0, clone.keys, 0, this.size);
            System.arraycopy(this.children, 0, clone.children, 0, this.size);
            clone.size = this.size;
            return clone;
        }

        @Override
        Node<V> copy() {
            Node4<V> clone = new Node4<>(Arrays.copyOf(partialKey, partialKey.length));
            clone.value = this.value;
            clone.size = this.size;
            clone.keys = Arrays.copyOf(this.keys, this.keys.length);
            clone.children = Arrays.copyOf(this.children, this.children.length);
            // Deep copy children
            for (int i = 0; i < size; i++) {
                clone.children[i] = children[i].copy();
            }
            return clone;
        }
    }

    private static class Node16<V> extends InternalNode<V> {
        private byte[] keys;
        private Node<V>[] children;
        private int size;

        @SuppressWarnings("unchecked")
        Node16(byte[] partialKey) {
            super(partialKey);
            this.keys = new byte[16];
            this.children = (Node<V>[]) new Node[16];
            this.size = 0;
        }

        @Override
        @Nullable Node<V> getChild(byte key) {
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) return children[i];
            }
            return null;
        }

        @Override
        void addChild(byte key, Node<V> child) {
            // Check if we're replacing an existing child
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    children[i] = child;
                    return;
                }
            }

            // If we're at capacity, grow
            if (size >= 16) {
                InternalNode<V> larger = grow();
                larger.addChild(key, child);
                throw new NodeGrowthException(larger);
            }

            // Find insertion point while maintaining sorted order
            int pos = 0;
            while (pos < size && (keys[pos] & 0xFF) < (key & 0xFF)) pos++;

            // Shift elements to make room for new entry
            if (pos < size) {
                System.arraycopy(keys, pos, keys, pos + 1, size - pos);
                System.arraycopy(children, pos, children, pos + 1, size - pos);
            }

            keys[pos] = key;
            children[pos] = child;
            size++;
        }

        private InternalNode<V> grow() {
            Node48<V> node = new Node48<>(partialKey);
            node.value = this.value;
            for (int i = 0; i < size; i++) {
                node.addChild(keys[i], children[i]);
            }
            return node;
        }

        @Override
        InternalNode<V> cloneWithNewKey(byte[] newKey) {
            Node16<V> clone = new Node16<>(newKey);
            clone.value = this.value;
            System.arraycopy(this.keys, 0, clone.keys, 0, this.size);
            System.arraycopy(this.children, 0, clone.children, 0, this.size);
            clone.size = this.size;
            return clone;
        }

        @Override
        Node<V> copy() {
            Node16<V> clone = new Node16<>(Arrays.copyOf(partialKey, partialKey.length));
            clone.value = this.value;
            clone.size = this.size;
            clone.keys = Arrays.copyOf(this.keys, this.keys.length);
            clone.children = Arrays.copyOf(this.children, this.children.length);
            // Deep copy children
            for (int i = 0; i < size; i++) {
                clone.children[i] = children[i].copy();
            }
            return clone;
        }
    }

    private static class Node48<V> extends InternalNode<V> {
        private byte[] index;
        private Node<V>[] children;
        private int size;

        @SuppressWarnings("unchecked")
        Node48(byte[] partialKey) {
            super(partialKey);
            this.index = new byte[256];
            Arrays.fill(this.index, (byte)-1);
            this.children = (Node<V>[]) new Node[48];
            this.size = 0;
        }

        @Override
        @Nullable Node<V> getChild(byte key) {
            int idx = index[key & 0xFF];
            return idx >= 0 ? children[idx] : null;
        }

        @Override
        void addChild(byte key, Node<V> child) {
            int keyIndex = key & 0xFF;

            // Check if we're replacing an existing child
            if (index[keyIndex] >= 0) {
                children[index[keyIndex]] = child;
                return;
            }

            // If we're at capacity, grow
            if (size >= 48) {
                InternalNode<V> larger = grow();
                larger.addChild(key, child);
                throw new NodeGrowthException(larger);
            }

            // Add new child
            index[keyIndex] = (byte)size;
            children[size] = child;
            size++;
        }

        private InternalNode<V> grow() {
            Node256<V> node = new Node256<>(partialKey);
            node.value = this.value;
            for (int i = 0; i < 256; i++) {
                if (index[i] >= 0) {
                    node.addChild((byte)i, children[index[i]]);
                }
            }
            return node;
        }

        @Override
        InternalNode<V> cloneWithNewKey(byte[] newKey) {
            Node48<V> clone = new Node48<>(newKey);
            clone.value = this.value;
            System.arraycopy(this.index, 0, clone.index, 0, this.index.length);
            System.arraycopy(this.children, 0, clone.children, 0, this.size);
            clone.size = this.size;
            return clone;
        }

        @Override
        Node<V> copy() {
            Node48<V> clone = new Node48<>(Arrays.copyOf(partialKey, partialKey.length));
            clone.value = this.value;
            clone.size = this.size;
            clone.index = Arrays.copyOf(this.index, this.index.length);
            clone.children = Arrays.copyOf(this.children, this.children.length);
            // Deep copy children
            for (int i = 0; i < size; i++) {
                clone.children[i] = children[i].copy();
            }
            return clone;
        }
    }

    private static class Node256<V> extends InternalNode<V> {
        private final @Nullable Node<V>[] children;
        private int size;

        @SuppressWarnings("unchecked")
        Node256(byte[] partialKey) {
            super(partialKey);
            this.children = (Node<V>[]) new Node[256];
            this.size = 0;
        }

        @Override
        @Nullable Node<V> getChild(byte key) {
            return children[key & 0xFF];
        }

        @Override
        void addChild(byte key, Node<V> child) {
            if (children[key & 0xFF] == null) {
                size++;
            }
            children[key & 0xFF] = child;
        }

        @Override
        InternalNode<V> cloneWithNewKey(byte[] newKey) {
            Node256<V> clone = new Node256<>(newKey);
            clone.value = this.value;
            System.arraycopy(this.children, 0, clone.children, 0, this.children.length);
            clone.size = this.size;
            return clone;
        }

        @Override
        Node<V> copy() {
            Node256<V> clone = new Node256<>(Arrays.copyOf(partialKey, partialKey.length));
            clone.value = this.value;
            clone.size = this.size;
            System.arraycopy(this.children, 0, clone.children, 0, this.children.length);
            // Deep copy children
            for (int i = 0; i < 256; i++) {
                if (children[i] != null) {
                    clone.children[i] = children[i].copy();
                }
            }
            return clone;
        }
    }

    // Custom exception to handle node growth
    private static class NodeGrowthException extends RuntimeException {
        private final InternalNode<?> newNode;

        NodeGrowthException(InternalNode<?> newNode) {
            this.newNode = newNode;
        }

        @SuppressWarnings("unchecked")
        <V> InternalNode<V> getNewNode() {
            return (InternalNode<V>) newNode;
        }
    }

    public void insert(String key, V value) {
        byte[] keyBytes = key.getBytes();
        if (root == null) {
            root = new LeafNode<>(keyBytes, value);
        } else {
            root = root.insert(keyBytes, 0, value);
        }
    }

    @Nullable public V search(String key) {
        if (root == null) return null;
        return root.search(key.getBytes(), 0);
    }

    public AdaptiveRadixTree<V> copy() {
        AdaptiveRadixTree<V> newTree = new AdaptiveRadixTree<>();
        if (root != null) {
            newTree.root = root.copy();
        }
        return newTree;
    }

    public void clear() {
        root = null;
    }
}