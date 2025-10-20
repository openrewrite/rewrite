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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;
import org.openrewrite.yaml.MergeYaml.InsertMode;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.openrewrite.Cursor.ROOT_VALUE;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.ListUtils.*;
import static org.openrewrite.internal.StringUtils.*;
import static org.openrewrite.yaml.MergeYaml.InsertMode.*;
import static org.openrewrite.yaml.MergeYaml.REMOVE_PREFIX;

/**
 * Visitor class to merge two yaml files.
 *
 * @param <P> An input object that is passed to every visit method.
 * @implNote Loops recursively through the documents, for every part a new MergeYamlVisitor instance will be created.
 * As inline comments are put on the prefix of the next element (which can be an item very much higher in the tree),
 * the following solutions are chosen to merge the comments as well:
 * <ul>
 * <li>when an element has new items, the comment of the next element is copied to the previous element
 * <li>the original comment will be removed (either by traversing the children or by using cursor messages)
 */
@RequiredArgsConstructor
public class MergeYamlVisitor<P> extends YamlVisitor<P> {

    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    private final Yaml existing;
    private final Yaml incoming;
    private final boolean acceptTheirs;

    @Nullable
    private final String objectIdentifyingProperty;

    @Nullable
    private final InsertMode insertMode;

    @Nullable
    private final String insertProperty;

    private boolean shouldAutoFormat = true;

    @Nullable
    private String linebreak = null;

    private String linebreak() {
        if (linebreak == null) {
            linebreak = Optional.ofNullable(getCursor().firstEnclosing(Yaml.Documents.class))
                    .map(docs -> Style.from(GeneralFormatStyle.class, docs))
                    .map(format -> format.isUseCRLFNewLines() ? "\r\n" : "\n")
                    .orElse("\n");
        }
        return linebreak;
    }

