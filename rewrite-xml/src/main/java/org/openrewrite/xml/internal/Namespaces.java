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
package org.openrewrite.xml.internal;

import lombok.Value;
import org.openrewrite.internal.lang.NonNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Value
public class Namespaces implements Iterable<Map.Entry<String, String>> {

    Map<String, String> namespaces = new HashMap<>();

    public Namespaces() {
    }

    public Namespaces(String prefix, String uri) {
        this.namespaces.put(prefix, uri);
    }

    public Namespaces(Map<String, String> namespaces) {
        this.namespaces.putAll(namespaces);
    }

    public Namespaces add(String prefix, String uri) {
        Map<String, String> combinedNamespaces = new HashMap<>(namespaces);
        combinedNamespaces.put(prefix, uri);
        return new Namespaces(combinedNamespaces);
    }

    public Namespaces add(Map<String, String> namespaces) {
        Map<String, String> combinedNamespaces = new HashMap<>(this.namespaces);
        combinedNamespaces.putAll(namespaces);
        return new Namespaces(combinedNamespaces);
    }

    public Namespaces combine(Namespaces namespaces) {
        return add(namespaces.getNamespaces());
    }

    public String get(String prefix) {
        return namespaces.get(prefix);
    }

    public boolean containsPrefix(String prefix) {
        return namespaces.containsKey(prefix);
    }

    public boolean containsUri(String uri) {
        return namespaces.containsValue(uri);
    }

    @NonNull
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return new NamespacesIterator(this.namespaces);
    }

    private static class NamespacesIterator implements Iterator<Map.Entry<String, String>> {

        private final Iterator<Map.Entry<String, String>> entriesIterator;

        public NamespacesIterator(Map<String, String> map) {
            this.entriesIterator = map.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return entriesIterator.hasNext();
        }

        @Override
        public Map.Entry<String, String> next() {
            return entriesIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removal not supported");
        }
    }
}
