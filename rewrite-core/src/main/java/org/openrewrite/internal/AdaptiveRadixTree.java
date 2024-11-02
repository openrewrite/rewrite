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
    private Node root;

    public AdaptiveRadixTree() {
    }

    private AdaptiveRadixTree(AdaptiveRadixTree<V> from) {
        this.root = from.root == null ? null : from.root.copy();
        this.size = from.size;
    }

    public AdaptiveRadixTree<V> copy() {
        return new AdaptiveRadixTree<>(this);
    }

    public void insert(String key, V value) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        if (root == null) {
            // create leaf node and set root to that
            root = new Node.LeafNode<>(bytes, value);
            size = 1;
            return;
        }
        put(bytes, value);
    }

    public void clear() {
        size = 0;
        root = null;
    }

    public @Nullable V search(String key) {
        Node.LeafNode<V> entry = getEntry(key);
        return (entry == null ? null : entry.getValue());
    }

    private Node.@Nullable LeafNode<V> getEntry(String key) {
        if (root == null) { // empty tree
            return null;
        }
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return getEntry(root, bytes);
    }

    // Arrays.equals() only available in Java 9+
    private static boolean equals(byte @Nullable [] a, int aFromIndex, int aToIndex,
                                  byte @Nullable [] b, int bFromIndex, int bToIndex) {
        // Check for null arrays
        if (a == null || b == null) {
            return a == b; // Both are null or one is null
        }

        // Check for invalid index ranges
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return false;
        }

        // Compare the specified ranges
        for (int i = 0; i < (aToIndex - aFromIndex); i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }

        return true;
    }

    private Node.@Nullable LeafNode<V> getEntry(Node node, byte[] key) {
        int depth = 0;
        boolean skippedPrefix = false;
        while (true) {
            if (node instanceof Node.LeafNode) {
                @SuppressWarnings("unchecked") Node.LeafNode<V> leaf = (Node.LeafNode<V>) node;
                byte[] leafBytes = leaf.getKeyBytes();
                int startFrom = skippedPrefix ? 0 : depth;
                if (equals(leafBytes, startFrom, leafBytes.length, key, startFrom, key.length)) {
                    return leaf;
                }
                return null;
            }

            Node.InnerNode innerNode = (Node.InnerNode) node;

            if (key.length < depth + innerNode.prefixLen) {
                return null;
            }

            if (innerNode.prefixLen <= Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
                // match pessimistic compressed path completely
                for (int i = 0; i < innerNode.prefixLen; i++) {
                    if (innerNode.prefixKeys[i] != key[depth + i])
                        return null;
                }
            } else {
                // else take optimistic jump
                skippedPrefix = true;
            }

            // took pessimistic match or optimistic jump, continue search
            depth = depth + innerNode.prefixLen;
            Node nextNode;
            if (depth == key.length) {
                nextNode = innerNode.getLeaf();
                if (!skippedPrefix) {
                    //noinspection unchecked
                    return (Node.LeafNode<V>) nextNode;
                }
            } else {
                nextNode = innerNode.findChild(key[depth]);
                depth++;
            }
            if (nextNode == null) {
                return null;
            }
            // set fields for next iteration
            node = nextNode;
        }
    }

    void replace(int depth, byte[] key, Node.@Nullable InnerNode prevDepth, Node replaceWith) {
        if (prevDepth == null) {
            root = replaceWith;
        } else {
            prevDepth.replace(key[depth - 1], replaceWith);
        }
    }

    private void put(byte[] keyBytes, V value) {
        int depth = 0;
        Node.InnerNode prevDepth = null;
        Node node = root;
        while (true) {
            if (node instanceof Node.LeafNode) {
                @SuppressWarnings("unchecked")
                Node.LeafNode<V> leaf = (Node.LeafNode<V>) node;
                Node pathCompressedNode = lazyExpansion(leaf, keyBytes, value, depth);
                if (pathCompressedNode == node) {
                    // key already exists
                    leaf.setValue(value);
                    return;
                }
                // we gotta replace the prevDepth's child pointer to this new node
                replace(depth, keyBytes, prevDepth, pathCompressedNode);
                size++;
                return;
            }
            // compare with compressed path
            Node.InnerNode innerNode = (Node.InnerNode) node;
            int newDepth = matchCompressedPath(innerNode, keyBytes, value, depth, prevDepth);
            if (newDepth == -1) { // matchCompressedPath already inserted the leaf node for us
                size++;
                return;
            }

            if (keyBytes.length == newDepth) {
                @SuppressWarnings("unchecked") Node.LeafNode<V> leaf = (Node.LeafNode<V>) innerNode.getLeaf();
                leaf.setValue(value);
                return;
            }

            // we're now at line 26 in paper
            byte partialKey = keyBytes[newDepth];
            Node child = innerNode.findChild(partialKey);
            if (child != null) {
                // set fields for next iteration
                prevDepth = innerNode;
                depth = newDepth + 1;
                node = child;
                continue;
            }

            // add this key as child
            Node leaf = new Node.LeafNode<>(keyBytes, value);
            if (innerNode.isFull()) {
                innerNode = innerNode.grow();
                replace(depth, keyBytes, prevDepth, innerNode);
            }
            innerNode.addChild(partialKey, leaf);
            size++;
            return;
        }
    }

    private static <V> Node lazyExpansion(Node.LeafNode<V> leaf, byte[] keyBytes, V value, int depth) {

        // find LCP
        int lcp = 0;
        byte[] leafKey = leaf.getKeyBytes(); // loadKey in paper
        int end = Math.min(leafKey.length, keyBytes.length);
        for (; depth < end && leafKey[depth] == keyBytes[depth]; depth++, lcp++) ;
        if (depth == keyBytes.length && depth == leafKey.length) {
            // we're referring to a key that already exists, replace value and return current
            return leaf;
        }

        // create new node with LCP
        Node.Node4 pathCompressedNode = new Node.Node4();
        pathCompressedNode.prefixLen = lcp;
        int pessimisticLcp = Math.min(lcp, Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT);
        System.arraycopy(keyBytes, depth - lcp, pathCompressedNode.prefixKeys, 0, pessimisticLcp);

        // add new key and old leaf as children
        Node.LeafNode<V> newLeaf = new Node.LeafNode<>(keyBytes, value);
        if (depth == keyBytes.length) {
            // barca to be inserted, barcalona already exists
            // set barca's parent to be this path compressed node
            // setup uplink whenever we set downlink
            pathCompressedNode.setLeaf(newLeaf);
            pathCompressedNode.addChild(leafKey[depth], leaf); // l
        } else if (depth == leafKey.length) {
            // barcalona to be inserted, barca already exists
            pathCompressedNode.setLeaf(leaf);
            pathCompressedNode.addChild(keyBytes[depth], newLeaf); // l
        } else {
            pathCompressedNode.addChild(leafKey[depth], leaf);
            pathCompressedNode.addChild(keyBytes[depth], newLeaf);
        }

        return pathCompressedNode;
    }

    static void removeOptimisticLCPFromCompressedPath(Node.InnerNode node, int depth, int lcp, byte[] leafBytes) {
        // since there's more compressed path left
        // we need to "bring up" more of it what we can take
        node.prefixLen = node.prefixLen - lcp - 1;
        int end = Math.min(Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
        System.arraycopy(leafBytes, depth + 1, node.prefixKeys, 0, end);
    }

    static void removePessimisticLCPFromCompressedPath(Node.InnerNode node, int depth, int lcp) {
        if (node.prefixLen <= Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
            node.prefixLen = node.prefixLen - lcp - 1;
            System.arraycopy(node.prefixKeys, lcp + 1, node.prefixKeys, 0, node.prefixLen);
        } else {
            // since there's more compressed path left
            // we need to "bring up" more of it what we can take
            node.prefixLen = node.prefixLen - lcp - 1;
            byte[] leafBytes = getFirstEntry(node).getKeyBytes();
            int end = Math.min(Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT, node.prefixLen);
            System.arraycopy(leafBytes, depth + 1, node.prefixKeys, 0, end);
        }
    }

    private int matchCompressedPath(Node.InnerNode node, byte[] keyBytes, V value, int depth, Node.@Nullable InnerNode prevDepth) {
        int lcp = 0;
        int end = Math.min(keyBytes.length - depth, Math.min(node.prefixLen, Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT));
        // match pessimistic compressed path
        for (; lcp < end && keyBytes[depth] == node.prefixKeys[lcp]; lcp++, depth++) ;

        if (lcp == node.prefixLen) {
            if (depth == keyBytes.length && !node.hasLeaf()) { // key ended, it means it is a prefix
                Node.LeafNode<V> leafNode = new Node.LeafNode<>(keyBytes, value);
                node.setLeaf(leafNode);
                return -1;
            } else {
                return depth;
            }
        }

        Node.InnerNode newNode;
        if (lcp == Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT) {
            // match remaining optimistic path
            byte[] leafBytes = getFirstEntry(node).getKeyBytes();
            int leftToMatch = node.prefixLen - Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT;
            end = Math.min(keyBytes.length, depth + leftToMatch);
			/*
				match remaining optimistic path
				if we match entirely we return with new depth and caller can proceed with findChild (depth + lcp + 1)
				if we don't match entirely, then we split
			 */
            for (; depth < end && keyBytes[depth] == leafBytes[depth]; depth++, lcp++) ;
            if (lcp == node.prefixLen) {
                if (depth == keyBytes.length && !node.hasLeaf()) { // key ended, it means it is a prefix
                    Node.LeafNode<V> leafNode = new Node.LeafNode<>(keyBytes, value);
                    node.setLeaf(leafNode);
                    return -1;
                } else {
                    // matched entirely, but key is left
                    return depth;
                }
            } else {
                newNode = branchOutOptimistic(node, keyBytes, value, lcp, depth, leafBytes);
            }
        } else {
            newNode = branchOutPessimistic(node, keyBytes, value, lcp, depth);
        }
        // replace "this" node with newNode
        // initialDepth can be zero even if prefixLen is not zero.
        // the root node could have a prefix too, for example after insertions of
        // BAR, BAZ? prefix would be BA kept in the root node itself
        replace(depth - lcp, keyBytes, prevDepth, newNode);
        return -1; // we've already inserted the leaf node, caller needs to do nothing more
    }

    // called when lcp has become more than InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT
    static <V> Node.InnerNode branchOutOptimistic(Node.InnerNode node, byte[] keyBytes, V value, int lcp, int depth,
                                                  byte[] leafBytes) {
        int initialDepth = depth - lcp;
        Node.LeafNode<V> leafNode = new Node.LeafNode<>(keyBytes, value);

        // new node with updated prefix len, compressed path
        Node.Node4 branchOut = new Node.Node4();
        branchOut.prefixLen = lcp;
        // note: depth is the updated depth (initialDepth = depth - lcp)
        System.arraycopy(keyBytes, initialDepth, branchOut.prefixKeys, 0, Node.InnerNode.PESSIMISTIC_PATH_COMPRESSION_LIMIT);
        if (depth == keyBytes.length) {
            branchOut.setLeaf(leafNode);
        } else {
            branchOut.addChild(keyBytes[depth], leafNode);
        }
        branchOut.addChild(leafBytes[depth], node); // reusing "this" node

        // remove lcp common prefix key from "this" node
        removeOptimisticLCPFromCompressedPath(node, depth, lcp, leafBytes);
        return branchOut;
    }

    static <V> Node.InnerNode branchOutPessimistic(Node.InnerNode node, byte[] keyBytes, V value, int lcp, int depth) {
        int initialDepth = depth - lcp;

        // create new lazy leaf node for unmatched key?
        Node.LeafNode<V> leafNode = new Node.LeafNode<>(keyBytes, value);

        // new node with updated prefix len, compressed path
        Node.Node4 branchOut = new Node.Node4();
        branchOut.prefixLen = lcp;
        // note: depth is the updated depth (initialDepth = depth - lcp)
        System.arraycopy(keyBytes, initialDepth, branchOut.prefixKeys, 0, lcp);
        if (depth == keyBytes.length) { // key ended it means it is a prefix
            branchOut.setLeaf(leafNode);
        } else {
            branchOut.addChild(keyBytes[depth], leafNode);
        }
        branchOut.addChild(node.prefixKeys[lcp], node); // reusing "this" node

        // remove lcp common prefix key from "this" node
        removePessimisticLCPFromCompressedPath(node, depth, lcp);
        return branchOut;
    }

    @SuppressWarnings("unchecked")
    private static <V> Node.LeafNode<V> getFirstEntry(Node startFrom) {
        Node node = startFrom;
        Node next = node.firstOrLeaf();
        while (next != null) {
            node = next;
            next = node.firstOrLeaf();
        }
        return (Node.LeafNode<V>) node;
    }

    public int size() {
        return size;
    }

    abstract static class Node {
        // 2^7 = 128
        static final int BYTE_SHIFT = 1 << Byte.SIZE - 1;

        /**
         * For Node4, Node16 to interpret every byte as unsigned when storing partial keys.
         * Node 48, Node256 simply use {@link Byte#toUnsignedInt(byte)}
         * to index into their key arrays.
         */
        static byte unsigned(byte b) {
            return (byte) (b ^ BYTE_SHIFT);
        }

        // passed b must have been interpreted as unsigned already
        // this is the reverse of unsigned
        static byte signed(byte b) {
            return unsigned(b);
        }

        abstract @Nullable Node first();

        abstract @Nullable Node firstOrLeaf();

        abstract Node copy();

        static abstract class InnerNode extends Node {
            static final int PESSIMISTIC_PATH_COMPRESSION_LIMIT = 8;
            final byte[] prefixKeys = new byte[PESSIMISTIC_PATH_COMPRESSION_LIMIT];
            int prefixLen;
            short noOfChildren;
            final Node @Nullable[] child;

            InnerNode(int size) {
                child = new Node[size + 1];
            }

            InnerNode(InnerNode node, int size) {
                super();
                child = new Node[size + 1];
                this.noOfChildren = node.noOfChildren;
                this.prefixLen = node.prefixLen;
                System.arraycopy(node.prefixKeys, 0, this.prefixKeys, 0, PESSIMISTIC_PATH_COMPRESSION_LIMIT);
                LeafNode<?> leaf = node.getLeaf();
                if (leaf != null) {
                    setLeaf(leaf);
                }
            }

            public InnerNode(InnerNode from) {
                System.arraycopy(from.prefixKeys, 0, this.prefixKeys, 0, PESSIMISTIC_PATH_COMPRESSION_LIMIT);
                this.prefixLen = from.prefixLen;
                this.noOfChildren = from.noOfChildren;
                this.child = new Node[from.child.length];
                for (int i = 0; i < child.length; i++) {
                    Node fromChild = from.child[i];
                    this.child[i] = fromChild == null ? null : fromChild.copy();
                }
            }

            public void setLeaf(LeafNode<?> leaf) {
                child[child.length - 1] = leaf;
            }

            public boolean hasLeaf() {
                return child[child.length - 1] != null;
            }

            public @Nullable LeafNode<?> getLeaf() {
                return (LeafNode<?>) child[child.length - 1];
            }

            @Override
            public @Nullable Node firstOrLeaf() {
                return hasLeaf() ? getLeaf() : first();
            }

            Node[] getChild() {
                //noinspection NullableProblems
                return child;
            }

            public short size() {
                return noOfChildren;
            }

            abstract @Nullable Node findChild(byte partialKey);

            abstract void addChild(byte partialKey, Node child);

            abstract void replace(byte partialKey, Node newChild);

            abstract InnerNode grow();

            abstract boolean isFull();
        }

        static class LeafNode<V> extends Node {
            private V value;

            // we have to save the keyBytes, because leaves are lazy expanded at times
            private final byte[] keyBytes;

            LeafNode(byte[] keyBytes, V value) {
                this.value = value;
                // defensive copy
                this.keyBytes = Arrays.copyOf(keyBytes, keyBytes.length);
            }

            @Override
            Node copy() {
                return new LeafNode<>(keyBytes, value);
            }

            public void setValue(V value) {
                this.value = value;
            }

            public V getValue() {
                return value;
            }

            byte[] getKeyBytes() {
                return keyBytes;
            }

            @Override
            public @Nullable Node first() {
                return null;
            }

            @Override
            public @Nullable Node firstOrLeaf() {
                return null;
            }

            @Override
            public String toString() {
                return Arrays.toString(keyBytes) + "=" + value;
            }
        }

        static class Node4 extends InnerNode {

            static final int NODE_SIZE = 4;

            // each array element would contain the partial byte key to match
            // if key matches then take up the same index from the child pointer array
            private final byte[] keys = new byte[NODE_SIZE];

            Node4() {
                super(NODE_SIZE);
            }

            private Node4(Node4 node4) {
                super(node4);
            }

            @Override
            Node copy() {
                return new Node4(this);
            }

            @Override
            public @Nullable Node findChild(byte partialKey) {
                partialKey = unsigned(partialKey);
                // paper does simple loop over because it's a tiny array of size 4
                for (int i = 0; i < noOfChildren; i++) {
                    if (keys[i] == partialKey) {
                        return child[i];
                    }
                }
                return null;
            }

            @Override
            public void addChild(byte partialKey, Node child) {
                byte unsignedPartialKey = unsigned(partialKey);
                // shift elements from this point to right by one place
                // noOfChildren here would never be == Node_SIZE (since we have isFull() check)
                int i = noOfChildren;
                for (; i > 0 && unsignedPartialKey < keys[i - 1]; i--) {
                    keys[i] = keys[i - 1];
                    this.child[i] = this.child[i - 1];
                }
                keys[i] = unsignedPartialKey;
                this.child[i] = child;
                noOfChildren++;
            }

            @Override
            public void replace(byte partialKey, Node newChild) {
                byte unsignedPartialKey = unsigned(partialKey);

                int index = 0;
                for (; index < noOfChildren; index++) {
                    if (keys[index] == unsignedPartialKey) {
                        break;
                    }
                }
                // replace will be called from in a state where you know partialKey entry surely exists
                child[index] = newChild;
            }

            @Override
            public InnerNode grow() {
                // grow from Node4 to Node16
                return new Node16(this);
            }

            @Override
            public @Nullable Node first() {
                return child[0];
            }

            @Override
            public boolean isFull() {
                return noOfChildren == NODE_SIZE;
            }

            byte[] getKeys() {
                return keys;
            }
        }

        static class Node16 extends InnerNode {
            static final int NODE_SIZE = 16;
            private final byte[] keys = new byte[NODE_SIZE];

            Node16(Node4 node) {
                super(node, NODE_SIZE);
                byte[] keys = node.getKeys();
                Node[] child = node.getChild();
                System.arraycopy(keys, 0, this.keys, 0, node.noOfChildren);
                System.arraycopy(child, 0, this.child, 0, node.noOfChildren);
            }

            private Node16(Node16 from) {
                super(from);
                System.arraycopy(from.keys, 0, this.keys, 0, NODE_SIZE);
            }

            @Override
            Node copy() {
                return new Node16(this);
            }

            @Override
            public @Nullable Node findChild(byte partialKey) {
                // TODO: use simple loop to see if -XX:+SuperWord applies SIMD JVM instrinsics
                partialKey = unsigned(partialKey);
                for (int i = 0; i < noOfChildren; i++) {
                    if (keys[i] == partialKey) {
                        return child[i];
                    }
                }
                return null;
            }

            @Override
            public void addChild(byte partialKey, Node child) {
                byte unsignedPartialKey = unsigned(partialKey);

                int index = Arrays.binarySearch(keys, 0, noOfChildren, unsignedPartialKey);
                // the partialKey should not exist
                int insertionPoint = -(index + 1);
                // shift elements from this point to right by one place
                for (int i = noOfChildren; i > insertionPoint; i--) {
                    keys[i] = keys[i - 1];
                    this.child[i] = this.child[i - 1];
                }
                keys[insertionPoint] = unsignedPartialKey;
                this.child[insertionPoint] = child;
                noOfChildren++;
            }

            @Override
            public void replace(byte partialKey, Node newChild) {
                byte unsignedPartialKey = unsigned(partialKey);
                int index = Arrays.binarySearch(keys, 0, noOfChildren, unsignedPartialKey);
                child[index] = newChild;
            }

            @Override
            public InnerNode grow() {
                return new Node48(this);
            }

            @Override
            public @Nullable Node first() {
                return child[0];
            }

            @Override
            public boolean isFull() {
                return noOfChildren == NODE_SIZE;
            }

            byte[] getKeys() {
                return keys;
            }
        }

        static class Node48 extends InnerNode {
            static final int NODE_SIZE = 48;
            static final int KEY_INDEX_SIZE = 256;

            // for partial keys of one byte size, you index directly into this array to find the
            // array index of the child pointer array
            // the index value can only be between 0 to 47 (to index into the child pointer array)
            private final byte[] keyIndex = new byte[KEY_INDEX_SIZE];

            // so that when you use the partial key to index into keyIndex
            // and you see a -1, you know there's no mapping for this key
            static final byte ABSENT = -1;

            Node48(Node16 node) {
                super(node, NODE_SIZE);

                Arrays.fill(keyIndex, ABSENT);

                byte[] keys = node.getKeys();
                Node[] child = node.getChild();

                for (int i = 0; i < Node16.NODE_SIZE; i++) {
                    byte key = signed(keys[i]);
                    int index = Byte.toUnsignedInt(key);
                    keyIndex[index] = (byte) i;
                    this.child[i] = child[i];
                }
            }

            private Node48(Node48 from) {
                super(from);
                System.arraycopy(from.keyIndex, 0, this.keyIndex, 0, KEY_INDEX_SIZE);
            }

            @Override
            Node copy() {
                return new Node48(this);
            }

            @Override
            public @Nullable Node findChild(byte partialKey) {
                byte index = keyIndex[Byte.toUnsignedInt(partialKey)];
                if (index == ABSENT) {
                    return null;
                }

                return child[index];
            }

            @Override
            public void addChild(byte partialKey, Node child) {
                int index = Byte.toUnsignedInt(partialKey);
                // find a null place, left fragmented by a removeChild or has always been null
                byte insertPosition = 0;
                for (; this.child[insertPosition] != null && insertPosition < NODE_SIZE; insertPosition++) ;

                this.child[insertPosition] = child;
                keyIndex[index] = insertPosition;
                noOfChildren++;
            }

            @Override
            public void replace(byte partialKey, Node newChild) {
                byte index = keyIndex[Byte.toUnsignedInt(partialKey)];
                child[index] = newChild;
            }

            @Override
            public InnerNode grow() {
                return new Node256(this);
            }

            @Override
            public @Nullable Node first() {
                int i = 0;
                while (keyIndex[i] == ABSENT) i++;
                return child[keyIndex[i]];
            }

            @Override
            public boolean isFull() {
                return noOfChildren == NODE_SIZE;
            }


            byte[] getKeyIndex() {
                return keyIndex;
            }
        }

        static class Node256 extends InnerNode {
            static final int NODE_SIZE = 256;

            Node256(Node48 node) {
                super(node, NODE_SIZE);

                byte[] keyIndex = node.getKeyIndex();
                Node[] child = node.getChild();

                for (int i = 0; i < Node48.KEY_INDEX_SIZE; i++) {
                    byte index = keyIndex[i];
                    if (index == Node48.ABSENT) {
                        continue;
                    }
                    // index is byte, but gets type promoted
                    // https://docs.oracle.com/javase/specs/jls/se7/html/jls-10.html#jls-10.4-120
                    this.child[i] = child[index];
                }
            }

            private Node256(Node256 from) {
                super(from);
            }

            @Override
            Node copy() {
                return new Node256(this);
            }

            @Override
            public @Nullable Node findChild(byte partialKey) {
                // We treat the 8 bits as unsigned int since we've got 256 slots
                int index = Byte.toUnsignedInt(partialKey);
                return child[index];
            }

            @Override
            public void addChild(byte partialKey, Node child) {
                // addChild would never be called on a full Node256
                // since the corresponding findChild for any byte key
                // would always find the byte since the Node is full.
                int index = Byte.toUnsignedInt(partialKey);
                this.child[index] = child;
                noOfChildren++;
            }

            @Override
            public void replace(byte partialKey, Node newChild) {
                int index = Byte.toUnsignedInt(partialKey);
                child[index] = newChild;
            }

            @Override
            public InnerNode grow() {
                throw new UnsupportedOperationException("Span of ART is 8 bits, so Node256 is the largest node type.");
            }

            @Override
            public @Nullable Node first() {
                int i = 0;
                while (child[i] == null) i++;
                return child[i];
            }

            @Override
            public boolean isFull() {
                return noOfChildren == NODE_SIZE;
            }
        }
    }
}
