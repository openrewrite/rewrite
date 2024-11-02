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

import java.nio.charset.StandardCharsets;
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
            int len = partialKey.length;
            if (depth + len > key.length) return false;
            switch (len) {
                case 0:
                    return true;
                case 1:
                    return key[depth] == partialKey[0];
                case 2:
                    return key[depth] == partialKey[0] && key[depth + 1] == partialKey[1];
                default:
                    for (int i = 0; i < len; i++) {
                        if (key[depth + i] != partialKey[i]) return false;
                    }
                    return true;
            }
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
            // Fast path for empty and single-byte partial key
            switch (partialKey.length) {
                case 0:
                    return depth == key.length ? value : null;
                case 1:
                    return depth < key.length && key[depth] == partialKey[0] &&
                           depth + 1 == key.length ? value : null;
            }

            // Standard implementation for longer keys
            if (!matchesPartialKey(key, depth)) return null;
            return depth + partialKey.length == key.length ? value : null;
        }

        @Override
        Node<V> insert(byte[] key, int depth, V value) {
            // Fast path for empty partial key
            if (partialKey.length == 0) {
                if (depth == key.length) {
                    return new LeafNode<>(partialKey, value);
                }
                Node4<V> newNode = new Node4<>(partialKey);
                newNode.value = this.value;

                // Optimize this common case - we know the exact length needed
                byte[] remainingKey;
                int remainingLength = key.length - (depth + 1);
                if (remainingLength == 0) {
                    remainingKey = new byte[0];
                } else {
                    remainingKey = Arrays.copyOfRange(key, depth + 1, key.length);
                }
                newNode.addChild(key[depth], new LeafNode<>(remainingKey, value));
                return newNode;
            }

            if (matchesPartialKey(key, depth) && depth + partialKey.length == key.length) {
                return new LeafNode<>(partialKey, value);
            }

            int commonPrefix = findCommonPrefixLength(key, depth, partialKey, 0);
            byte[] commonKey = Arrays.copyOfRange(key, depth, depth + commonPrefix);
            Node4<V> newNode = new Node4<>(commonKey);

            int remainingOldLength = partialKey.length - commonPrefix;
            if (remainingOldLength > 0) {
                byte firstByte = partialKey[commonPrefix];
                newNode.addChild(firstByte, new LeafNode<>(
                        Arrays.copyOfRange(partialKey, commonPrefix + 1, partialKey.length),
                        this.value
                ));
            } else {
                newNode.value = this.value;
            }

            int remainingNewLength = key.length - (depth + commonPrefix);
            if (remainingNewLength > 0) {
                byte firstByte = key[depth + commonPrefix];
                newNode.addChild(firstByte, new LeafNode<>(
                        Arrays.copyOfRange(key, depth + commonPrefix + 1, key.length),
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

        // Return the new node if growth occurred, otherwise null
        abstract @Nullable InternalNode<V> addChildWithGrowth(byte key, Node<V> child);

        void addChild(byte key, Node<V> child) {
            addChildWithGrowth(key, child);
        }

        @Override
        @Nullable
        V search(byte[] key, int depth) {
            // Fast path for empty partial key
            if (partialKey.length == 0) {
                if (depth == key.length) {
                    return value;
                }
                Node<V> child = getChild(key[depth]);
                return child != null ? child.search(key, depth + 1) : null;
            }

            if (!matchesPartialKey(key, depth)) return null;
            depth += partialKey.length;

            // We've reached the end of the search key
            if (depth == key.length) {
                return value;
            }

            // If there's more key to search but we've found a value, keep searching
            Node<V> child = getChild(key[depth]);
            return child != null ? child.search(key, depth + 1) : null;
        }

        @Override
        Node<V> insert(byte[] key, int depth, V value) {
            if (!matchesPartialKey(key, depth)) {
                int commonPrefix = findCommonPrefixLength(key, depth, partialKey, 0);

                byte[] commonKey = Arrays.copyOfRange(key, depth, depth + commonPrefix);
                Node4<V> newNode = new Node4<>(commonKey);

                int remainingCurrentLength = partialKey.length - commonPrefix;
                if (remainingCurrentLength > 0) {
                    byte firstByte = partialKey[commonPrefix];
                    InternalNode<V> currentNodeCopy = this.cloneWithNewKey(
                            Arrays.copyOfRange(partialKey, commonPrefix + 1, partialKey.length)
                    );
                    InternalNode<V> grown = newNode.addChildWithGrowth(firstByte, currentNodeCopy);
                    if (grown != null) {
                        newNode = (Node4<V>)grown;
                    }
                } else {
                    newNode.value = this.value;
                    for (int i = 0; i < 256; i++) {
                        Node<V> child = getChild((byte) i);
                        if (child != null) {
                            InternalNode<V> grown = newNode.addChildWithGrowth((byte)i, child);
                            if (grown != null) {
                                newNode = (Node4<V>)grown;
                            }
                        }
                    }
                }

                int remainingNewLength = key.length - (depth + commonPrefix);
                if (remainingNewLength > 0) {
                    byte firstByte = key[depth + commonPrefix];
                    Node<V> leafNode = new LeafNode<>(
                            Arrays.copyOfRange(key, depth + commonPrefix + 1, key.length),
                            value
                    );
                    InternalNode<V> grown = newNode.addChildWithGrowth(firstByte, leafNode);
                    return grown != null ? grown : newNode;
                } else {
                    newNode.value = value;
                    return newNode;
                }
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
                InternalNode<V> grown = addChildWithGrowth(nextByte, newChild);
                return grown != null ? grown : this;
            }

            Node<V> newChild = child.insert(key, depth + 1, value);
            if (newChild != child) {
                InternalNode<V> grown = addChildWithGrowth(nextByte, newChild);
                if (grown != null) {
                    grown.partialKey = this.partialKey;
                    grown.value = this.value;
                    return grown;
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
        @Nullable
        Node<V> getChild(byte key) {
            // Unrolled loop for Node4 since it's the most common case
            switch (size) {
                case 4:
                    if (keys[3] == key) return children[3];
                case 3:
                    if (keys[2] == key) return children[2];
                case 2:
                    if (keys[1] == key) return children[1];
                case 1:
                    if (keys[0] == key) return children[0];
                default:
                    return null;
            }
        }

        @Override
        @Nullable
        InternalNode<V> addChildWithGrowth(byte key, Node<V> child) {
            // Check if we're replacing an existing child
            switch (size) {
                case 4:
                    if (keys[3] == key) {
                        children[3] = child;
                        return null;
                    }
                case 3:
                    if (keys[2] == key) {
                        children[2] = child;
                        return null;
                    }
                case 2:
                    if (keys[1] == key) {
                        children[1] = child;
                        return null;
                    }
                case 1:
                    if (keys[0] == key) {
                        children[0] = child;
                        return null;
                    }
            }

            // If we're at capacity, grow
            if (size == 4) {
                Node16<V> node = new Node16<>(partialKey);
                node.value = this.value;
                for (int i = 0; i < size; i++) {
                    node.addChildWithGrowth(keys[i], children[i]);
                }
                node.addChildWithGrowth(key, child);
                return node;
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
            return null;
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
        private static final int LINEAR_SEARCH_THRESHOLD = 8;
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
        @Nullable
        Node<V> getChild(byte key) {
            // Use linear search for small sizes
            if (size <= LINEAR_SEARCH_THRESHOLD) {
                for (int i = 0; i < size; i++) {
                    if (keys[i] == key) return children[i];
                }
                return null;
            }

            // Use binary search for larger sizes
            int low = 0;
            int high = size - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midVal = keys[mid] & 0xFF;
                int keyVal = key & 0xFF;

                if (midVal < keyVal)
                    low = mid + 1;
                else if (midVal > keyVal)
                    high = mid - 1;
                else
                    return children[mid];
            }
            return null;
        }

        @Override
        @Nullable
        InternalNode<V> addChildWithGrowth(byte key, Node<V> child) {
            // Check if we're replacing an existing child
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    children[i] = child;
                    return null;
                }
            }

            // If we're at capacity, grow
            if (size >= 16) {
                Node48<V> node = new Node48<>(partialKey);
                node.value = this.value;
                for (int i = 0; i < size; i++) {
                    node.addChildWithGrowth(keys[i], children[i]);
                }
                node.addChildWithGrowth(key, child);
                return node;
            }

            // Find insertion point while maintaining sorted order
            int pos = 0;
            while (pos < size && (keys[pos] & 0xFF) < (key & 0xFF)) pos++;

            if (pos < size) {
                System.arraycopy(keys, pos, keys, pos + 1, size - pos);
                System.arraycopy(children, pos, children, pos + 1, size - pos);
            }

            keys[pos] = key;
            children[pos] = child;
            size++;
            return null;
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
            Arrays.fill(this.index, (byte) -1);
            this.children = (Node<V>[]) new Node[48];
            this.size = 0;
        }

        @Override
        @Nullable
        Node<V> getChild(byte key) {
            int idx = index[key];
            return idx >= 0 ? children[idx] : null;
        }

        @Override
        @Nullable
        InternalNode<V> addChildWithGrowth(byte key, Node<V> child) {
            if (index[key] >= 0) {
                children[index[key]] = child;
                return null;
            }

            if (size >= 48) {
                Node256<V> node = new Node256<>(partialKey);
                node.value = this.value;
                for (int i = 0; i < 256; i++) {
                    if (index[i] >= 0) {
                        node.addChildWithGrowth((byte) i, children[index[i]]);
                    }
                }
                node.addChildWithGrowth(key, child);
                return node;
            }

            index[key] = (byte) size;
            children[size] = child;
            size++;
            return null;
        }

        private InternalNode<V> grow() {
            Node256<V> node = new Node256<>(partialKey);
            node.value = this.value;
            for (int i = 0; i < 256; i++) {
                if (index[i] >= 0) {
                    node.addChild((byte) i, children[index[i]]);
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
        @Nullable
        Node<V> getChild(byte key) {
            return children[key & 0xFF];
        }

        @Override
        @Nullable
        InternalNode<V> addChildWithGrowth(byte key, Node<V> child) {
            if (children[key] == null) {
                size++;
            }
            children[key] = child;
            return null;
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
        insert(key.getBytes(StandardCharsets.UTF_8), value);
    }

    public void insert(byte[] keyBytes, V value) {
        if (root == null) {
            root = new LeafNode<>(keyBytes, value);
        } else {
            root = root.insert(keyBytes, 0, value);
        }
    }

    public @Nullable V search(String key) {
        if (root == null) return null;
        return search(key.getBytes(StandardCharsets.UTF_8));
    }

    public @Nullable V search(byte[] bytes) {
        if (root == null) return null;
        return root.search(bytes, 0);
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