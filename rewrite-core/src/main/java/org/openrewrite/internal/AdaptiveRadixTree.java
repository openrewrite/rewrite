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

    private final KeyTable keyTable;

    @Nullable
    private Node<V> root;

    public AdaptiveRadixTree() {
        this.keyTable = new KeyTable();
    }

    private AdaptiveRadixTree(KeyTable keyTable) {
        this.keyTable = keyTable;
    }

    private static abstract class Node<V> {
        protected int keyOffset;
        protected int keyLength;

        protected Node(int keyOffset, int keyLength) {
            this.keyOffset = keyOffset;
            this.keyLength = keyLength;
        }

        abstract @Nullable V search(byte[] key, int depth, KeyTable keyTable);

        abstract Node<V> insert(byte[] key, int depth, V value, KeyTable keyTable);

        abstract Node<V> copy(); // New abstract method

        protected boolean matchesPartialKey(byte[] key, int depth, KeyTable keyTable) {
            return keyTable.matches(key, depth, keyOffset, keyLength);
        }
    }

    private static class LeafNode<V> extends Node<V> {
        private final V value;

        LeafNode(int keyOffset, int keyLength, V value) {
            super(keyOffset, keyLength);
            this.value = value;
        }

        static <V> LeafNode<V> create(byte[] key, int offset, int length, V value, KeyTable keyTable) {
            if (length <= 0) {
                return new LeafNode<>(-1, 0, value);
            }
            int keyOffset = keyTable.store(key, offset, length);
            return new LeafNode<>(keyOffset, length, value);
        }

        @Override
        @Nullable
        V search(byte[] key, int depth, KeyTable keyTable) {
            // Fast path for empty and single-byte partial key
            switch (keyLength) {
                case 0:
                    return depth == key.length ? value : null;
                case 1:
                    return depth < key.length && key[depth] == keyTable.get(keyOffset) &&
                           depth + 1 == key.length ? value : null;
            }

            // Standard implementation for longer keys
            if (!matchesPartialKey(key, depth, keyTable)) return null;
            return depth + keyLength == key.length ? value : null;
        }

        @Override
        Node<V> insert(byte[] key, int depth, V value, KeyTable keyTable) {
            // Fast path for empty partial key
            if (keyLength == 0) {
                if (depth == key.length) {
                    return new LeafNode<>(-1, 0, value);
                }
                Node4<V> newNode = new Node4<>(-1, 0);
                newNode.value = this.value;
                Node<V> newChild = create(key, depth + 1, key.length - (depth + 1), value, keyTable);
                newNode.addChild(key[depth], newChild, keyTable);
                return newNode;
            }

            if (depth + keyLength == key.length && keyTable.matches(key, depth, keyOffset, keyLength)) {
                return new LeafNode<>(keyOffset, keyLength, value);
            }

            // Find common prefix without creating arrays
            int commonPrefix = 0;
            int maxLength = Math.min(key.length - depth, keyLength);
            while (commonPrefix < maxLength && key[depth + commonPrefix] == keyTable.get(keyOffset + commonPrefix)) {
                commonPrefix++;
            }

            // Create new node with common prefix
            Node4<V> newNode = new Node4<>(keyOffset, commonPrefix);

            // Handle remaining parts of old key
            int remainingOldLength = keyLength - commonPrefix;
            if (remainingOldLength > 0) {
                byte firstByte = keyTable.get(keyOffset + commonPrefix);
                LeafNode<V> oldChild = new LeafNode<>(
                        keyOffset + commonPrefix + 1,
                        remainingOldLength - 1,
                        this.value);
                newNode.addChild(firstByte, oldChild, keyTable);
            } else {
                newNode.value = this.value;
            }

            // Handle remaining parts of new key
            int remainingNewLength = key.length - (depth + commonPrefix);
            if (remainingNewLength > 0) {
                byte firstByte = key[depth + commonPrefix];
                LeafNode<V> newChild = create(
                        key, depth + commonPrefix + 1, remainingNewLength - 1,
                        value, keyTable);
                newNode.addChild(firstByte, newChild, keyTable);
            } else {
                newNode.value = value;
            }

            return newNode;
        }

        @Override
        Node<V> copy() {
            return new LeafNode<>(keyOffset, keyLength, value);
        }
    }

    // Base class for all internal nodes
    private static abstract class InternalNode<V> extends Node<V> {
        protected @Nullable V value; // Value stored at this node (if any)

        protected InternalNode(int keyOffset, int keyLength) {
            super(keyOffset, keyLength);
        }

        abstract @Nullable Node<V> getChild(byte key);

        // Return the new node if growth occurred, otherwise null
        abstract @Nullable InternalNode<V> addChild(byte key, Node<V> child, KeyTable keyTable);

        void adjustKey(int newKeyOffset, int newKeyLength) {
            this.keyOffset = newKeyOffset;
            this.keyLength = newKeyLength;
        }

        @Override
        @Nullable
        V search(byte[] key, int depth, KeyTable keyTable) {
            // Fast path for empty partial key
            if (keyLength == 0) {
                if (depth == key.length) {
                    return value;
                }
                Node<V> child = getChild(key[depth]);
                return child != null ? child.search(key, depth + 1, keyTable) : null;
            }

            if (!matchesPartialKey(key, depth, keyTable)) return null;
            depth += keyLength;

            // We've reached the end of the search key
            if (depth == key.length) {
                return value;
            }

            // If there's more key to search but we've found a value, keep searching
            Node<V> child = getChild(key[depth]);
            return child != null ? child.search(key, depth + 1, keyTable) : null;
        }

        @Override
        Node<V> insert(byte[] key, int depth, V value, KeyTable keyTable) {
            if (!matchesPartialKey(key, depth, keyTable)) {
                // Find common prefix length
                int commonPrefix = 0;
                int maxLength = Math.min(key.length - depth, keyLength);
                while (commonPrefix < maxLength && key[depth + commonPrefix] == keyTable.get(keyOffset + commonPrefix)) {
                    commonPrefix++;
                }

                Node4<V> newNode = split(commonPrefix, keyTable);

                // Handle remaining parts of new key
                int remainingNewLength = key.length - (depth + commonPrefix);
                if (remainingNewLength > 0) {
                    byte firstByte = key[depth + commonPrefix];
                    Node<V> leafNode = LeafNode.create(
                            key, depth + commonPrefix + 1, remainingNewLength - 1,
                            value, keyTable);
                    InternalNode<V> grown = newNode.addChild(firstByte, leafNode, keyTable);
                    return grown != null ? grown : newNode;
                } else {
                    newNode.value = value;
                    return newNode;
                }
            }

            depth += keyLength;

            // We've reached the end of the key
            if (depth == key.length) {
                this.value = value;
                return this;
            }

            // Continue with child node
            byte nextByte = key[depth];
            Node<V> child = getChild(nextByte);

            if (child == null) {
                // Create new leaf node
                Node<V> newChild = LeafNode.create(key, depth + 1, key.length - (depth + 1), value, keyTable);
                InternalNode<V> grown = addChild(nextByte, newChild, keyTable);
                return grown != null ? grown : this;
            }

            // Recursively insert into child node
            Node<V> newChild = child.insert(key, depth + 1, value, keyTable);
            if (newChild != child) {
                InternalNode<V> grown = addChild(nextByte, newChild, keyTable);
                return grown != null ? grown : this;
            }
            return this;
        }

        private Node4<V> split(int commonPrefix, KeyTable keyTable) {
            Node4<V> newParent = new Node4<>(keyOffset, commonPrefix);
            newParent.value = commonPrefix == keyLength ? this.value : null;

            assert commonPrefix < keyLength;
            byte splitByte = keyTable.get(keyOffset + commonPrefix);
            adjustKey(keyOffset + commonPrefix + 1, keyLength - commonPrefix - 1);
            newParent.addChild(splitByte, this, keyTable);
            return newParent;
        }
    }

    private static class Node4<V> extends InternalNode<V> {
        // Keys and children inline to avoid array overhead
        private byte k0, k1, k2, k3;
        private @Nullable Node<V> c0, c1, c2, c3;
        private byte size;

        Node4(int keyOffset, int keyLength) {
            super(keyOffset, keyLength);
            this.size = 0;
        }

        @Override
        @Nullable
        Node<V> getChild(byte key) {
            int mask = (1 << size) - 1; // Creates mask like 0001, 0011, 0111, 1111
            return ((mask & 1) != 0 && k0 == key) ? c0 :
                    ((mask & 2) != 0 && k1 == key) ? c1 :
                            ((mask & 4) != 0 && k2 == key) ? c2 :
                                    ((mask & 8) != 0 && k3 == key) ? c3 : null;
        }

        @SuppressWarnings("DataFlowIssue")
        @Override
        @Nullable
        InternalNode<V> addChild(byte key, Node<V> child, KeyTable keyTable) {
            // Check if we're replacing an existing child
            if (size > 0) {
                if (k0 == key) {
                    c0 = child;
                    return null;
                }
                if (size > 1) {
                    if (k1 == key) {
                        c1 = child;
                        return null;
                    }
                    if (size > 2 && k2 == key) {
                        c2 = child;
                        return null;
                    }
                    if (size > 3 && k3 == key) {
                        c3 = child;
                        return null;
                    }
                }
            }

            // If we're at capacity, grow to Node16
            if (size == 4) {
                Node16<V> node = new Node16<>(keyOffset, keyLength);
                node.value = this.value;
                // Add existing children in sorted order
                node.addChild(k0, c0, keyTable);
                node.addChild(k1, c1, keyTable);
                node.addChild(k2, c2, keyTable);
                node.addChild(k3, c3, keyTable);
                node.addChild(key, child, keyTable);
                return node;
            }

            // Find insertion point while maintaining sorted order
            byte keyByte = (byte) (key & 0xFF);
            if (size == 0) {
                k0 = keyByte;
                c0 = child;
            } else if (size == 1) {
                if (keyByte < (k0 & 0xFF)) {
                    k1 = k0;
                    c1 = c0;
                    k0 = keyByte;
                    c0 = child;
                } else {
                    k1 = keyByte;
                    c1 = child;
                }
            } else if (size == 2) {
                if (keyByte < (k0 & 0xFF)) {
                    k2 = k1;
                    c2 = c1;
                    k1 = k0;
                    c1 = c0;
                    k0 = keyByte;
                    c0 = child;
                } else if (keyByte < (k1 & 0xFF)) {
                    k2 = k1;
                    c2 = c1;
                    k1 = keyByte;
                    c1 = child;
                } else {
                    k2 = keyByte;
                    c2 = child;
                }
            } else { // size == 3
                if (keyByte < (k0 & 0xFF)) {
                    k3 = k2;
                    c3 = c2;
                    k2 = k1;
                    c2 = c1;
                    k1 = k0;
                    c1 = c0;
                    k0 = keyByte;
                    c0 = child;
                } else if (keyByte < (k1 & 0xFF)) {
                    k3 = k2;
                    c3 = c2;
                    k2 = k1;
                    c2 = c1;
                    k1 = keyByte;
                    c1 = child;
                } else if (keyByte < (k2 & 0xFF)) {
                    k3 = k2;
                    c3 = c2;
                    k2 = keyByte;
                    c2 = child;
                } else {
                    k3 = keyByte;
                    c3 = child;
                }
            }
            size++;
            return null;
        }

        @SuppressWarnings("DataFlowIssue")
        @Override
        Node<V> copy() {
            Node4<V> clone = new Node4<>(keyOffset, keyLength);
            clone.value = this.value;
            clone.size = this.size;
            clone.k0 = this.k0;
            clone.k1 = this.k1;
            clone.k2 = this.k2;
            clone.k3 = this.k3;
            if (size > 0) clone.c0 = c0.copy();
            if (size > 1) clone.c1 = c1.copy();
            if (size > 2) clone.c2 = c2.copy();
            if (size > 3) clone.c3 = c3.copy();
            return clone;
        }
    }

    private static class Node16<V> extends InternalNode<V> {
        private static final int LINEAR_SEARCH_THRESHOLD = 12;
        private byte[] keys;
        private @Nullable Node<V>[] children;
        private int size;

        @SuppressWarnings("unchecked")
        Node16(int keyOffset, int keyLength) {
            super(keyOffset, keyLength);
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

            int idx = unsignedBinarySearch(keys, size, key & 0xFF);
            return idx >= 0 ? children[idx] : null;
        }

        // Custom binary search for unsigned bytes
        private int unsignedBinarySearch(byte[] array, int toIndex, int key) {
            int low = 0;
            int high = toIndex - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midVal = array[mid] & 0xFF;

                if (midVal < key)
                    low = mid + 1;
                else if (midVal > key)
                    high = mid - 1;
                else
                    return mid; // key found
            }
            return -(low + 1);  // key not found
        }

        @Override
        @Nullable
        InternalNode<V> addChild(byte key, Node<V> child, KeyTable keyTable) {
            // Check if we're replacing an existing child
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    children[i] = child;
                    return null;
                }
            }

            // If we're at capacity, grow
            if (size >= 16) {
                Node48<V> node = new Node48<>(keyOffset, keyLength);
                node.value = this.value;
                for (int i = 0; i < size; i++) {
                    //noinspection DataFlowIssue
                    node.addChild(keys[i], children[i], keyTable);
                }
                node.addChild(key, child, keyTable);
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

        @Override
        Node<V> copy() {
            Node16<V> clone = new Node16<>(keyOffset, keyLength);
            clone.value = this.value;
            clone.size = this.size;
            clone.keys = Arrays.copyOf(this.keys, this.keys.length);
            clone.children = Arrays.copyOf(this.children, this.children.length);
            // Deep copy children
            for (int i = 0; i < size; i++) {
                //noinspection DataFlowIssue
                clone.children[i] = children[i].copy();
            }
            return clone;
        }
    }

    private static class Node48<V> extends InternalNode<V> {
        private byte[] index;
        private @Nullable Node<V>[] children;
        private int size;

        @SuppressWarnings("unchecked")
        Node48(int keyOffset, int keyLength) {
            super(keyOffset, keyLength);
            this.index = new byte[256];
            Arrays.fill(this.index, (byte) -1);
            this.children = (Node<V>[]) new Node[48];
            this.size = 0;
        }

        @Override
        @Nullable
        Node<V> getChild(byte key) {
            byte idx = index[key & 0xFF];
            return idx >= 0 ? children[idx] : null;
        }

        @Override
        @Nullable
        InternalNode<V> addChild(byte key, Node<V> child, KeyTable keyTable) {
            int idx = key & 0xFF;
            if (index[idx] >= 0) {
                children[index[idx]] = child;
                return null;
            }

            if (size >= 48) {
                Node256<V> node = new Node256<>(-1, 0);
                node.value = this.value;
                for (int i = 0; i < 256; i++) {
                    if (index[i] >= 0) {
                        //noinspection DataFlowIssue
                        node.addChild((byte) i, children[index[i]], keyTable);
                    }
                }
                node.addChild(key, child, keyTable);
                return node;
            }

            index[idx] = (byte) size;
            children[size] = child;
            size++;
            return null;
        }

        @Override
        Node<V> copy() {
            Node48<V> clone = new Node48<>(keyOffset, keyLength);
            clone.value = this.value;
            clone.size = this.size;
            clone.index = Arrays.copyOf(this.index, this.index.length);
            clone.children = Arrays.copyOf(this.children, this.children.length);
            // Deep copy children
            for (int i = 0; i < size; i++) {
                //noinspection DataFlowIssue
                clone.children[i] = children[i].copy();
            }
            return clone;
        }
    }

    private static class Node256<V> extends InternalNode<V> {
        private final @Nullable Node<V>[] children;

        @SuppressWarnings("unchecked")
        Node256(int keyOffset, int keyLength) {
            super(keyOffset, keyLength);
            this.children = (Node<V>[]) new Node[256];
        }

        @SuppressWarnings("unchecked")
        Node256(int keyOffset, int keyLength, @Nullable Node<V>[] children) {
            super(keyOffset, keyLength);
            this.children = children;
        }

        @Override
        @Nullable
        Node<V> getChild(byte key) {
            return children[key & 0xFF];
        }

        @Override
        @Nullable
        InternalNode<V> addChild(byte key, Node<V> child, KeyTable keyTable) {
            int idx = key & 0xFF;
            children[idx] = child;
            return null;
        }

        @Override
        Node<V> copy() {
            Node256<V> clone = new Node256<>(keyOffset, keyLength);
            clone.value = this.value;
            System.arraycopy(this.children, 0, clone.children, 0, this.children.length);
            // Deep copy children
            for (int i = 0; i < 256; i++) {
                Node<V> child = children[i];
                if (child != null) {
                    clone.children[i] = child.copy();
                }
            }
            return clone;
        }
    }

    public void insert(String key, V value) {
        insert(key.getBytes(StandardCharsets.UTF_8), value);
    }

    public void insert(byte[] keyBytes, V value) {
        if (root == null) {
            root = LeafNode.create(keyBytes, 0, keyBytes.length, value, keyTable);
        } else {
            root = root.insert(keyBytes, 0, value, keyTable);
        }
    }

    public @Nullable V search(String key) {
        if (root == null) return null;
        return search(key.getBytes(StandardCharsets.UTF_8));
    }

    public @Nullable V search(byte[] bytes) {
        if (root == null) return null;
        return root.search(bytes, 0, keyTable);
    }

    public AdaptiveRadixTree<V> copy() {
        AdaptiveRadixTree<V> newTree = new AdaptiveRadixTree<>(keyTable);
        if (root != null) {
            newTree.root = root.copy();
        }
        return newTree;
    }

    public void clear() {
        root = null;
    }

    private static class KeyTable {
        private static final int INITIAL_CAPACITY = 128 * 1024; // 128KiB
        private static final int MAX_SMALL_GROWTH_SIZE = 1024 * 1024; // 1MiB
        private static final double LARGE_GROWTH_FACTOR = 1.3;

        private byte[] storage;
        private int size;

        KeyTable() {
            this.storage = new byte[INITIAL_CAPACITY];
            this.size = 0;
        }

        // Returns offset where the key was stored
        int store(byte[] key, int offset, int length) {
            ensureCapacity(length);
            int startOffset = size;
            System.arraycopy(key, offset, storage, size, length);
            size += length;
            return startOffset;
        }

        boolean matches(byte[] key, int keyOffset, int storedOffset, int length) {
            if (length <= 0) return true;
            if (keyOffset + length > key.length) return false;

            for (int i = 0; i < length; i++) {
                if (key[keyOffset + i] != storage[storedOffset + i]) {
                    return false;
                }
            }
            return true;
        }

        private void ensureCapacity(int additional) {
            int required = size + additional;
            if (required <= storage.length) {
                return;
            }

            int newCapacity;
            if (storage.length < MAX_SMALL_GROWTH_SIZE) {
                // Double the size for small arrays
                newCapacity = Math.max(storage.length * 2, required);
            } else {
                // Grow by 10% for large arrays
                newCapacity = Math.max((int) (storage.length * LARGE_GROWTH_FACTOR), required);
            }
            storage = Arrays.copyOf(storage, newCapacity);
        }

        public byte get(int offset) {
            return storage[offset];
        }
    }
}
