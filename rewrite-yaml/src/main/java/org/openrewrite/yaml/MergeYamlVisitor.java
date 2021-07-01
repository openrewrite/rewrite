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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Marker;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class MergeYamlVisitor extends YamlVisitor<ExecutionContext> {
    final Yaml scope;
    final Yaml incoming;

    @Override
    public Yaml visitScalar(Yaml.Scalar existingScalar, ExecutionContext executionContext) {
        if (!scope.isScope(existingScalar)) {
            return super.visitScalar(existingScalar, executionContext);
        }
        return mergeScalar(existingScalar, (Yaml.Scalar) incoming);
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence existingSeq, ExecutionContext ctx) {
        if (!scope.isScope(existingSeq)) {
            return super.visitSequence(existingSeq, ctx);
        }
        return mergeSequence(existingSeq, (Yaml.Sequence) incoming, ctx, getCursor());
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping existingMapping, ExecutionContext ctx) {
        if (!scope.isScope(existingMapping)) {
            return super.visitMapping(existingMapping, ctx);
        }

        return mergeMapping(existingMapping, (Yaml.Mapping) incoming, ctx, getCursor());
    }

    private static boolean keyMatches(Yaml.Mapping.Entry e1, Yaml.Mapping.Entry e2) {
        return e1.getKey().getValue().equals(e2.getKey().getValue());
    }

    private static Yaml.Mapping mergeMapping(Yaml.Mapping m1, Yaml.Mapping m2, ExecutionContext ctx, Cursor cursor) {
        List<Yaml.Mapping.Entry> mutatedEntries = ListUtils.map(m1.getEntries(), existingEntry -> {
            return m2.getEntries().stream()
                    .filter(incomingEntry -> keyMatches(existingEntry, incomingEntry))
                    .findFirst()
                    .map(incomingEntry -> {
                        return existingEntry.withValue((Yaml.Block) new MergeYamlVisitor(existingEntry.getValue(), incomingEntry.getValue())
                                .visit(existingEntry.getValue(), ctx, cursor));
                    })
                    .orElse(existingEntry);
        });
        return m1.withEntries(mutatedEntries);
    }

    private static Yaml.Sequence mergeSequence(Yaml.Sequence existingSeq, Yaml.Sequence incomingSeq, ExecutionContext ctx, Cursor cursor) {
        AtomicInteger idx = new AtomicInteger(0);
        List<Yaml.Sequence.Entry> incomingEntries = incomingSeq.getEntries();
        List<Yaml.Sequence.Entry> entries = ListUtils.map(existingSeq.getEntries(), existingSeqEntry -> {
            Yaml.Sequence.Entry incomingEntry = incomingEntries.get(idx.getAndIncrement());
            Yaml.Block b = (Yaml.Block) new MergeYamlVisitor(existingSeqEntry.getBlock(), incomingEntry.getBlock())
                    .visit(existingSeqEntry.getBlock(), ctx, cursor);
            return existingSeqEntry.withBlock(requireNonNull(b));
        });
        return existingSeq.withEntries(entries);
    }

    private static Yaml.Scalar mergeScalar(Yaml.Scalar y1, Yaml.Scalar y2) {
        String s1 = y1.getValue();
        String s2 = y2.getValue();
        for (Marker m : y2.getMarkers().entries()) {
            y1 = y1.withMarkers(y1.getMarkers().addIfAbsent(m));
        }
        return !s1.equals(s2) ? y1.withValue(s2) : y1;

    }

}
