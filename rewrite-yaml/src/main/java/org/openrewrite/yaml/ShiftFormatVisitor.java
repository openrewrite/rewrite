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
 * Shifts the indentation of every element within {@code scope} by a fixed amount.
 * A positive {@code shift} adds whitespace (moves right); a negative {@code shift}
 * removes whitespace (moves left). The relative indentation between nested elements
 * is preserved, so the whole subtree simply moves as a block.
 */
public class ShiftFormatVisitor<P> extends YamlIsoVisitor<P> {
    private final Yaml scope;
    private final int shift;

    public ShiftFormatVisitor(Yaml scope, int shift) {
        this.scope = scope;
        this.shift = shift;
    }

    @Override
    public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        Yaml.Sequence.Entry e = super.visitSequenceEntry(entry, p);
        if (getCursor().isScopeInPath(scope) && e.isDash() && e.getPrefix().contains("\n")) {
            e = e.withPrefix(shiftPrefix(e.getPrefix()));
        }
        return e;
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
        if (getCursor().isScopeInPath(scope) && e.getPrefix().contains("\n")) {
            e = e.withPrefix(shiftPrefix(e.getPrefix()));
        }
        return e;
    }

    @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
    private String shiftPrefix(String prefix) {
        return String.join("\n", ListUtils.map(Arrays.asList(prefix.split("\\n")), (index, s) -> {
            // The segment before the first line break stays on the previous element's line
            // (it may hold a trailing inline comment), so it is never re-indented.
            if (index == 0) {
                return s;
            }
            if (shift >= 0) {
                return StringUtils.repeat(" ", shift) + s;
            }
            // Only remove whitespace when there is enough available on this line, mirroring the
            // safeguards in ShiftFormatLeftVisitor so comments are not mangled.
            int amount = -shift;
            if (StringUtils.indexOfNonWhitespace(s) >= amount || (StringUtils.indexOfNonWhitespace(s) == -1 && s.length() >= amount)) {
                return s.substring(amount);
            }
            return s;
        }));
    }
}