    public MergeYamlVisitor(Yaml.Block block, Yaml incoming, boolean acceptTheirs, @Nullable String objectIdentifyingProperty, boolean shouldAutoFormat, @Nullable InsertMode insertMode, @Nullable String insertProperty) {
        this(block, incoming, acceptTheirs, objectIdentifyingProperty, insertMode, insertProperty);
        this.shouldAutoFormat = shouldAutoFormat;
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar existingScalar, P p) {
        if (existing.isScope(existingScalar) && incoming instanceof Yaml.Scalar) {
            return mergeScalar(existingScalar, (Yaml.Scalar) incoming);
        }
        return super.visitScalar(existingScalar, p);
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence existingSeq, P p) {
        if (existing.isScope(existingSeq)) {
            if (incoming instanceof Yaml.Mapping) {
                // Distribute the incoming mapping to each entry in the sequence
                return existingSeq.withEntries(map(existingSeq.getEntries(), (i, existingSeqEntry) ->
                        existingSeqEntry.withBlock((Yaml.Block)
                                new MergeYamlVisitor<>(existingSeqEntry.getBlock(), incoming, acceptTheirs, objectIdentifyingProperty, shouldAutoFormat, insertMode, insertProperty)
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
    public Yaml visitMapping(Yaml.Mapping existingMapping, P p) {
        if (existing.isScope(existingMapping) && incoming instanceof Yaml.Mapping) {
            Yaml.Mapping mapping = mergeMapping(existingMapping, (Yaml.Mapping) incoming, p, getCursor());

            if (getCursor().getMessage(REMOVE_PREFIX, false)) {
                List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) getCursor().getValue()).getEntries();
                return mapping.withEntries(mapLast(mapping.getEntries(), it ->
                        it.withPrefix(linebreak() + substringOfAfterFirstLineBreak(entries.get(entries.size() - 1).getPrefix()))));
            }

            return mapping;
        }
        return super.visitMapping(existingMapping, p);
    }

    private static boolean keyMatches(Yaml.Mapping.@Nullable Entry e1, Yaml.Mapping.@Nullable Entry e2) {
        return e1 != null && e2 != null && e1.getKey().getValue().equals(e2.getKey().getValue());
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
        // Merge same key, different value together
        List<Yaml.Mapping.Entry> mergedEntries = map(m1.getEntries(), existingEntry -> {
            for (Yaml.Mapping.Entry incomingEntry : m2.getEntries()) {
                if (keyMatches(existingEntry, incomingEntry)) {
                    Yaml.Block value = incomingEntry.getValue();
                    if (shouldAutoFormat && incomingEntry.getValue() instanceof Yaml.Scalar && hasLineBreak(((Yaml.Scalar) value).getValue())) {
                        MultilineScalarChanged marker = new MultilineScalarChanged(randomId(), false, calculateMultilineIndent(incomingEntry));
                        value = autoFormat(value.withMarkers(value.getMarkers().add(marker)), p);
                    }
                    Yaml mergedYaml = new MergeYamlVisitor<>(existingEntry.getValue(), value, acceptTheirs, objectIdentifyingProperty, shouldAutoFormat, insertMode, insertProperty)
                            .visitNonNull(existingEntry.getValue(), p, new Cursor(cursor, existingEntry));
                    return existingEntry.withValue((Yaml.Block) mergedYaml);
                }
            }
            return existingEntry;
        });

        // Transform new entries with spacing, remove entries already existing in original mapping
        List<Yaml.Mapping.Entry> newEntries = map(m2.getEntries(), it -> {
            for (Yaml.Mapping.Entry existingEntry : m1.getEntries()) {
                if (keyMatches(existingEntry, it)) {
                    return null;
                }
            }
            if (shouldAutoFormat && it.getValue() instanceof Yaml.Scalar && hasLineBreak(((Yaml.Scalar) it.getValue()).getValue())) {
                MultilineScalarChanged marker = new MultilineScalarChanged(randomId(), true, calculateMultilineIndent(it));
                it = it.withValue(it.getValue().withMarkers(it.getValue().getMarkers().add(marker)));
            }
            return shouldAutoFormat ? autoFormat(it, p, cursor) : it;
        });

        // Merge existing and new entries together
        ListConcat<Yaml.Mapping.Entry> mutatedEntries = concatAll(mergedEntries, newEntries, it -> it.getKey().getValue());

        // copy comment to previous element if needed
        if (m1.getEntries().size() < mutatedEntries.ls.size() && !getCursor().isRoot()) {
            // If newly entries are inserted somewhere, but not the last entry nor at the document root, we can be sure no changes to the rest of the tree is needed
            if (mutatedEntries.lastNewlyAddedItemIndex != -1 && mutatedEntries.lastNewlyAddedItemIndex < mutatedEntries.ls.size() - 1) {
                Yaml.Mapping.Entry afterInsertEntry = mutatedEntries.ls.get(mutatedEntries.lastNewlyAddedItemIndex + 1);

                // Check if merge is done on the very first key of the yaml file
                if (mutatedEntries.firstNewlyAddedItemIndex == 0 && getCursor().getParentOrThrow().getValue() instanceof Yaml.Document &&
                        ((Yaml.Mapping) ((Yaml.Document) getCursor().getParentOrThrow().getValue()).getBlock()).getEntries().equals(((Yaml.Mapping) existing).getEntries())) {
                    mutatedEntries.ls.set(mutatedEntries.firstNewlyAddedItemIndex, mutatedEntries.ls.get(0).withPrefix("")); // Remove linebreak from first entry
                    mutatedEntries.ls.set(mutatedEntries.lastNewlyAddedItemIndex + 1, afterInsertEntry.withPrefix(linebreak() + ((Yaml.Document) getCursor().getParentOrThrow().getValue()).getPrefix() + afterInsertEntry.getPrefix()));
                    getCursor().getParentOrThrow().putMessage(REMOVE_PREFIX, true);
                } else {
                    Yaml.Mapping.Entry firstNewlyAddedEntry = mutatedEntries.ls.get(mutatedEntries.firstNewlyAddedItemIndex);
                    String partOne = substringOfBeforeFirstLineBreak(afterInsertEntry.getPrefix());
                    String partTwo = substringOfAfterFirstLineBreak(afterInsertEntry.getPrefix());

                    mutatedEntries.ls.set(mutatedEntries.firstNewlyAddedItemIndex, firstNewlyAddedEntry.withPrefix(partOne + firstNewlyAddedEntry.getPrefix()));
                    mutatedEntries.ls.set(mutatedEntries.lastNewlyAddedItemIndex + 1, afterInsertEntry.withPrefix(linebreak() + partTwo));
                }
            } else {
                Cursor c = getCursor().dropParentUntil(it -> {
                    if (ROOT_VALUE.equals(it) || it instanceof Yaml.Document) {
                        return true;
                    }

                    if (it instanceof Yaml.Mapping) {
                        List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) it).getEntries();
                        // At least two entries and when current elem is the last entry should not be current entry
                        return entries.size() > 1 && !entries.get(entries.size() - 1).equals(getCursor().getParentOrThrow().getValue());
                    }

                    return false;
                });

                String comment = null;
                if (c.getValue() instanceof Yaml.Document) {
                    Yaml.Document doc = c.getValue();
                    // Don't treat document end prefix as comment if it contains a document separator
                    if (!preserveDocumentSeparator(doc)) {
                        comment = doc.getEnd().getPrefix();
                    }
                } else if (c.getValue() instanceof Yaml.Mapping) {
                    List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) c.getValue()).getEntries();

                    // Get comment from next element in same mapping block
                    for (int i = 0; i < entries.size() - 1; i++) {
                        if (entries.get(i).getValue().equals(getCursor().getValue())) {
                            comment = substringOfBeforeFirstLineBreak(entries.get(i + 1).getPrefix());
                            break;
                        }
                    }
                    // OR retrieve it for last item from next element (could potentially be much higher in the tree).
                    if (comment == null && hasLineBreak(entries.get(entries.size() - 1).getPrefix())) {
                        comment = substringOfBeforeFirstLineBreak(entries.get(entries.size() - 1).getPrefix());
                    }
                }

                if (isNotEmpty(comment)) {
                    // Copy comment to last mutated element AND put message on cursor to remove comment from original element
                    Yaml.Mapping.Entry last = mutatedEntries.ls.get(mutatedEntries.ls.size() - 1);
                    mutatedEntries.ls.set(mutatedEntries.ls.size() - 1, last.withPrefix(comment + last.getPrefix()));
                    c.putMessage(REMOVE_PREFIX, true);
                }
            }
        }

        if (insertMode != Before) {
            removePrefixForDirectChildren(m1.getEntries(), mutatedEntries.ls);
        }

        return m1.withEntries(mutatedEntries.ls);
    }

