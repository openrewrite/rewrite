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

import org.openrewrite.yaml.tree.Yaml;

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

    private String shiftPrefix(String prefix) {
        return prefix.substring(0, prefix.indexOf('\n') + 1) +
                prefix.substring(prefix.indexOf('\n') + shift + 1);
    }
}
