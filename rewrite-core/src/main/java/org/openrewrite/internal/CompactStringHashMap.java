/*
 * Copyright 2025 the original author or authors.
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

import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

import static org.openrewrite.internal.StringUtils.getBytes;

/**
 * A memory-efficient hash map implementation that stores String keys as byte arrays.
 * Uses open addressing with quadratic probing for collision resolution.
 * Does not implement the Map interface to minimize overhead due to `Map.Entry` API.
 *
 * @param <V> The type of values stored in the map
 */
public class CompactStringHashMap<V> {
    private static final float DEFAULT_LOAD_FACTOR = 0.8f;
    private static final int DEFAULT_INITIAL_CAPACITY = 256;

    // Marker for deleted entries
    private static final byte[] TOMBSTONE = new byte[0];

    private final XXHash32 hasher = XXHashFactory.fastestInstance().hash32();

    private byte[][] keys;
    private @Nullable Object[] values;

    private int size;
    private int threshold;
    private final float loadFactor;

    public CompactStringHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a ByteHashMap with specified initial capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     */
    public CompactStringHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity cannot be negative: " + initialCapacity);
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor must be positive: " + loadFactor);
        }

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }

        this.loadFactor = loadFactor;
        this.threshold = (int) (capacity * loadFactor);
        this.keys = new byte[capacity][];
        this.values = new Object[capacity];
        this.size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Inserts a mapping from the specified key to the specified value.
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param key   the key to insert
     * @param value the value to insert
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public @Nullable V insert(String key, V value) {
        byte[] keyBytes = getBytes(key);
        return insert0(keyBytes, value);
    }

    /**
     * Inserts a mapping using a byte array key directly.
     *
     * @param keyBytes the key as a byte array
     * @param value    the value to insert
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public @Nullable V insert(byte[] keyBytes, V value) {
        return insert0(keyBytes, value);
    }

    @SuppressWarnings("unchecked")
    private @Nullable V insert0(byte[] keyBytes, @Nullable V value) {
        if (size >= threshold) {
            resize();
        }

        int hash = hash(keyBytes);
        int capacity = keys.length;
        int tombstoneIndex = -1;

        for (int probe = 0; probe < capacity; probe++) {
            // Calculate index using quadratic formula: (hash + probe*(probe+1)/2) % capacity
            int index = (int) (hash + (long) probe * (probe + 1) / 2) & (capacity - 1);

            if (keys[index] == null) {
                // Found empty slot
                int insertionIndex = (tombstoneIndex != -1) ? tombstoneIndex : index;
                keys[insertionIndex] = keyBytes;
                values[insertionIndex] = value;
                size++;
                return null; // New key inserted
            }

            if (keys[index] == TOMBSTONE) {
                // Remember the first tombstone for possible reuse
                if (tombstoneIndex == -1) {
                    tombstoneIndex = index;
                }
            } else if (keyEquals(keys[index], keyBytes)) {
                // Key found, replace value
                V oldValue = (V) values[index];
                // Even if we found a tombstone earlier, update in place where key was found
                values[index] = value;
                return oldValue;
            }
            // Collision, continue to next probe step
        }

        // If loop finishes, we should have found an empty slot or reused a tombstone if load factor < 1
        // If a tombstone was found, insert there. This path might be hit if the table is full of tombstones.
        if (tombstoneIndex != -1) {
            keys[tombstoneIndex] = keyBytes;
            values[tombstoneIndex] = value;
            size++;
            return null;
        }

        // Should not be reached if load factor < 1 and resize works
        throw new IllegalStateException("Hash table full or probing failed unexpectedly.");
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or null if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if not found
     */
    public @Nullable V search(String key) {
        byte[] keyBytes = getBytes(key);
        return search0(keyBytes);
    }

    /**
     * Searches using a byte array key directly.
     *
     * @param keyBytes the key as a byte array
     * @return the value associated with the key, or null if not found
     */
    public @Nullable V search(byte[] keyBytes) {
        return search0(keyBytes);
    }

    @SuppressWarnings("unchecked")
    private @Nullable V search0(byte[] keyBytes) {
        int hash = hash(keyBytes);
        int capacity = keys.length;

        for (int probe = 0; probe < capacity; probe++) {
            // Calculate index using quadratic formula
            int index = (int) (hash + (long) probe * (probe + 1) / 2) & (capacity - 1);

            if (keys[index] == null) {
                return null;
            }

            if (keys[index] != TOMBSTONE && keyEquals(keys[index], keyBytes)) {
                return (V) values[index];
            }
            // Collision or Tombstone, continue probing
        }

        return null;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with the key, or null if not found
     */
    public @Nullable V remove(String key) {
        byte[] keyBytes = getBytes(key);

        return remove0(keyBytes);
    }

    @SuppressWarnings("unchecked")
    private @Nullable V remove0(byte[] keyBytes) {
        int hash = hash(keyBytes);
        int capacity = keys.length;

        for (int probe = 0; probe < capacity; probe++) {
            // Calculate index using quadratic formula
            int index = (int) (hash + (long)probe * (probe + 1) / 2) & (capacity - 1);

            if (keys[index] == null) {
                // Found empty slot, key cannot exist further
                return null;
            }

            if (keys[index] != TOMBSTONE && keyEquals(keys[index], keyBytes)) {
                // Key found, mark as deleted
                V oldValue = (V) values[index];
                keys[index] = TOMBSTONE; // Mark with tombstone
                values[index] = null;   // Clear value reference
                size--;
                // Optional: Add tombstone counting logic here
                return oldValue;
            }
            // Collision or Tombstone, continue probing
        }

        return null;
    }

    public void clear() {
        keys = new byte[keys.length][];
        values = new Object[values.length];
        size = 0;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = keys.length * 2;
        byte[][] oldKeys = keys;
        @Nullable Object[] oldValues = values;

        keys = new byte[newCapacity][];
        values = new Object[newCapacity];
        threshold = (int) (newCapacity * loadFactor);

        // Temporary set size to 0 as entries will be re-inserted
        size = 0;

        // Re-insert all existing entries
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null && oldKeys[i] != TOMBSTONE) {
                insert0(oldKeys[i], (V) oldValues[i]);
            }
        }
    }

    private int hash(byte[] bytes) {
        return hasher.hash(bytes, 0, bytes.length, 0);
    }

    private boolean keyEquals(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}
