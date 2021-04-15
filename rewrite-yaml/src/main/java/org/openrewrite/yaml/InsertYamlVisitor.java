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

import java.util.List;
import java.util.stream.Collectors;

@Incubating(since = "7.2.0")
public class InsertYamlVisitor<P> extends YamlIsoVisitor<P> {
    private final Yaml.Mapping.Entry scope;

    private final String yamlString;

    @Nullable
    private List<Yaml.Block> yaml;

    public InsertYamlVisitor(Yaml.Mapping.Entry scope, String yaml) {
        this.scope = scope;
        this.yamlString = yaml;
    }

    @Override
    public Yaml.Documents visitDocuments(Yaml.Documents documents, P p) {
        if (yaml == null) {
            Yaml.Documents docs = new YamlParser().parse(p instanceof ExecutionContext ?
                            (ExecutionContext) p :
                            new InMemoryExecutionContext(),
                    yamlString)
                    .get(0);

            StringBuilder leadingIndentBuilder = new StringBuilder();
            for (int i = 0; i < findIndent(scope.getPrefix()) + Autodetect
                    .tabsAndIndents(documents, YamlDefaultStyles.indents()).getIndentSize(); i++) {
                leadingIndentBuilder.append(' ');
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
                    .getBlocks();
        }
        return super.visitDocuments(documents, p);
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
        assert yaml != null;

        if (scope.isScope(entry)) {
            Yaml.Block value = e.getValue();
            if (value instanceof Yaml.Mapping) {
                Yaml.Mapping map = (Yaml.Mapping) value;

                e = e.withValue(map.withEntries(ListUtils.concatAll(map.getEntries(),
                        yaml.stream()
                                .peek(b -> {
                                    if (!(b instanceof Yaml.Mapping) && p instanceof ExecutionContext) {
                                        ((ExecutionContext) p).getOnError().accept(new IllegalArgumentException(
                                                "Attempted to insert a " + b.getClass().getSimpleName() + " into a Yaml.Mapping"));
                                    }
                                })
                                .filter(Yaml.Mapping.class::isInstance)
                                .flatMap(mapping -> ((Yaml.Mapping) mapping).getEntries().stream())
                                .collect(Collectors.toList()))
                ));
            } else if (value instanceof Yaml.Sequence) {
                Yaml.Sequence seq = (Yaml.Sequence) value;
            }
        }

        return e;
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