    private void removePrefixForDirectChildren(List<Yaml.Mapping.Entry> m1Entries, List<Yaml.Mapping.Entry> mutatedEntries) {
        for (int i = 0; i < m1Entries.size() - 1; i++) {
            if (m1Entries.get(i).getValue() instanceof Yaml.Mapping && mutatedEntries.get(i).getValue() instanceof Yaml.Mapping &&
                    ((Yaml.Mapping) m1Entries.get(i).getValue()).getEntries().size() < ((Yaml.Mapping) mutatedEntries.get(i).getValue()).getEntries().size()) {
                mutatedEntries.set(i + 1, mutatedEntries.get(i + 1).withPrefix(
                        linebreak() + substringOfAfterFirstLineBreak(mutatedEntries.get(i + 1).getPrefix())));
            }
        }
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

            String existingEntryPrefix = s1.getEntries().get(0).getPrefix();
            String currentIndent = existingEntryPrefix.substring(existingEntryPrefix.lastIndexOf('\n'));
            List<Yaml.Sequence.Entry> newEntries = ListUtils.map(incomingEntries, it -> it.withPrefix(currentIndent));
            List<Yaml.Sequence.Entry> mutatedEntries = concatAll(s1.getEntries(), newEntries, it -> ((Yaml.Scalar) it.getBlock()).getValue()).ls;

            return s1.withEntries(mutatedEntries);
        }

        if (objectIdentifyingProperty == null) {
            // No identifier set to match entries on, so cannot continue
            return s1;
        }

