/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.properties.tree;

import lombok.*;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.internal.PropertiesPrinter;
import org.openrewrite.properties.markers.LineContinuation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public interface Properties extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptProperties(v.adapt(PropertiesVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(PropertiesVisitor.class);
    }

    @Nullable
    default <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    String getPrefix();

    Properties withPrefix(String prefix);

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class File implements Properties, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        Path sourcePath;

        List<Content> content;
        String eof;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        boolean charsetBomMarked;

        @Nullable
        FileAttributes fileAttributes;

        @Nullable
        Checksum checksum;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitFile(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new PropertiesPrinter<>();
        }
    }

    interface Content extends Properties {
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Entry implements Content {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefix;

        @With
        Markers markers;

        String key;

        public Entry withKey(String key) {
            if (this.key.equals(key)) {
                return this;
            }

            Optional<LineContinuation> entryContinuationOptional = markers.findFirst(LineContinuation.class);
            String cleaned = key;
            Markers maybeUpdate = markers;
            if (entryContinuationOptional.isPresent()) {
                LineContinuation lineContinuation = entryContinuationOptional.get();
                int beginning = prefix.length();
                int end = beginning + this.key.length();

                List<Map.Entry<Integer, String>> entries = lineContinuation.getContinuationMap().entrySet()
                        .stream()
                        .filter(e -> e.getKey() >= beginning && e.getKey() <= end)
                        .collect(Collectors.toList());

                int continuationLength = 0;
                for (Map.Entry<Integer, String> entry : entries) {
                    continuationLength += entry.getValue().length();
                }

                Map<Integer, String> continuations = new HashMap<>();
                if (LineContinuationUtil.containsEscapedContinuation(key)) {
                    cleaned = LineContinuationUtil.extractValue(cleaned, beginning, continuations);
                }

                for (Map.Entry<Integer, String> entry : entries) {
                    lineContinuation.getContinuationMap().remove(entry.getKey());
                }

                int newLength = this.key.length() >= cleaned.length() ? this.key.length() - cleaned.length() : cleaned.length() - this.key.length();
                int diff = continuationLength - (continuationLength - newLength);
                Map<Integer, String> newContinuations = new HashMap<>();
                if (diff != 0) {
                    for (Integer integer : lineContinuation.getContinuationMap().keySet()) {
                        String value = lineContinuation.getContinuationMap().get(integer);
                        newContinuations.put(integer + diff, value);
                    }
                }

                if (!continuations.isEmpty()) {
                    if (diff == 0) {
                        newContinuations.putAll(lineContinuation.getContinuationMap());
                    }
                    newContinuations.putAll(continuations);
                }

                if (!newContinuations.isEmpty()) {
                    maybeUpdate = markers.removeByType(LineContinuation.class);
                    lineContinuation = lineContinuation.withContinuationMap(newContinuations);
                    maybeUpdate = maybeUpdate.addIfAbsent(lineContinuation);
                }
            }

            return new Entry(id, prefix, maybeUpdate, cleaned, beforeEquals, value);
        }

        String beforeEquals;

        public Entry withBeforeEquals(String beforeEquals) {
            if (this.beforeEquals.equals(beforeEquals)) {
                return this;
            }

            Optional<LineContinuation> entryContinuationOptional = markers.findFirst(LineContinuation.class);
            String cleaned = beforeEquals;
            Markers maybeUpdate = markers;
            if (entryContinuationOptional.isPresent()) {
                LineContinuation lineContinuation = entryContinuationOptional.get();
                int beginning = prefix.length() + key.length();
                int end = beginning + this.beforeEquals.length();

                List<Map.Entry<Integer, String>> entries = lineContinuation.getContinuationMap().entrySet()
                        .stream()
                        .filter(e -> e.getKey() >= beginning && e.getKey() <= end)
                        .collect(Collectors.toList());

                int continuationLength = 0;
                for (Map.Entry<Integer, String> entry : entries) {
                    continuationLength += entry.getValue().length();
                }

                Map<Integer, String> continuations = new HashMap<>();
                if (LineContinuationUtil.containsEscapedContinuation(beforeEquals)) {
                    cleaned = LineContinuationUtil.extractValue(cleaned, beginning, continuations);
                }

                for (Map.Entry<Integer, String> entry : entries) {
                    lineContinuation.getContinuationMap().remove(entry.getKey());
                }

                int newLength = this.beforeEquals.length() >= cleaned.length() ? this.beforeEquals.length() - cleaned.length() : cleaned.length() - this.beforeEquals.length();
                int diff = continuationLength - (continuationLength - newLength);
                Map<Integer, String> newContinuations = new HashMap<>();
                if (diff != 0) {
                    for (Integer integer : lineContinuation.getContinuationMap().keySet()) {
                        String value = lineContinuation.getContinuationMap().get(integer);
                        newContinuations.put(integer + diff, value);
                    }
                }

                if (!continuations.isEmpty()) {
                    if (diff == 0) {
                        newContinuations.putAll(lineContinuation.getContinuationMap());
                    }
                    newContinuations.putAll(continuations);
                }

                if (!newContinuations.isEmpty()) {
                    maybeUpdate = markers.removeByType(LineContinuation.class);
                    lineContinuation = lineContinuation.withContinuationMap(newContinuations);
                    maybeUpdate = maybeUpdate.addIfAbsent(lineContinuation);
                }
            }
            return new Entry(id, prefix, maybeUpdate, key, cleaned, value);
        }

        @With
        Value value;

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitEntry(this, p);
        }
    }

    /**
     * Note that this class cannot easily be made to implement {@link Properties} like it should,
     * because existing serialized ASTs will not have a {@link com.fasterxml.jackson.annotation.JsonIdentityInfo}
     * reference to deserialize into the type.
     */
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Value {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String prefix;

        @With
        Markers markers;

        String text;

        public Value withText(String text) {
            if (this.text.equals(text)) {
                return this;
            }

            Optional<LineContinuation> entryContinuationOptional = markers.findFirst(LineContinuation.class);
            String cleaned = text;
            Markers maybeUpdate = markers;
            if (entryContinuationOptional.isPresent()) {
                LineContinuation lineContinuation = entryContinuationOptional.get();
                int beginning = prefix.length();
                int end = beginning + this.text.length();

                List<Map.Entry<Integer, String>> entries = lineContinuation.getContinuationMap().entrySet()
                        .stream()
                        .filter(e -> e.getKey() >= beginning && e.getKey() <= end)
                        .collect(Collectors.toList());

                int continuationLength = 0;
                for (Map.Entry<Integer, String> entry : entries) {
                    continuationLength += entry.getValue().length();
                }

                Map<Integer, String> continuations = new HashMap<>();
                if (LineContinuationUtil.containsEscapedContinuation(text)) {
                    cleaned = LineContinuationUtil.extractValue(cleaned, beginning, continuations);
                }

                for (Map.Entry<Integer, String> entry : entries) {
                    lineContinuation.getContinuationMap().remove(entry.getKey());
                }

                int newLength = this.text.length() >= cleaned.length() ? this.text.length() - cleaned.length() : cleaned.length() - this.text.length();
                int diff = continuationLength - (continuationLength - newLength);
                Map<Integer, String> newContinuations = new HashMap<>();
                if (diff != 0) {
                    for (Integer integer : lineContinuation.getContinuationMap().keySet()) {
                        String value = lineContinuation.getContinuationMap().get(integer);
                        newContinuations.put(integer + diff, value);
                    }
                }

                if (!continuations.isEmpty()) {
                    if (diff == 0) {
                        newContinuations.putAll(lineContinuation.getContinuationMap());
                    }
                    newContinuations.putAll(continuations);
                }

                if (!newContinuations.isEmpty()) {
                    maybeUpdate = markers.removeByType(LineContinuation.class);
                    lineContinuation = lineContinuation.withContinuationMap(newContinuations);
                    maybeUpdate = maybeUpdate.addIfAbsent(lineContinuation);
                } else if (lineContinuation.getContinuationMap().isEmpty()) {
                    maybeUpdate = markers.removeByType(LineContinuation.class);
                }
            }
            return new Value(id, prefix, maybeUpdate, text);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Comment implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        String message;

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }
    }

    class LineContinuationUtil {
        static boolean containsEscapedContinuation(String value) {
            return value.contains("\\\n") || value.contains("\\\r\n");
        }

        static String extractValue(String value, int position, Map<Integer, String> continuations) {
            StringBuilder buff = new StringBuilder();
            StringBuilder continuation = new StringBuilder();

            boolean inContinuation = false;
            char prev = '$';

            char[] charArray = value.toCharArray();
            for (char c : charArray) {
                if (inContinuation) {
                    if (Character.isWhitespace(c) && !(c == '\n' && prev == '\n')) {
                        continuation.append(c);
                        prev = c;
                        continue;
                    } else {
                        continuations.put(position + buff.length(), continuation.toString());
                        continuation.setLength(0);
                        inContinuation = false;
                    }
                }

                if (c == '\n') {
                    if (prev == '\\') {
                        inContinuation = true;

                        // Move the escape character to the continuation.
                        buff.deleteCharAt(buff.length() - 1);
                        continuation.append("\\");

                        continuation.append(c);
                    }
                } else {
                    buff.append(c);
                }
                prev = c;
            }

            if (inContinuation) {
                continuations.put(position + buff.length(), continuation.toString());
                continuation.setLength(0);
            }

            return buff.toString();
        }
    }
}
