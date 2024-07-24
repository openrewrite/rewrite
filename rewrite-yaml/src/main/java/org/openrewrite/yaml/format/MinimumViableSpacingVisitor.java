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
package org.openrewrite.yaml.format;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@RequiredArgsConstructor
public class MinimumViableSpacingVisitor<P> extends YamlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
        if (!e.getPrefix().contains("\n")) {
            if (getCursor().getParentOrThrow(2).getValue() instanceof Yaml.Document) {
                Yaml.Mapping mapping = getCursor().getParentOrThrow().getValue();
                if (mapping.getEntries().isEmpty() || mapping.getEntries().get(0) == e) {
                    return e;
                }
            }

            Yaml enclosing = getCursor().getParentOrThrow(2).getValue();
            if (enclosing instanceof Yaml.Sequence.Entry) {
                Yaml.Mapping mapping = getCursor().getParentOrThrow().getValue();
                if (mapping.getEntries().isEmpty() || mapping.getEntries().get(0) == entry) {
                    return e;
                }
            }
            return e.withPrefix("\n");
        }
        return e;
    }

    @Override
    public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        Yaml.Sequence.Entry e = super.visitSequenceEntry(entry, p);
        if (!e.getPrefix().contains("\n") && e.isDash()) {
            return e.withPrefix("\n");
        }
        return e;
    }

    @Override
    public @Nullable Yaml postVisit(Yaml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Yaml.Documents.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Yaml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Yaml) tree;
        }
        return super.visit(tree, p);
    }
}
