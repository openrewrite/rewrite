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
package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;

public class WeakIdentityHashMap<K, V> implements Map<K, V> {

    private static class IdentityWeakReference<T> extends WeakReference<T> {
        private final int hash;

        public IdentityWeakReference(T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IdentityWeakReference)) return false;
            T t = this.get();
            @SuppressWarnings("unchecked")
            T u = ((IdentityWeakReference<T>) obj).get();
            return t != null && u != null && t == u;
        }
    }

    private final Map<IdentityWeakReference<K>, V> map = new HashMap<>();
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    private void expungeStaleEntries() {
        IdentityWeakReference<K> ref;
        while ((ref = (IdentityWeakReference<K>) queue.poll()) != null) {
            map.remove(ref);
        }
    }

    @Override
    public int size() {
        expungeStaleEntries();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        expungeStaleEntries();
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        expungeStaleEntries();
        @SuppressWarnings("unchecked")
        K k = (K) key;
        return map.containsKey(new IdentityWeakReference<>(k, queue));
    }

    @Override
    public boolean containsValue(Object value) {
        expungeStaleEntries();
        return map.containsValue(value);
    }

    @Override
    public @Nullable V get(Object key) {
        expungeStaleEntries();
        @SuppressWarnings("unchecked")
        K k = (K) key;
        return map.get(new IdentityWeakReference<>(k, queue));
    }

    @Override
    public @Nullable V put(K key, V value) {
        expungeStaleEntries();
        return map.put(new IdentityWeakReference<>(key, queue), value);
    }

    @Override
    public @Nullable V remove(Object key) {
        expungeStaleEntries();
        @SuppressWarnings("unchecked")
        K k = (K) key;
        return map.remove(new IdentityWeakReference<>(k, queue));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        expungeStaleEntries();
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
        while (queue.poll() != null) {
            // Clear the reference queue
        }
    }

    @Override
    public Set<K> keySet() {
        expungeStaleEntries();
        Set<K> keys = new HashSet<>();
        for (IdentityWeakReference<K> ref : map.keySet()) {
            K key = ref.get();
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    @Override
    public Collection<V> values() {
        expungeStaleEntries();
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        expungeStaleEntries();
        Set<Entry<K, V>> entries = new HashSet<>();
        for (Entry<IdentityWeakReference<K>, V> entry : map.entrySet()) {
            K key = entry.getKey().get();
            if (key != null) {
                entries.add(new AbstractMap.SimpleEntry<>(key, entry.getValue()));
            }
        }
        return entries;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        expungeStaleEntries();
        IdentityWeakReference<K> keyRef = new IdentityWeakReference<>(key, queue);
        return map.computeIfAbsent(keyRef, ref -> mappingFunction.apply(key));
    }
}