        List<Yaml.Sequence.Entry> mutatedEntries = map(s2.getEntries(), entry -> {
            Yaml.Mapping incomingMapping = (Yaml.Mapping) entry.getBlock();
            for (Yaml.Sequence.Entry existingEntry : s1.getEntries()) {
                Yaml.Mapping existingMapping = (Yaml.Mapping) existingEntry.getBlock();
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

        List<Yaml.Sequence.Entry> entries = concatAll(
                filter(s1.getEntries(), it -> !mutatedEntries.contains(it)),
                map(mutatedEntries, it -> autoFormat(it, p, cursor)),
                it -> {
                    Yaml.Mapping.Entry entry = ((Yaml.Mapping) it.getBlock()).getEntries().get(0);
                    return entry.getKey().getValue() + ": " + ((Yaml.Scalar) entry.getValue()).getValue();
                }).ls;

        return s1.withEntries(entries);
    }

    private Yaml.Scalar mergeScalar(Yaml.Scalar y1, Yaml.Scalar y2) {
        String s1 = y1.getValue();
        String s2 = y2.getValue();
        return !s1.equals(s2) && !acceptTheirs ? y1.withValue(s2) : y1;
    }

    /**
     * Specialized concatAll function which takes the `insertPlace` property into account.
     */
    private <T> ListConcat<T> concatAll(List<T> ls, List<T> t, Function<T, String> getValue) {
        if (insertMode == null || insertMode == Last || insertProperty == null || t.isEmpty()) {
            return new ListConcat<>(ListUtils.concatAll(ls, t), -1, -1);
        }

        List<T> mutatedEntries = new ArrayList<>();
        boolean hasInsertedBeforeOrAfterElements = false;
        int insertIndex = -1, closeIndex = -1;
        for (int i = 0; i < ls.size(); i++) {
            T existingEntry = ls.get(i);
            if (!hasInsertedBeforeOrAfterElements && insertMode == Before && insertProperty.equals(getValue.apply(existingEntry))) {
                hasInsertedBeforeOrAfterElements = true;
                mutatedEntries.addAll(t);
                insertIndex = i;
                closeIndex = i + t.size() - 1;
            }
            mutatedEntries.add(existingEntry);
            if (!hasInsertedBeforeOrAfterElements && insertMode == After && insertProperty.equals(getValue.apply(existingEntry))) {
                hasInsertedBeforeOrAfterElements = true;
                mutatedEntries.addAll(t);
                insertIndex = i + 1;
                closeIndex = i + t.size();
            }
        }
        if (!hasInsertedBeforeOrAfterElements) {
            mutatedEntries.addAll(t);
        }
        return new ListConcat<>(mutatedEntries, insertIndex, closeIndex);
    }

    @Value
    private static class ListConcat<T> {
        List<T> ls;
        int firstNewlyAddedItemIndex;
        int lastNewlyAddedItemIndex;
    }

    private String substringOfBeforeFirstLineBreak(String s) {
        String[] lines = LINE_BREAK.split(s);
        return lines.length > 0 ? lines[0] : "";
    }

    private String substringOfAfterFirstLineBreak(String s) {
        String[] lines = LINE_BREAK.split(s, -1);
        return lines.length > 1 ? String.join(linebreak(), Arrays.copyOfRange(lines, 1, lines.length)) : "";
    }

    private int calculateMultilineIndent(Yaml.Mapping.Entry entry) {
        String[] lines = LINE_BREAK.split(entry.getPrefix(), -1);
        int keyIndent = (lines.length > 1 ? lines[lines.length - 1] : "").length();
        int indent = minCommonIndentLevel(substringOfAfterFirstLineBreak(((Yaml.Scalar) entry.getValue()).getValue()));
        return Math.max(indent - keyIndent, 0);
    }

    private boolean preserveDocumentSeparator(Yaml.Document document) {
        // Check if this document is part of a multi-document YAML with a following explicit document
        Yaml.Documents documents = getCursor().firstEnclosing(Yaml.Documents.class);
        if (documents != null) {
            int currentIndex = documents.getDocuments().indexOf(document);
            // Preserve a newline before the next document separator
            if (0 <= currentIndex && currentIndex < documents.getDocuments().size() - 1) {
                return documents.getDocuments().get(currentIndex + 1).isExplicit();
            }
            // Or if this is the last document and it has an explicit end
            return currentIndex == documents.getDocuments().size() - 1 && document.getEnd().isExplicit();
        }
        return false;
    }
}
