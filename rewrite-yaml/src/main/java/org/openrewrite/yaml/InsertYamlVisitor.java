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

import java.util.Objects;
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

            // place the first entry to insert on a new line
            docs = (Yaml.Documents) new YamlIsoVisitor<Integer>() {
                boolean firstBlock = true;

                @Override
                public Yaml preVisit(Yaml tree, Integer integer) {
                    if ((tree instanceof Yaml.Mapping.Entry || tree instanceof Yaml.Sequence.Entry)
                            && firstBlock) {
                        tree = tree.withPrefix("\n");
                        firstBlock = false;
                    }
                    return tree;
                }
            }.visit(docs, 0);

            // indent to position underneath scope
            assert docs != null;
            docs = shiftRight(docs, leadingIndentBuilder.toString());

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
                d = d.withBlock(addEntries((Yaml.Mapping) d.getBlock(), yaml));
                return d;
            } else {
                // TODO support insertion into sequence
            }
        }

        return super.visitDocument(d, p);
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
        Yaml.Mapping m = super.visitMapping(mapping, p);

        if (scope.isScope(mapping)) {
            Yaml.Block value = m;
            assert yaml != null;
            m = (Yaml.Mapping) insertIntoBlock(value, yaml);
        }

        return m;
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);

        if (scope.isScope(entry)) {
            Yaml.Block value = e.getValue();
            assert yaml != null;
            e = e.withValue(insertIntoBlock(value, yaml));
        }

        return e;
    }

    private Yaml.Block insertIntoBlock(Yaml.Block block, Yaml.Block insert) {
        if (block instanceof Yaml.Scalar && ((Yaml.Scalar) block).getValue().isEmpty()) {
            return insert;
        } else if (block instanceof Yaml.Mapping) {
            return addEntries((Yaml.Mapping) block, insert);
        } else if (block instanceof Yaml.Sequence) {
            Yaml.Sequence seq = (Yaml.Sequence) block;
            return seq.withEntries(ListUtils.map(seq.getEntries(), entry -> {
                if (entry.getBlock() instanceof Yaml.Mapping) {
                    Yaml.Mapping mapping = (Yaml.Mapping) entry.getBlock();
                    Yaml.Block shiftedInsert = shiftRight(insert,
                            mapping.getEntries().iterator().next().getPrefix() + " ");
                    return entry.withBlock(insertIntoBlock(entry.getBlock(), shiftedInsert));
                }
                return entry;
            }));
        }
        return block;
    }

    private Yaml.Mapping addEntries(Yaml.Mapping map, @Nullable Yaml.Block insert) {
        assert insert instanceof Yaml.Mapping;
        return map.withEntries(ListUtils.concatAll(map.getEntries(),
                ((Yaml.Mapping) insert).getEntries().stream()
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

    private <Y extends Yaml> Y shiftRight(Y y, String shift) {
        //noinspection unchecked
        return (Y) Objects.requireNonNull(new YamlIsoVisitor<Integer>() {
            @Override
            public Yaml preVisit(Yaml tree, Integer integer) {
                if (tree.getPrefix().contains("\n")) {
                    tree = tree.withPrefix(tree.getPrefix() + shift);
                }
                return tree;
            }
        }.visit(y, 0));
    }
}
