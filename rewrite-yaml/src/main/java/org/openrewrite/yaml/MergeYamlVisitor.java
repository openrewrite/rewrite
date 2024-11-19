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
import org.openrewrite.yaml.tree.Yaml.Document;
import org.openrewrite.yaml.tree.Yaml.Mapping;
import org.openrewrite.yaml.tree.Yaml.Mapping.Entry;
import org.openrewrite.yaml.tree.Yaml.Scalar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.openrewrite.internal.ListUtils.*;
import static org.openrewrite.yaml.MergeYaml.REMOVE_PREFIX;

@AllArgsConstructor
@RequiredArgsConstructor
public class MergeYamlVisitor<P> extends YamlVisitor<P> {

    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    private final Yaml existing;
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
                            Document doc = docs.getDocuments().get(0);
                            if (doc.getBlock() instanceof Mapping) {
                                Mapping m = (Mapping) doc.getBlock();
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
    public Yaml visitScalar(Scalar existingScalar, P p) {
        if (existing.isScope(existingScalar) && incoming instanceof Scalar) {
            return mergeScalar(existingScalar, (Scalar) incoming);
        }
        return super.visitScalar(existingScalar, p);
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence existingSeq, P p) {
        if (existing.isScope(existingSeq)) {
            if (incoming instanceof Mapping) {
                // Distribute the incoming mapping to each entry in the sequence
                return existingSeq.withEntries(map(existingSeq.getEntries(), (i, existingSeqEntry) ->
                        existingSeqEntry.withBlock((Yaml.Block)
                                new MergeYamlVisitor<>(existingSeqEntry.getBlock(), incoming, acceptTheirs, objectIdentifyingProperty, shouldAutoFormat)
                                        .visitNonNull(existingSeqEntry.getBlock(), p, new Cursor(getCursor(), existingSeqEntry))
                        )
                ));
            } else if (incoming instanceof Yaml.Sequence) {
                return mergeSequence(existingSeq, (Yaml.Sequence) incoming, p, getCursor());
            }
        }
        return super.visitSequence(existingSeq, p);
    }

    @Override
    public Yaml visitMapping(Mapping existingMapping, P p) {
        if (existing.isScope(existingMapping) && incoming instanceof Mapping) {
            Mapping mapping = mergeMapping(existingMapping, (Mapping) incoming, p, getCursor());

            if (getCursor().getMessage(REMOVE_PREFIX, false)) {
                List<Entry> entries = ((Mapping) getCursor().getValue()).getEntries();
                return mapping.withEntries(mapLast(mapping.getEntries(), it -> it.withPrefix("\n" + grabPartLineBreak(entries.get(entries.size() - 1), 1))));
            }

            return mapping;
        }
        return super.visitMapping(existingMapping, p);
    }

    private static boolean keyMatches(@Nullable Entry e1, @Nullable Entry e2) {
        return e1 != null && e2 != null && e1.getKey().getValue().equals(e2.getKey().getValue());
    }

    private boolean keyMatches(Mapping m1, Mapping m2) {
        Optional<String> nameToAdd = m2.getEntries().stream()
                .filter(e -> objectIdentifyingProperty != null && objectIdentifyingProperty.equals(e.getKey().getValue()))
                .map(e -> ((Scalar) e.getValue()).getValue())
                .findAny();

        return nameToAdd.map(nameToAddValue -> m1.getEntries().stream()
                        .filter(e -> objectIdentifyingProperty.equals(e.getKey().getValue()))
                        .map(e -> ((Scalar) e.getValue()).getValue())
                        .anyMatch(existingName -> existingName.equals(nameToAddValue)))
                .orElse(false);
    }

    private Mapping mergeMapping(Mapping m1, Mapping m2, P p, Cursor cursor) {
        List<Entry> mergedEntries = map(m1.getEntries(), existingEntry -> {
            for (Entry incomingEntry : m2.getEntries()) {
                if (keyMatches(existingEntry, incomingEntry)) {
                    return existingEntry.withValue((Yaml.Block)
                            new MergeYamlVisitor<>(existingEntry.getValue(), incomingEntry.getValue(), acceptTheirs, objectIdentifyingProperty, shouldAutoFormat)
                                    .visitNonNull(existingEntry.getValue(), p, new Cursor(cursor, existingEntry)));
                }
            }
            return existingEntry;
        });

        List<Entry> mutatedEntries = concatAll(mergedEntries, map(m2.getEntries(), it -> {
            for (Entry existingEntry : m1.getEntries()) {
                if (keyMatches(existingEntry, it)) {
                    return null;
                }
            }
            // workaround: autoFormat put sometimes extra spaces before elements
            if (!mergedEntries.isEmpty() && it.getValue() instanceof Scalar && hasLineBreak(mergedEntries.get(0), 2)) {
                return it.withPrefix("\n" + grabPartLineBreak(mergedEntries.get(0), 1));
            }
            return shouldAutoFormat ? autoFormat(it, p, cursor) : it;
        }));

        if (m1.getEntries().size() < mutatedEntries.size()) {
            Cursor c = getCursor().dropParentUntil(it -> {
                if (it instanceof Document) {
                    return true;
                }

                if (it instanceof Mapping) {
                    List<Entry> entries = ((Mapping) it).getEntries();
                    // last member should search further upwards until two entries are found
                    if (entries.get(entries.size() - 1).equals(getCursor().getParentOrThrow().getValue())) {
                        return false;
                    }
                    return entries.size() > 1;
                }

                return false;
            });

            if (c.getValue() instanceof Document || c.getValue() instanceof Mapping) {
                String comment = null;

                if (c.getValue() instanceof Document) {
                    comment = ((Document) c.getValue()).getEnd().getPrefix();
                } else {
                    List<Entry> entries = ((Mapping) c.getValue()).getEntries();

                    // get comment from next element in same mapping block
                    for (int i = 0; i < entries.size() - 1; i++) {
                        if (entries.get(i).getValue().equals(getCursor().getValue())) {
                            comment = grabPartLineBreak(entries.get(i + 1), 0);
                            break;
                        }
                    }
                    // or retrieve it for last item from next element (could potentially be much higher in the tree)
                    if (comment == null && hasLineBreak(entries.get(entries.size() - 1), 1)) {
                        comment = grabPartLineBreak(entries.get(entries.size() - 1), 0);
                    }
                }

                if (comment != null) {
                    Entry last = mutatedEntries.get(mutatedEntries.size() - 1);
                    mutatedEntries.set(mutatedEntries.size() - 1, last.withPrefix(comment + last.getPrefix()));
                    c.putMessage(REMOVE_PREFIX, true);
                }
            }
        }

        removePrefixForDirectChildren(m1.getEntries(), mutatedEntries);

        return m1.withEntries(mutatedEntries);
    }

    private void removePrefixForDirectChildren(List<Entry> m1Entries, List<Entry> mutatedEntries) {
        for (int i = 0; i < m1Entries.size() - 1; i++) {
            if (m1Entries.get(i).getValue() instanceof Mapping && mutatedEntries.get(i).getValue() instanceof Mapping &&
                    ((Mapping) m1Entries.get(i).getValue()).getEntries().size() < ((Mapping) mutatedEntries.get(i).getValue()).getEntries().size()) {
                mutatedEntries.set(i + 1, mutatedEntries.get(i + 1).withPrefix("\n" + grabPartLineBreak(mutatedEntries.get(i + 1), 1)));
            }
        }
    }

    private Yaml.Sequence mergeSequence(Yaml.Sequence s1, Yaml.Sequence s2, P p, Cursor cursor) {
        if (acceptTheirs) {
            return s1;
        }

        boolean isSequenceOfScalars = s2.getEntries().stream().allMatch(entry -> entry.getBlock() instanceof Scalar);

        if (isSequenceOfScalars) {
            List<Yaml.Sequence.Entry> incomingEntries = new ArrayList<>(s2.getEntries());

            nextEntry:
            for (Yaml.Sequence.Entry entry : s1.getEntries()) {
                if (entry.getBlock() instanceof Scalar) {
                    String existingScalar = ((Scalar) entry.getBlock()).getValue();
                    for (Yaml.Sequence.Entry incomingEntry : incomingEntries) {
                        if (((Scalar) incomingEntry.getBlock()).getValue().equals(existingScalar)) {
                            incomingEntries.remove(incomingEntry);
                            continue nextEntry;
                        }
                    }
                }
            }

            return s1.withEntries(concatAll(s1.getEntries(), map(incomingEntries, it -> autoFormat(it, p, cursor))));
        } else {
            if (objectIdentifyingProperty == null) {
                // No identifier set to match entries on, so cannot continue
                return s1;
            } else {
                List<Yaml.Sequence.Entry> mutatedEntries = map(s2.getEntries(), entry -> {
                    Mapping incomingMapping = (Mapping) entry.getBlock();
                    for (Yaml.Sequence.Entry existingEntry : s1.getEntries()) {
                        Mapping existingMapping = (Mapping) existingEntry.getBlock();
                        if (keyMatches(existingMapping, incomingMapping)) {
                            Yaml.Sequence.Entry e1 = existingEntry.withBlock(mergeMapping(existingMapping, incomingMapping, p, cursor));
                            if (e1 == existingEntry) {
                                // Made no change, no need to consider the entry "mutated"
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

                List<Yaml.Sequence.Entry> entries = concatAll(
                        s1.getEntries().stream().filter(entry -> !mutatedEntries.contains(entry)).collect(Collectors.toList()),
                        map(mutatedEntries, entry -> autoFormat(entry, p, cursor)));

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

    private boolean hasLineBreak(Entry entry, int atLeastParts) {
        boolean a = LINE_BREAK.matcher(entry.getPrefix()).find();
        return a && !grabPartLineBreak(entry, atLeastParts - 1).isEmpty();
    }

    private String grabPartLineBreak(Entry entry, int index) {
        String[] parts = LINE_BREAK.split(entry.getPrefix());
        return parts.length > index ? parts[index] : "";
    }

    private Scalar mergeScalar(Scalar y1, Scalar y2) {
        String s1 = y1.getValue();
        String s2 = y2.getValue();
        return !s1.equals(s2) && !acceptTheirs ? y1.withValue(s2) : y1;
    }
}
