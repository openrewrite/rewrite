/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.internal.parity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Structural diff of two {@link ResolutionSnapshot}s as an ordered list of
 * {@code (jsonPath, left, right)} entries. Masks (loaded from {@code parity/masks.txt}) suppress
 * entries by path prefix; every mask must carry a ledger id.
 */
@Value
public class ResolutionDiff {
    private static final String MISSING = "<missing>";

    @Value
    public static class Entry {
        String path;
        String left;
        String right;
    }

    List<Entry> entries;

    public static ResolutionDiff between(ResolutionSnapshot left, ResolutionSnapshot right) {
        List<Entry> entries = new ArrayList<>();
        diff("$", left.getJson(), right.getJson(), entries);
        return new ResolutionDiff(entries);
    }

    public ResolutionDiff masked(List<SnapshotNormalizer.Mask> masks) {
        List<Entry> unmasked = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            boolean masked = false;
            for (SnapshotNormalizer.Mask mask : masks) {
                if (matches(mask.getJsonPathPrefix(), entry)) {
                    masked = true;
                    break;
                }
            }
            if (!masked) {
                unmasked.add(entry);
            }
        }
        return new ResolutionDiff(unmasked);
    }

    // A plain prefix masks by path; a `metaversion:<prefix>` mask additionally requires the engine (right) value to be a
    // metaversion literal (latest/release) — narrow enough to suppress only a legacy-resolves/engine-keeps-literal flip
    // (a real regression never keeps a metaversion literal), so it never hides a genuine version divergence.
    private static boolean matches(String jsonPathPrefix, Entry entry) {
        if (jsonPathPrefix.startsWith("metaversion:")) {
            String prefix = jsonPathPrefix.substring("metaversion:".length());
            return entry.getPath().startsWith(prefix) && isMetaversion(entry.getRight());
        }
        return entry.getPath().startsWith(jsonPathPrefix);
    }

    private static boolean isMetaversion(String rendered) {
        String value = rendered;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return "latest".equalsIgnoreCase(value) || "release".equalsIgnoreCase(value);
    }

    private static void diff(String path, @Nullable JsonNode left, @Nullable JsonNode right, List<Entry> entries) {
        if (left == null || right == null) {
            if (left != right) {
                entries.add(new Entry(path, render(left), render(right)));
            }
            return;
        }
        if (left.isObject() && right.isObject()) {
            SortedSet<String> names = new TreeSet<>();
            left.fieldNames().forEachRemaining(names::add);
            right.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                diff(path + "." + name, left.get(name), right.get(name), entries);
            }
        } else if (left.isArray() && right.isArray()) {
            int max = Math.max(left.size(), right.size());
            for (int i = 0; i < max; i++) {
                diff(path + "[" + i + "]", left.get(i), right.get(i), entries);
            }
        } else if (!left.equals(right)) {
            entries.add(new Entry(path, render(left), render(right)));
        }
    }

    private static String render(@Nullable JsonNode node) {
        return node == null ? MISSING : node.toString();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public String render() {
        if (isEmpty()) {
            return "<no differences>";
        }
        StringBuilder s = new StringBuilder();
        for (Entry entry : entries) {
            s.append(entry.getPath()).append(": ").append(entry.getLeft()).append(" != ").append(entry.getRight()).append('\n');
        }
        return s.toString();
    }
}
