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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.ListUtils.concatAll;
import static org.openrewrite.internal.ListUtils.map;
import static org.openrewrite.yaml.MappingUtils.keyMatches;

@RequiredArgsConstructor
public class MergeYamlVisitor extends YamlIsoVisitor<ExecutionContext> {
    @Nullable
    final Yaml scope;
    final Yaml incoming;

    @Override
    public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
        if (null != scope && scope.isScope(documents)) {
            return documents.withDocuments(map(documents.getDocuments(), doc ->
                    (Yaml.Document) new MergeYamlVisitor(doc, ((Yaml.Documents) incoming).getDocuments().get(0))
                            .visit(doc, ctx, getCursor())));
        }
        return super.visitDocuments(documents, ctx);
    }

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
        if (requireNonNull(scope).isScope(document)) {
            return document.withBlock((Yaml.Block) new MergeYamlVisitor(document.getBlock(), ((Yaml.Document) incoming).getBlock())
                    .visit(document.getBlock(), ctx, getCursor()));
        }
        return super.visitDocument(document, ctx);
    }

    @Override
    public Yaml.Scalar visitScalar(Yaml.Scalar existingScalar, ExecutionContext ctx) {
        if (requireNonNull(scope).isScope(existingScalar)) {
            return mergeScalar(existingScalar, (Yaml.Scalar) incoming);
        }
        return super.visitScalar(existingScalar, ctx);
    }

    @Override
    public Yaml.Sequence visitSequence(Yaml.Sequence existingSeq, ExecutionContext ctx) {
        if (requireNonNull(scope).isScope(existingSeq)) {
            return mergeSequence(existingSeq, (Yaml.Sequence) incoming, ctx, getCursor());
        }
        return super.visitSequence(existingSeq, ctx);
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping existingMapping, ExecutionContext ctx) {
        if (requireNonNull(scope).isScope(existingMapping)) {
            return mergeMapping(existingMapping, (Yaml.Mapping) incoming, ctx, getCursor());
        }
        return super.visitMapping(existingMapping, ctx);
    }

    private static Yaml.Mapping mergeMapping(Yaml.Mapping m1, Yaml.Mapping m2, ExecutionContext ctx, Cursor cursor) {
        List<Yaml.Mapping.Entry> incomingEntries = new ArrayList<>(m2.getEntries());
        List<Yaml.Mapping.Entry> newEntries = map(m1.getEntries(), existingEntry -> {
            for (Yaml.Mapping.Entry incomingEntry : m2.getEntries()) {
                if (keyMatches(existingEntry, incomingEntry)) {
                    return existingEntry
                            .withValue((Yaml.Block) new MergeYamlVisitor(existingEntry.getValue(), incomingEntry.getValue())
                                    .visit(existingEntry.getValue(), ctx, cursor));
                }
            }
            return existingEntry;
        });
        incomingEntries.removeIf(e -> newEntries.stream().anyMatch(newe -> keyMatches(newe, e)));
        return m1.withEntries(concatAll(newEntries, incomingEntries));
    }

    private static Yaml.Sequence mergeSequence(Yaml.Sequence existingSeq, Yaml.Sequence incomingSeq, ExecutionContext ctx, Cursor cursor) {
        AtomicInteger idx = new AtomicInteger(0);
        List<Yaml.Sequence.Entry> incomingEntries = incomingSeq.getEntries();
        List<Yaml.Sequence.Entry> entries = map(existingSeq.getEntries(), existingSeqEntry -> {
            Yaml.Sequence.Entry incomingEntry = incomingEntries.get(idx.getAndIncrement());
            if (existingSeqEntry == incomingEntry) {
                return existingSeqEntry;
            }
            Yaml.Block b = (Yaml.Block) new MergeYamlVisitor(existingSeqEntry.getBlock(), incomingEntry.getBlock())
                    .visit(existingSeqEntry.getBlock(), ctx, cursor);
            return existingSeqEntry.withBlock(requireNonNull(b));
        });
        if (incomingEntries.size() > idx.get()) {
            entries = concatAll(entries, incomingEntries.subList(idx.get(), incomingEntries.size()));
        }
        return existingSeq.withEntries(entries);
    }

    private static Yaml.Scalar mergeScalar(Yaml.Scalar y1, Yaml.Scalar y2) {
        String s1 = y1.getValue();
        String s2 = y2.getValue();
        return !s1.equals(s2) ? y1.withValue(s2) : y1;

    }

}
