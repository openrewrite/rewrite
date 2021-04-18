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

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.style.Autodetect;
import org.openrewrite.yaml.style.YamlDefaultStyles;
import org.openrewrite.yaml.tree.Yaml;

import java.util.stream.Collectors;

@Incubating(since = "7.2.0")
public class InsertYamlVisitor<P> extends YamlIsoVisitor<P> {
    private final Yaml scope;
    private final String yamlString;

    @Nullable
    private Yaml.Block yaml;

    public InsertYamlVisitor(Yaml scope, String yaml) {
        this.scope = scope;
        this.yamlString = yaml;
    }

    @Override
    public Yaml.Documents visitDocuments(Yaml.Documents documents, P p) {
        if (yaml == null) {
            Yaml.Documents docs = new YamlParser().parse(
                    p instanceof ExecutionContext ?
                            (ExecutionContext) p :
                            new InMemoryExecutionContext(),
                    yamlString
            ).get(0);

            StringBuilder leadingIndentBuilder = new StringBuilder();
            if (!(scope instanceof Yaml.Document)) {
                for (int i = 0; i < findIndent(scope.getPrefix()) + Autodetect
                        .tabsAndIndents(documents, YamlDefaultStyles.indents()).getIndentSize(); i++) {
                    leadingIndentBuilder.append(' ');
                }
            }
            String leadingIndent = leadingIndentBuilder.toString();

            // indent to position underneath scope
            docs = (Yaml.Documents) new YamlIsoVisitor<Integer>() {
                boolean firstBlock = true;

                @Override
                public Yaml preVisit(Yaml tree, Integer integer) {
                    if ((tree instanceof Yaml.Mapping.Entry || tree instanceof Yaml.Sequence.Entry)
                            && firstBlock) {
                        tree = tree.withPrefix("\n");
                        firstBlock = false;
                    }
                    if (tree.getPrefix().contains("\n")) {
                        tree = tree.withPrefix(tree.getPrefix() + leadingIndent);
                    }
                    return tree;
                }
            }.visit(docs, 0);

            assert docs != null;
            yaml = docs.getDocuments()
                    .get(0)
                    .getBlock();
        }
        return super.visitDocuments(documents, p);
    }

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, P p) {
        Yaml.Document d = document;

        if (scope.isScope(d)) {
            if (d.getBlock() instanceof Yaml.Mapping) {
                d = d.withBlock(addEntries((Yaml.Mapping) d.getBlock()));
                return d;
            } else {
                // TODO support insertion into sequence
            }
        }

        return super.visitDocument(d, p);
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);

        if (scope.isScope(entry)) {
            Yaml.Block value = e.getValue();
            if (value instanceof Yaml.Scalar && ((Yaml.Scalar) value).getValue().isEmpty()) {
                e = e.withValue(yaml);
            } else if (value instanceof Yaml.Mapping) {
                Yaml.Mapping map = (Yaml.Mapping) value;
                e = e.withValue(addEntries(map));
            } else if (value instanceof Yaml.Sequence) {
                Yaml.Sequence seq = (Yaml.Sequence) value;
                // TODO implement me!
                throw new UnsupportedOperationException("Inserting sequence entries not yet supported");
            }
        }

        return e;
    }

    private Yaml.Mapping addEntries(Yaml.Mapping map) {
        assert yaml instanceof Yaml.Mapping;
        return map.withEntries(ListUtils.concatAll(map.getEntries(),
                ((Yaml.Mapping) yaml).getEntries().stream()
                        .filter(newEntry -> map.getEntries().stream()
                                .noneMatch(existingEntry -> existingEntry.getKey().getValue().equals(newEntry.getKey().getValue())))
                        .collect(Collectors.toList()))
        );
    }

    private int findIndent(String prefix) {
        int size = 0;
        for (char c : prefix.toCharArray()) {
            size++;
            if (c == '\n' || c == '\r') {
                size = 0;
            }
        }
        return size;
    }
}
