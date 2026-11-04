/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.bun;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of Zig's lib/std/hash_map.zig {@code HashMapUnmanaged} over {@code AutoContext(u64)}, 80% load (oven-sh/zig
 * c031cbebf5, the Zig of bun 1.3.10): bun.lock.zig writes trusted and patched dependencies in its slot order.
 */
final class TrustedDependencies<V> {

    private static final int MAX_LOAD_PERCENTAGE = 80;
    private static final int MINIMAL_CAPACITY = 8;

    private long[] keys = new long[0];
    private Object[] values = new Object[0];
    private byte[] metadata = new byte[0];
    private int size;
    private int available;

    int count() {
        return size;
    }

    private int capacity() {
        return metadata.length;
    }

    private static int capacityForSize(int size) {
        long newCap = (long) size * 100 / MAX_LOAD_PERCENTAGE + 1;
        int cap = 1;
        while (cap < newCap) {
            cap <<= 1;
        }
        return cap;
    }

    private int load() {
        int maxLoad = capacity() * MAX_LOAD_PERCENTAGE / 100;
        return maxLoad - available;
    }

    void ensureTotalCapacity(int newSize) {
        if (newSize > size) {
            growIfNeeded(newSize - size);
        }
    }

    private void growIfNeeded(int newCount) {
        if (newCount > available) {
            grow(capacityForSize(load() + newCount));
        }
    }

    private void grow(int newCapacity) {
        int newCap = Math.max(newCapacity, MINIMAL_CAPACITY);
        long[] oldKeys = keys;
        Object[] oldValues = values;
        byte[] oldMetadata = metadata;
        int oldSize = size;
        keys = new long[newCap];
        values = new Object[newCap];
        metadata = new byte[newCap];
        available = newCap * MAX_LOAD_PERCENTAGE / 100;
        size = 0;
        for (int i = 0; i < oldMetadata.length && size < oldSize; i++) {
            if (isUsed(oldMetadata[i])) {
                putAssumeCapacityNoClobber(oldKeys[i], oldValues[i]);
            }
        }
    }

    private static boolean isUsed(byte m) {
        return (m & 0x80) != 0;
    }

    private static byte fill(long hash) {
        return (byte) (0x80 | (int) (hash >>> 57));
    }

    private void putAssumeCapacityNoClobber(long key, Object value) {
        long hash = Wyhash.hashU64(key);
        int mask = capacity() - 1;
        int idx = (int) (hash & mask);
        while (isUsed(metadata[idx])) {
            idx = (idx + 1) & mask;
        }
        available--;
        metadata[idx] = fill(hash);
        keys[idx] = key;
        values[idx] = value;
        size++;
    }

    void put(long key, V value) {
        growIfNeeded(1);
        long hash = Wyhash.hashU64(key);
        int mask = capacity() - 1;
        byte fingerprint = fill(hash);
        int limit = capacity();
        int idx = (int) (hash & mask);
        while (metadata[idx] != 0 && limit != 0) {
            if (metadata[idx] == fingerprint && keys[idx] == key) {
                values[idx] = value;
                return;
            }
            limit--;
            idx = (idx + 1) & mask;
        }
        available--;
        metadata[idx] = fingerprint;
        keys[idx] = key;
        values[idx] = value;
        size++;
    }

    @SuppressWarnings("unchecked")
    List<V> values() {
        List<V> out = new ArrayList<>(size);
        for (int i = 0; i < metadata.length; i++) {
            if (isUsed(metadata[i])) {
                out.add((V) values[i]);
            }
        }
        return out;
    }
}
