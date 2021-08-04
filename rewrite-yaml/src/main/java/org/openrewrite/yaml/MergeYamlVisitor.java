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
package org.openrewrite.yaml;

import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class MergeYamlVisitor<P> extends YamlVisitor<P> {
    private final Yaml scope;
    private final Yaml incoming;
    private final boolean acceptTheirs;

    public MergeYamlVisitor(Yaml scope, @Language("yml") String yamlString, boolean acceptTheirs) {
        this(scope, new YamlParser().parse(yamlString).get(0).getDocuments().get(0).getBlock(), acceptTheirs);
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar existingScalar, P p) {
        if (scope.isScope(existingScalar)) {
            return mergeScalar(existingScalar, (Yaml.Scalar) incoming);
        }
        return super.visitScalar(existingScalar, p);
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence existingSeq, P p) {
        if (scope.isScope(existingSeq)) {
            if(incoming instanceof Yaml.Mapping) {
                return existingSeq.withEntries(ListUtils.map(existingSeq.getEntries(), (i, existingSeqEntry) -> {
                    Yaml.Block b = (Yaml.Block) new MergeYamlVisitor<>(existingSeqEntry.getBlock(),
                            incoming, acceptTheirs).visit(existingSeqEntry.getBlock(), p, getCursor());
                    return existingSeqEntry.withBlock(requireNonNull(b));
                }));
            } else if(incoming instanceof Yaml.Sequence) {
                return mergeSequence(existingSeq, (Yaml.Sequence) incoming, p, getCursor());
            }
        }
        return super.visitSequence(existingSeq, p);
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping existingMapping, P p) {
        if (scope.isScope(existingMapping)) {
            return mergeMapping(existingMapping, (Yaml.Mapping) incoming, p, getCursor());
        }
        return super.visitMapping(existingMapping, p);
    }

    private static boolean keyMatches(Yaml.Mapping.Entry e1, Yaml.Mapping.Entry e2) {
        return e1.getKey().getValue().equals(e2.getKey().getValue());
    }

    private Yaml.Mapping mergeMapping(Yaml.Mapping m1, Yaml.Mapping m2, P p, Cursor cursor) {
        List<Yaml.Mapping.Entry> mutatedEntries = ListUtils.map(m1.getEntries(), existingEntry ->
                m2.getEntries().stream()
                        .filter(incomingEntry -> keyMatches(existingEntry, incomingEntry))
                        .findFirst()
                        .map(incomingEntry ->
                                existingEntry.withValue((Yaml.Block) new MergeYamlVisitor<>(existingEntry.getValue(),
                                        incomingEntry.getValue(), acceptTheirs).visit(existingEntry.getValue(), p, cursor)))
                        .orElse(existingEntry));

        mutatedEntries = ListUtils.concatAll(mutatedEntries, m2.getEntries().stream()
                .filter(incomingEntry -> {
                    for (Yaml.Mapping.Entry existingEntry : m1.getEntries()) {
                        if (keyMatches(existingEntry, incomingEntry)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(incomingEntry -> autoFormat(incomingEntry, p, getCursor().getParentOrThrow()))
                .collect(Collectors.toList()));

        return m1.withEntries(mutatedEntries);
    }

    private Yaml.Sequence mergeSequence(Yaml.Sequence s1, Yaml.Sequence s2, P p, Cursor cursor) {
        AtomicInteger idx = new AtomicInteger(0);
        List<Yaml.Sequence.Entry> incomingEntries = s2.getEntries();

        List<Yaml.Sequence.Entry> mutatedEntries = ListUtils.map(s1.getEntries(), existingSeqEntry -> {
            Yaml.Sequence.Entry incomingEntry = incomingEntries.get(idx.getAndIncrement());
            Yaml.Block b = (Yaml.Block) new MergeYamlVisitor<>(existingSeqEntry.getBlock(),
                    incomingEntry.getBlock(), acceptTheirs).visit(existingSeqEntry.getBlock(), p, cursor);
            return existingSeqEntry.withBlock(requireNonNull(b));
        });

        return s1.withEntries(mutatedEntries);
    }

    private Yaml.Scalar mergeScalar(Yaml.Scalar y1, Yaml.Scalar y2) {
        String s1 = y1.getValue();
        String s2 = y2.getValue();
        return !s1.equals(s2) && !acceptTheirs ? y1.withValue(s2) : y1;
    }
}
