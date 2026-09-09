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
package org.openrewrite.yaml;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Arrays;

/**
 * Shifts the indentation of every element within {@code scope} by a fixed amount, preserving the
 * relative indentation between nested elements so the whole subtree moves as a block. A positive
 * {@code shift} adds whitespace (moves right); a negative {@code shift} removes whitespace (moves
 * left).
 * <p>
 * Use {@link #toIndent(Yaml, int)} when the desired absolute indentation of {@code scope} is known
 * rather than the relative offset.
 */
public class ShiftIndentVisitor<P> extends YamlIsoVisitor<P> {
    private final Yaml scope;
    private final int shift;

    public ShiftIndentVisitor(Yaml scope, int shift) {
        this.scope = scope;
        this.shift = shift;
    }

    /**
     * Creates a visitor that moves {@code scope} (and its nested content) so that {@code scope}
     * itself lands at {@code targetColumn}. Leaves the tree unchanged when {@code scope}'s current
     * indentation cannot be determined (it does not begin its own line) or already matches.
     */
    public static <P> ShiftIndentVisitor<P> toIndent(Yaml scope, int targetColumn) {
        int currentIndent = currentIndent(scope.getPrefix());
        return new ShiftIndentVisitor<>(scope, currentIndent < 0 ? 0 : targetColumn - currentIndent);
    }

    @Override
    public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        Yaml.Sequence.Entry e = super.visitSequenceEntry(entry, p);
        if (shift != 0 && getCursor().isScopeInPath(scope) && e.isDash() && e.getPrefix().contains("\n")) {
            e = e.withPrefix(shiftPrefix(e.getPrefix()));
        }
        return e;
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
        if (shift != 0 && getCursor().isScopeInPath(scope) && e.getPrefix().contains("\n")) {
            e = e.withPrefix(shiftPrefix(e.getPrefix()));
        }
        return e;
    }

    @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
    private String shiftPrefix(String prefix) {
        // Split with a negative limit so a trailing line break (e.g. a bare "\n" prefix on a
        // zero-indent entry) is preserved rather than dropped.
        return String.join("\n", ListUtils.map(Arrays.asList(prefix.split("\\n", -1)), (index, s) -> {
            // The segment before the first line break stays on the previous element's line
            // (it may hold a trailing inline comment), so it is never re-indented.
            if (index == 0) {
                return s;
            }
            if (shift >= 0) {
                return StringUtils.repeat(" ", shift) + s;
            }
            // Only remove whitespace when there is enough available on this line, so comments and
            // content are never mangled.
            int amount = -shift;
            if (StringUtils.indexOfNonWhitespace(s) >= amount || (StringUtils.indexOfNonWhitespace(s) == -1 && s.length() >= amount)) {
                return s.substring(amount);
            }
            return s;
        }));
    }

    /**
     * The number of whitespace characters after the last line break of a prefix, or {@code -1} when
     * the prefix has no line break (i.e. the element is not on its own line).
     */
    private static int currentIndent(String prefix) {
        int idx = Math.max(prefix.lastIndexOf('\n'), prefix.lastIndexOf('\r'));
        return idx < 0 ? -1 : prefix.length() - idx - 1;
    }
}
