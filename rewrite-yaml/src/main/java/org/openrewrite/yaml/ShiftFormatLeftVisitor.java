/*
 * Copyright 2020 the original author or authors.
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

public class ShiftFormatLeftVisitor<P> extends YamlIsoVisitor<P> {
    private final Yaml scope;
    private final int shift;

    public ShiftFormatLeftVisitor(Yaml scope, int shift) {
        this.scope = scope;
        this.shift = shift;
    }

    @Override
    public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        Yaml.Sequence.Entry e = super.visitSequenceEntry(entry, p);
        if (getCursor().isScopeInPath(scope)) {
            if (e.isDash()) {
                e = e.withPrefix(shiftPrefix(e.getPrefix()));
            }
        }
        return e;
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
        if (getCursor().isScopeInPath(scope)) {
            if (e.getPrefix().contains("\n")) {
                e = e.withPrefix(shiftPrefix(e.getPrefix()));
            }
        }
        return e;
    }

    @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
    private String shiftPrefix(String prefix) {
        return String.join("\n", ListUtils.map(Arrays.asList(prefix.split("\\n")), (index, s) -> {
            // if the first line of "prefix" does not contain a newline character, it starts on the same line as an existing yaml object.
            // in that case, we just leave it alone. we do not want to left-shift a comment on the same line following the previous yaml object.
            if (index == 0 && !s.trim().isEmpty()) {
                return s;
                // Only remove the "shift"-amount of whitespace if there is enough available in the prefix on this line.
                // Specifically, we're trying to avoid comments in a prefix from being formatted in a way that mangles the yaml.
                // Otherwise, just leave this line be. It's worse to remove an overrun of whitespace than it is to do nothing to this prefix line.
                // If the prefix line only contains whitespace, make sure there's enough room to remove it as well.
            }
            if (StringUtils.indexOfNonWhitespace(s) >= shift || (StringUtils.indexOfNonWhitespace(s) == -1 && s.length() >= shift)) {
                return s.substring(shift);
            }
            return s;
        }));
    }
}
