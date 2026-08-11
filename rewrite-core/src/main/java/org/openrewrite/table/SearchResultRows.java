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
package org.openrewrite.table;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.emptyMap;

/**
 * Buffers the rows a search produces for one source file so each row can carry the line number
 * of its own match, which is known only once the file is printed. Call {@link #found} in place of
 * {@link SearchResult#found(Tree)} at each match, and {@link #insertRows} when the file has been
 * visited.
 */
public class SearchResultRows<R> {
    private final DataTable<R> table;

    /**
     * Keyed by the fence {@link PrintOutputCapture.MarkerPrinter#FENCED} prints for the marker that
     * locates the match, so that locating one is a lookup of a string this registered.
     */
    private final Map<String, Function<@Nullable Integer, R>> rows = new LinkedHashMap<>();

    private final Set<UUID> matched = new HashSet<>();

    public SearchResultRows(DataTable<R> table) {
        this.table = table;
    }

    /**
     * Marks {@code t} as a search result and buffers the row describing it, to be built once the
     * match's line number is known. Buffers nothing when this search already matched {@code t},
     * so that an element reached by two visit methods is reported once.
     */
    public <T extends Tree> T found(T t, Function<@Nullable Integer, R> row) {
        if (!matched.add(t.getId())) {
            return t;
        }
        Markers markers = t.getMarkers().add(new SearchResult(Tree.randomId(), null));
        // `Markers.add` keeps out a search result equal to one an earlier search left, and every
        // search result on an element fences in the same place, so any of them locates the match
        markers.findFirst(SearchResult.class)
                .ifPresent(located -> rows.put("{{" + located.getId() + "}}", row));
        return t.withMarkers(markers);
    }

    /**
     * Inserts every buffered row with the line its match starts on in {@code searched} as printed,
     * or with no line when the printer locates no marker for it, and empties the buffer.
     */
    public void insertRows(@Nullable Tree searched, ExecutionContext ctx) {
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Integer> lines = searched instanceof SourceFile ?
                lineNumbers((SourceFile) searched, rows.keySet()) : emptyMap();
        for (Map.Entry<String, Function<@Nullable Integer, R>> row : rows.entrySet()) {
            table.insertRow(ctx, row.getValue().apply(lines.get(row.getKey())));
        }
        rows.clear();
        matched.clear();
    }

    /**
     * The 1-based line each of {@code fences} begins on, keyed by fence.
     */
    private static Map<String, Integer> lineNumbers(SourceFile searched, Set<String> fences) {
        LineCounter counter = new LineCounter(fences);
        try {
            searched.printAll(counter);
        } catch (RuntimeException ignored) {
            return emptyMap();
        }
        return counter.lines;
    }

    /**
     * Counts lines as a file prints, noting the line each of the fences it is looking for opens on.
     * {@link PrintOutputCapture.MarkerPrinter#FENCED} appends each fence on its own after the
     * marked syntax's prefix, and printers only ever append to a capture, so a fence can be placed
     * as it arrives and the printed text stays unbuilt.
     */
    private static class LineCounter extends PrintOutputCapture<Integer> {
        private static final int FENCE_LENGTH = "{{".length() + 36 + "}}".length();

        private final Set<String> fences;
        final Map<String, Integer> lines = new HashMap<>();
        private int line = 1;

        LineCounter(Set<String> fences) {
            super(0, MarkerPrinter.FENCED);
            this.fences = fences;
        }

        @Override
        public PrintOutputCapture<Integer> append(@Nullable String text) {
            if (text == null || text.isEmpty()) {
                return this;
            }
            // Length and brace rule out all but a fence before hashing the string
            if (text.length() == FENCE_LENGTH && text.charAt(0) == '{' && fences.contains(text)) {
                // Syntax is fenced on both sides; the opening fence is the one that locates it.
                lines.putIfAbsent(text, line);
                return this;
            }
            for (int i = text.indexOf('\n'); i >= 0; i = text.indexOf('\n', i + 1)) {
                line++;
            }
            return this;
        }

        @Override
        public PrintOutputCapture<Integer> append(char c) {
            if (c == '\n') {
                line++;
            }
            return this;
        }
    }
}
