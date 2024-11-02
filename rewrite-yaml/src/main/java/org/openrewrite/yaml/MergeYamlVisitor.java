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

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@RequiredArgsConstructor
public class MergeYamlVisitor<P> extends YamlVisitor<P> {
    private final Yaml scope;
    private final Yaml incoming;
    private final boolean acceptTheirs;

    @Nullable
    private final String objectIdentifyingProperty;

    private boolean shouldAutoFormat = true;

    public MergeYamlVisitor(Yaml scope, @Language("yml") String yamlString, boolean acceptTheirs, @Nullable String objectIdentifyingProperty) {
        this(scope,
                new YamlParser().parse(yamlString)
                        .findFirst()
                        .map(Yaml.Documents.class::cast)
                        .map(docs -> {
                            // Any comments will have been put on the parent Document node, preserve by copying to the mapping
                            Yaml.Document doc = docs.getDocuments().get(0);
                            if(doc.getBlock() instanceof Yaml.Mapping) {
                                Yaml.Mapping m = (Yaml.Mapping) doc.getBlock();
                                return m.withEntries(ListUtils.mapFirst(m.getEntries(), entry -> entry.withPrefix(doc.getPrefix())));
                            } else if (doc.getBlock() instanceof Yaml.Sequence) {
                                Yaml.Sequence s = (Yaml.Sequence) doc.getBlock();
                                return s.withEntries(ListUtils.mapFirst(s.getEntries(), entry -> entry.withPrefix(doc.getPrefix())));
                            }
                            return doc.getBlock().withPrefix(doc.getPrefix());
                        })
                        .orElseThrow(() -> new IllegalArgumentException("Could not parse as YAML")),
                acceptTheirs,
                objectIdentifyingProperty);
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar existingScalar, P p) {
        if (scope.isScope(existingScalar) && incoming instanceof Yaml.Scalar) {
            return mergeScalar(existingScalar, (Yaml.Scalar) incoming);
        }
        return super.visitScalar(existingScalar, p);
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence existingSeq, P p) {
        if (scope.isScope(existingSeq)) {
            if (incoming instanceof Yaml.Mapping) {
                // Distribute the incoming mapping to each entry in the sequence
                return existingSeq.withEntries(ListUtils.map(existingSeq.getEntries(), (i, existingSeqEntry) -> {
                    Yaml.Block b = (Yaml.Block) new MergeYamlVisitor<>(existingSeqEntry.getBlock(), incoming,
                            acceptTheirs, objectIdentifyingProperty, shouldAutoFormat)
                            .visitNonNull(existingSeqEntry.getBlock(), p, new Cursor(getCursor(), existingSeqEntry));
                    return existingSeqEntry.withBlock(b);
                }));
            } else if (incoming instanceof Yaml.Sequence) {
                return mergeSequence(existingSeq, (Yaml.Sequence) incoming, p, getCursor());
            }
        }
        return super.visitSequence(existingSeq, p);
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping existingMapping, P p) {
        if (scope.isScope(existingMapping) && incoming instanceof Yaml.Mapping) {
            return mergeMapping(existingMapping, (Yaml.Mapping) incoming, p, getCursor());
        }
        return super.visitMapping(existingMapping, p);
    }

    private static boolean keyMatches(Yaml.Mapping.Entry e1, Yaml.Mapping.Entry e2) {
        return e1.getKey().getValue().equals(e2.getKey().getValue());
    }

    private boolean keyMatches(Yaml.Mapping m1, Yaml.Mapping m2) {
        Optional<String> nameToAdd = m2.getEntries().stream()
                .filter(e -> objectIdentifyingProperty != null && objectIdentifyingProperty.equals(e.getKey().getValue()))
                .map(e -> ((Yaml.Scalar) e.getValue()).getValue())
                .findAny();

        return nameToAdd.map(nameToAddValue -> m1.getEntries().stream()
                        .filter(e -> objectIdentifyingProperty.equals(e.getKey().getValue()))
                        .map(e -> ((Yaml.Scalar) e.getValue()).getValue())
                        .anyMatch(existingName -> existingName.equals(nameToAddValue)))
                .orElse(false);
    }

    private Yaml.Mapping mergeMapping(Yaml.Mapping m1, Yaml.Mapping m2, P p, Cursor cursor) {
        List<Yaml.Mapping.Entry> mutatedEntries = ListUtils.map(m1.getEntries(), existingEntry -> {
            for (Yaml.Mapping.Entry incomingEntry : m2.getEntries()) {
                if (keyMatches(existingEntry, incomingEntry)) {
                    return existingEntry.withValue((Yaml.Block) new MergeYamlVisitor<>(existingEntry.getValue(),
                            incomingEntry.getValue(), acceptTheirs, objectIdentifyingProperty, shouldAutoFormat)
                            .visitNonNull(existingEntry.getValue(), p, new Cursor(cursor, existingEntry)));
                }
            }
            return existingEntry;
        });

        mutatedEntries = ListUtils.concatAll(mutatedEntries, ListUtils.map(m2.getEntries(), incomingEntry -> {
            for (Yaml.Mapping.Entry existingEntry : m1.getEntries()) {
                if (keyMatches(existingEntry, incomingEntry)) {
                    return null;
                }
            }
            if (shouldAutoFormat) {
                incomingEntry = autoFormat(incomingEntry, p, cursor);
            }
            return incomingEntry;
        }));

        return m1.withEntries(mutatedEntries);
    }

    private Yaml.Sequence mergeSequence(Yaml.Sequence s1, Yaml.Sequence s2, P p, Cursor cursor) {
        if (acceptTheirs) {
            return s1;
        }

        boolean isSequenceOfScalars = s2.getEntries().stream().allMatch(entry -> entry.getBlock() instanceof Yaml.Scalar);

        if (isSequenceOfScalars) {
            List<Yaml.Sequence.Entry> incomingEntries = new ArrayList<>(s2.getEntries());

            nextEntry:
            for (Yaml.Sequence.Entry entry : s1.getEntries()) {
                if (entry.getBlock() instanceof Yaml.Scalar) {
                    String existingScalar = ((Yaml.Scalar) entry.getBlock()).getValue();
                    for (Yaml.Sequence.Entry incomingEntry : incomingEntries) {
                        if (((Yaml.Scalar) incomingEntry.getBlock()).getValue().equals(existingScalar)) {
                            incomingEntries.remove(incomingEntry);
                            continue nextEntry;
                        }
                    }
                }
            }

            return s1.withEntries(ListUtils.concatAll(s1.getEntries(),
                    ListUtils.map(incomingEntries, incomingEntry -> autoFormat(incomingEntry, p, cursor))));
        } else {
            if (objectIdentifyingProperty == null) {
                // No identifier set to match entries on, so cannot continue
                return s1;
            } else {
                List<Yaml.Sequence.Entry> mutatedEntries = ListUtils.map(s2.getEntries(), entry -> {
                    Yaml.Mapping incomingMapping = (Yaml.Mapping) entry.getBlock();
                    for (Yaml.Sequence.Entry existingEntry : s1.getEntries()) {
                        Yaml.Mapping existingMapping = (Yaml.Mapping) existingEntry.getBlock();
                        if (keyMatches(existingMapping, incomingMapping)) {
                            Yaml.Sequence.Entry e1 = existingEntry.withBlock(mergeMapping(existingMapping, incomingMapping, p, cursor));
                            if(e1 == existingEntry) {
                                // Made no change, no need to consider the entry "mutated"
                                //noinspection DataFlowIssue
                                return null;
                            }
                            return e1;
                        }
                    }
                    return entry;
                });
                if (mutatedEntries.isEmpty()) {
                    return s1;
                }

                List<Yaml.Sequence.Entry> entries = ListUtils.concatAll(
                        s1.getEntries().stream().filter(entry -> !mutatedEntries.contains(entry))
                                .collect(Collectors.toList()),
                        ListUtils.map(mutatedEntries, entry -> autoFormat(entry, p, cursor)));

                if (entries.size() != s1.getEntries().size()) {
                    return s1.withEntries(entries);
                }
                for (int i = 0; i < s1.getEntries().size(); i++) {
                    if (entries.get(i) != s1.getEntries().get(i)) {
                        return s1.withEntries(entries);
                    }
                }
                return s1;
            }
        }
    }

    private Yaml.Scalar mergeScalar(Yaml.Scalar y1, Yaml.Scalar y2) {
        String s1 = y1.getValue();
        String s2 = y2.getValue();
        return !s1.equals(s2) && !acceptTheirs ? y1.withValue(s2) : y1;
    }
}
