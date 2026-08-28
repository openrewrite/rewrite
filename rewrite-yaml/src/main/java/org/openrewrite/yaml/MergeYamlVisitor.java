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
import org.openrewrite.yaml.trait.BlockScalar;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.openrewrite.Cursor.ROOT_VALUE;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.ListUtils.*;
import static org.openrewrite.internal.StringUtils.*;
import static org.openrewrite.yaml.MergeYaml.InsertMode.*;
import static org.openrewrite.yaml.MergeYaml.REMOVE_DOCUMENT_PREFIX;
import static org.openrewrite.yaml.MergeYaml.REMOVE_PREFIX;

/**
 * Visitor class to merge two yaml files.
 * <p>
 * Apply this visitor to the enclosing {@link Yaml.Documents} or {@link Yaml.Document} rather than to just the block
 * being merged into; an inline comment that has to move onto a newly inserted element is stored on an element that
 * follows the merged block, which can be an element much higher in the tree, and is left behind otherwise.
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
    public Yaml visitDocument(Yaml.Document document, P p) {
        Yaml y = super.visitDocument(document, p);
        if (!(y instanceof Yaml.Document)) {
            return y;
        }
        Yaml.Document d = (Yaml.Document) y;
        if (hasMessage(REMOVE_DOCUMENT_PREFIX)) {
            d = d.withPrefix("");
        }
        if (hasMessage(REMOVE_PREFIX)) {
            d = removeInlineCommentFromEnd(d);
            if (d.getEnd().getPrefix().isEmpty() && preserveDocumentSeparator(document)) {
                d = d.withEnd(d.getEnd().withPrefix(linebreak()));
            }
        }
        return relocateDocumentEndComment(document, d, null);
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

            Map<UUID, BoundaryRepair> repairs = getCursor().pollMessage(SIBLING_BOUNDARY_REPAIR);
            if (repairs != null) {
                mapping = applySiblingBoundaryRepair(mapping, repairs);
            }

            if (hasMessage(REMOVE_PREFIX)) {
                List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) getCursor().getValue()).getEntries();
                return mapping.withEntries(mapLast(mapping.getEntries(), it ->
                        it.withPrefix(linebreak() + substringOfAfterFirstLineBreak(entries.get(entries.size() - 1).getPrefix()))));
            }

            return mapping;
        }
        Yaml y = super.visitMapping(existingMapping, p);
        if (y instanceof Yaml.Mapping && hasMessage(REMOVE_PREFIX)) {
            return removeInlineCommentFromLastEntry((Yaml.Mapping) y);
        }
        return y;
    }

    private boolean hasMessage(String key) {
        return getCursor().getMessage(key, false);
    }

    private Yaml.Mapping applySiblingBoundaryRepair(Yaml.Mapping mapping, Map<UUID, BoundaryRepair> repairs) {
        List<Yaml.Mapping.Entry> patched = new ArrayList<>(mapping.getEntries());
        String lineBreak = linebreak();
        for (int i = 0; i < patched.size() - 1; i++) {
            if (repairs.get(patched.get(i).getId()) == BoundaryRepair.BLOCK_TO_PLAIN) {
                Yaml.Mapping.Entry next = patched.get(i + 1);
                if (!next.getPrefix().startsWith("\n") && !next.getPrefix().startsWith("\r")) {
                    patched.set(i + 1, next.withPrefix(lineBreak + next.getPrefix()));
                }
            }
        }
        return mapping.withEntries(patched);
    }

    private static boolean keyMatches(Yaml.Mapping.@Nullable Entry e1, Yaml.Mapping.@Nullable Entry e2) {
        return e1 != null && e2 != null && e1.getKey().getValue().equals(e2.getKey().getValue());
    }

    private boolean keyMatches(Yaml.Mapping existingMapping, Yaml.Mapping incomingMapping) {
        Optional<String> nameToAdd = incomingMapping.getEntries().stream()
                .filter(e -> objectIdentifyingProperty != null && objectIdentifyingProperty.equals(e.getKey().getValue()))
                .map(e -> ((Yaml.Scalar) e.getValue()).getValue())
                .findAny();

        return nameToAdd.map(nameToAddValue -> existingMapping.getEntries().stream()
                        .filter(e -> objectIdentifyingProperty.equals(e.getKey().getValue()))
                        .map(e -> ((Yaml.Scalar) e.getValue()).getValue())
                        .anyMatch(existingName -> existingName.equals(nameToAddValue)))
                .orElse(false);
    }

    private Yaml.Mapping mergeMapping(Yaml.Mapping existingMapping, Yaml.Mapping incomingMapping, P p, Cursor cursor) {
        List<Yaml.Mapping.Entry> mergedEntries = mergeExistingEntries(existingMapping, incomingMapping, p, cursor);
        List<Yaml.Mapping.Entry> newEntries = prepareNewEntries(existingMapping, incomingMapping, p, cursor);
        ListConcat<Yaml.Mapping.Entry> mutatedEntries = concatAll(mergedEntries, newEntries, it -> it.getKey().getValue());

        if (existingMapping.getEntries().size() < mutatedEntries.entries.size() && !getCursor().isRoot()) {
            repairPrefixesAfterInsertion(mutatedEntries);
        }

        if (insertMode != Before) {
            repairPrefixesAfterExpandedChildren(existingMapping.getEntries(), mutatedEntries.entries);
        }

        return existingMapping.withEntries(mutatedEntries.entries);
    }

    private List<Yaml.Mapping.Entry> mergeExistingEntries(Yaml.Mapping existingMapping, Yaml.Mapping incomingMapping, P p, Cursor cursor) {
        List<Yaml.Mapping.Entry> mergedEntries = existingMapping.getEntries();
        for (int i = 0; i < existingMapping.getEntries().size(); i++) {
            Yaml.Mapping.Entry existingEntry = existingMapping.getEntries().get(i);
            for (Yaml.Mapping.Entry incomingEntry : incomingMapping.getEntries()) {
                if (keyMatches(existingEntry, incomingEntry)) {
                    Yaml.Block value = incomingValueFor(incomingEntry, p);
                    Yaml merged = new MergeYamlVisitor<>(existingEntry.getValue(), value, acceptTheirs,
                            objectIdentifyingProperty, shouldAutoFormat, insertMode, insertProperty)
                            .visitNonNull(existingEntry.getValue(), p, new Cursor(cursor, existingEntry));
                    Yaml.Mapping.Entry mergedEntry = existingEntry.withValue((Yaml.Block) merged);
                    if (mergedEntry != existingEntry) {
                        if (mergedEntries == existingMapping.getEntries()) {
                            mergedEntries = new ArrayList<>(existingMapping.getEntries());
                        }
                        mergedEntries.set(i, mergedEntry);
                    }
                    break;
                }
            }
        }
        return mergedEntries;
    }

    private Yaml.Block incomingValueFor(Yaml.Mapping.Entry incomingEntry, P p) {
        Yaml.Block value = incomingEntry.getValue();
        if (shouldAutoFormat && value instanceof Yaml.Scalar && hasLineBreak(((Yaml.Scalar) value).getValue())) {
            MultilineScalarChanged marker = new MultilineScalarChanged(randomId(), false, calculateMultilineIndent(incomingEntry));
            value = autoFormat(value.withMarkers(value.getMarkers().add(marker)), p);
        }
        return value;
    }

    private List<Yaml.Mapping.Entry> prepareNewEntries(Yaml.Mapping existingMapping, Yaml.Mapping incomingMapping, P p, Cursor cursor) {
        int existingIndent = shouldAutoFormat ? blockIndent(existingMapping) : -1;
        List<Yaml.Mapping.Entry> newEntries = new ArrayList<>(incomingMapping.getEntries().size());
        for (Yaml.Mapping.Entry incomingEntry : incomingMapping.getEntries()) {
            boolean alreadyExists = false;
            for (Yaml.Mapping.Entry existingEntry : existingMapping.getEntries()) {
                if (keyMatches(existingEntry, incomingEntry)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (alreadyExists) {
                continue;
            }

            Yaml.Mapping.Entry entry = markAddedMultilineEntry(incomingEntry);
            if (!shouldAutoFormat) {
                newEntries.add(entry);
                continue;
            }
            newEntries.add(alignToIndent(autoFormat(entry, p, cursor), existingIndent));
        }
        return newEntries;
    }

    private Yaml.Mapping.Entry markAddedMultilineEntry(Yaml.Mapping.Entry entry) {
        if (shouldAutoFormat && entry.getValue() instanceof Yaml.Scalar &&
                hasLineBreak(((Yaml.Scalar) entry.getValue()).getValue())) {
            MultilineScalarChanged marker = new MultilineScalarChanged(randomId(), true, calculateMultilineIndent(entry));
            return entry.withValue(entry.getValue().withMarkers(entry.getValue().getMarkers().add(marker)));
        }
        return entry;
    }

    private void repairPrefixesAfterInsertion(ListConcat<Yaml.Mapping.Entry> mutatedEntries) {
        if (mutatedEntries.lastNewlyAddedIndex != -1 &&
                mutatedEntries.lastNewlyAddedIndex < mutatedEntries.entries.size() - 1) {
            repairInsertedEntryPrefixes(mutatedEntries);
        } else {
            relocateTrailingComment(mutatedEntries);
        }
    }

    private void repairInsertedEntryPrefixes(ListConcat<Yaml.Mapping.Entry> mutatedEntries) {
        Yaml.Mapping.Entry afterInsertEntry = mutatedEntries.entries.get(mutatedEntries.lastNewlyAddedIndex + 1);
        if (isFirstEntryAtDocumentRoot(mutatedEntries)) {
            Yaml.Document document = getCursor().getParentOrThrow().getValue();
            mutatedEntries.entries.set(mutatedEntries.firstNewlyAddedIndex, mutatedEntries.entries.get(0).withPrefix(""));
            mutatedEntries.entries.set(mutatedEntries.lastNewlyAddedIndex + 1,
                    afterInsertEntry.withPrefix(linebreak() + document.getPrefix() + afterInsertEntry.getPrefix()));
            getCursor().getParentOrThrow().putMessage(REMOVE_DOCUMENT_PREFIX, true);
            return;
        }

        Yaml.Mapping.Entry firstNewlyAddedEntry = mutatedEntries.entries.get(mutatedEntries.firstNewlyAddedIndex);
        String partOne = substringOfBeforeFirstLineBreak(afterInsertEntry.getPrefix());
        String partTwo = substringOfAfterFirstLineBreak(afterInsertEntry.getPrefix());

        if (insertMode == Before && partOne.isEmpty() && hasLineBreak(partTwo) && !partTwo.contains("#")) {
            mutatedEntries.entries.set(mutatedEntries.lastNewlyAddedIndex + 1,
                    afterInsertEntry.withPrefix(firstNewlyAddedEntry.getPrefix()));
            mutatedEntries.entries.set(mutatedEntries.firstNewlyAddedIndex,
                    firstNewlyAddedEntry.withPrefix(afterInsertEntry.getPrefix()));
            return;
        }

        String newFirstPrefix = partOne + firstNewlyAddedEntry.getPrefix();
        if (afterInsertEntry.getPrefix().isEmpty() && partOne.isEmpty() && newFirstPrefix.startsWith("\n")) {
            // Remove leading newline since the previous element already provides line separation
            newFirstPrefix = newFirstPrefix.substring(1);
        }

        mutatedEntries.entries.set(mutatedEntries.firstNewlyAddedIndex, firstNewlyAddedEntry.withPrefix(newFirstPrefix));
        mutatedEntries.entries.set(mutatedEntries.lastNewlyAddedIndex + 1,
                afterInsertEntry.withPrefix(linebreak() + partTwo));
    }

    private boolean isFirstEntryAtDocumentRoot(ListConcat<Yaml.Mapping.Entry> mutatedEntries) {
        if (mutatedEntries.firstNewlyAddedIndex != 0 ||
                !(getCursor().getParentOrThrow().getValue() instanceof Yaml.Document)) {
            return false;
        }
        Yaml.Document document = getCursor().getParentOrThrow().getValue();
        return document.getBlock() instanceof Yaml.Mapping &&
                ((Yaml.Mapping) document.getBlock()).getEntries().equals(((Yaml.Mapping) existing).getEntries());
    }

    private void relocateTrailingComment(ListConcat<Yaml.Mapping.Entry> mutatedEntries) {
        CommentSource source = findTrailingComment();
        if (source == null) {
            return;
        }

        int lastIndex = mutatedEntries.entries.size() - 1;
        Yaml.Mapping.Entry last = mutatedEntries.entries.get(lastIndex);
        mutatedEntries.entries.set(lastIndex, last.withPrefix(
                source.comment + (hasLineBreak(last.getPrefix()) ? "" : linebreak()) + last.getPrefix()));
        source.cursor.putMessage(REMOVE_PREFIX, true);
    }

    private @Nullable CommentSource findTrailingComment() {
        Cursor commentCursor = findCommentCursor();
        if (commentCursor.getValue() instanceof Yaml.Document) {
            return documentEndComment(commentCursor);
        }
        if (!(commentCursor.getValue() instanceof Yaml.Mapping)) {
            return null;
        }

        return mappingComment(commentCursor, commentCursor.getValue());
    }

    private @Nullable CommentSource documentEndComment(Cursor cursor) {
        String comment = inlineCommentOf(((Yaml.Document) cursor.getValue()).getEnd().getPrefix());
        return isNotEmpty(comment) ? new CommentSource(cursor, comment) : null;
    }

    private @Nullable CommentSource mappingComment(Cursor cursor, Yaml.Mapping mapping) {
        List<Yaml.Mapping.Entry> entries = mapping.getEntries();
        String comment = null;
        boolean foundDirectSibling = false;
        for (int i = 0; i < entries.size() - 1; i++) {
            if (entries.get(i).getValue().equals(getCursor().getValue())) {
                comment = substringOfBeforeFirstLineBreak(entries.get(i + 1).getPrefix());
                foundDirectSibling = true;
                break;
            }
        }
        if (comment == null && hasLineBreak(entries.get(entries.size() - 1).getPrefix())) {
            comment = substringOfBeforeFirstLineBreak(entries.get(entries.size() - 1).getPrefix());
        }

        if (!foundDirectSibling && !isNotEmpty(comment)) {
            Cursor documentCursor = cursor.dropParentUntil(it -> ROOT_VALUE.equals(it) || it instanceof Yaml.Document);
            if (documentCursor.getValue() instanceof Yaml.Document) {
                String endComment = inlineCommentOf(((Yaml.Document) documentCursor.getValue()).getEnd().getPrefix());
                if (isNotEmpty(endComment)) {
                    return new CommentSource(documentCursor, endComment);
                }
            }
        }
        return isNotEmpty(comment) ? new CommentSource(cursor, comment) : null;
    }

    private Cursor findCommentCursor() {
        return getCursor().dropParentUntil(this::isCommentBoundary);
    }

    private boolean isCommentBoundary(Object value) {
        if (ROOT_VALUE.equals(value) || value instanceof Yaml.Document) {
            return true;
        }
        if (!(value instanceof Yaml.Mapping)) {
            return false;
        }

        List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) value).getEntries();
        return entries.size() > 1 && !entries.get(entries.size() - 1).equals(getCursor().getParentOrThrow().getValue());
    }

    private void repairPrefixesAfterExpandedChildren(List<Yaml.Mapping.Entry> existingEntries, List<Yaml.Mapping.Entry> mutatedEntries) {
        for (int i = 0; i < existingEntries.size() - 1; i++) {
            if (mappingGainedEntries(existingEntries.get(i), mutatedEntries.get(i))) {
                mutatedEntries.set(i + 1, mutatedEntries.get(i + 1).withPrefix(
                        linebreak() + substringOfAfterFirstLineBreak(mutatedEntries.get(i + 1).getPrefix())));
            }
        }
    }

    private boolean mappingGainedEntries(Yaml.Mapping.Entry existingEntry, Yaml.Mapping.Entry mutatedEntry) {
        return existingEntry.getValue() instanceof Yaml.Mapping && mutatedEntry.getValue() instanceof Yaml.Mapping &&
                ((Yaml.Mapping) existingEntry.getValue()).getEntries().size() <
                        ((Yaml.Mapping) mutatedEntry.getValue()).getEntries().size();
    }

    private Yaml.Sequence mergeSequence(Yaml.Sequence existingSequence, Yaml.Sequence incomingSequence, P p, Cursor cursor) {
        if (acceptTheirs) {
            return existingSequence;
        }

        if (incomingSequence.getEntries().stream().allMatch(entry -> entry.getBlock() instanceof Yaml.Scalar)) {
            return mergeScalarSequence(existingSequence, incomingSequence);
        }
        return mergeMappingSequence(existingSequence, incomingSequence, p, cursor);
    }

    private Yaml.Sequence mergeScalarSequence(Yaml.Sequence existingSequence, Yaml.Sequence incomingSequence) {
        List<Yaml.Sequence.Entry> incomingEntries = new ArrayList<>(incomingSequence.getEntries());
        removeExistingScalarEntries(existingSequence, incomingEntries);

        boolean isFlowStyle = existingSequence.getOpeningBracketPrefix() != null;
        List<Yaml.Sequence.Entry> newEntries = formatSequenceEntries(existingSequence, incomingEntries, isFlowStyle);
        List<Yaml.Sequence.Entry> entries = concatAll(existingSequence.getEntries(), newEntries, this::sequenceEntryKey).entries;

        if (isFlowStyle && !newEntries.isEmpty() && entries.size() > existingSequence.getEntries().size()) {
            int lastExistingIndex = existingSequence.getEntries().size() - 1;
            entries.set(lastExistingIndex, entries.get(lastExistingIndex).withTrailingCommaPrefix(""));
            entries.set(entries.size() - 1, entries.get(entries.size() - 1).withTrailingCommaPrefix(null));
        }

        return existingSequence.withEntries(entries);
    }

    private void removeExistingScalarEntries(Yaml.Sequence existingSequence, List<Yaml.Sequence.Entry> incomingEntries) {
        for (Yaml.Sequence.Entry existingEntry : existingSequence.getEntries()) {
            if (!(existingEntry.getBlock() instanceof Yaml.Scalar)) {
                continue;
            }

            String existingScalar = ((Yaml.Scalar) existingEntry.getBlock()).getValue();
            for (int i = 0; i < incomingEntries.size(); i++) {
                if (((Yaml.Scalar) incomingEntries.get(i).getBlock()).getValue().equals(existingScalar)) {
                    incomingEntries.remove(i);
                    break;
                }
            }
        }
    }

    private List<Yaml.Sequence.Entry> formatSequenceEntries(Yaml.Sequence existingSequence,
                                                             List<Yaml.Sequence.Entry> incomingEntries,
                                                             boolean flowStyle) {
        if (flowStyle) {
            List<Yaml.Sequence.Entry> formattedEntries = new ArrayList<>(incomingEntries.size());
            for (Yaml.Sequence.Entry entry : incomingEntries) {
                formattedEntries.add(entry.withPrefix("").withBlock(entry.getBlock().withPrefix(" ")).withTrailingCommaPrefix(null));
            }
            return formattedEntries;
        }

        String existingEntryPrefix = existingSequence.getEntries().get(0).getPrefix();
        String newEntryPrefix = existingEntryPrefix.substring(existingEntryPrefix.lastIndexOf('\n'));
        List<Yaml.Sequence.Entry> formattedEntries = new ArrayList<>(incomingEntries.size());
        for (Yaml.Sequence.Entry entry : incomingEntries) {
            formattedEntries.add(entry.withPrefix(newEntryPrefix));
        }
        return formattedEntries;
    }

    private String sequenceEntryKey(Yaml.Sequence.Entry entry) {
        if (entry.getBlock() instanceof Yaml.Scalar) {
            return ((Yaml.Scalar) entry.getBlock()).getValue();
        } else if (entry.getBlock() instanceof Yaml.Mapping) {
            Yaml.Mapping.Entry firstEntry = ((Yaml.Mapping) entry.getBlock()).getEntries().get(0);
            return firstEntry.getKey().getValue();
        }
        return "";
    }

    private Yaml.Sequence mergeMappingSequence(Yaml.Sequence existingSequence, Yaml.Sequence incomingSequence, P p, Cursor cursor) {
        if (objectIdentifyingProperty == null) {
            return existingSequence;
        }

        List<Yaml.Sequence.Entry> mutatedEntries = new ArrayList<>();
        for (Yaml.Sequence.Entry incomingEntry : incomingSequence.getEntries()) {
            Yaml.Mapping incomingMapping = (Yaml.Mapping) incomingEntry.getBlock();
            boolean matched = false;
            for (Yaml.Sequence.Entry existingEntry : existingSequence.getEntries()) {
                Yaml.Mapping existingMapping = (Yaml.Mapping) existingEntry.getBlock();
                if (keyMatches(existingMapping, incomingMapping)) {
                    matched = true;
                    Yaml.Sequence.Entry mergedEntry = existingEntry.withBlock(mergeMapping(existingMapping, incomingMapping, p, cursor));
                    if (mergedEntry != existingEntry) {
                        mutatedEntries.add(mergedEntry);
                    }
                    break;
                }
            }
            if (!matched) {
                mutatedEntries.add(incomingEntry);
            }
        }

        List<Yaml.Sequence.Entry> formattedEntries = new ArrayList<>(mutatedEntries.size());
        for (Yaml.Sequence.Entry entry : mutatedEntries) {
            formattedEntries.add(autoFormat(entry, p, cursor));
        }

        List<Yaml.Sequence.Entry> entries = concatAll(
                filter(existingSequence.getEntries(), it -> !mutatedEntries.contains(it)),
                formattedEntries,
                it -> {
                    Yaml.Mapping.Entry entry = ((Yaml.Mapping) it.getBlock()).getEntries().get(0);
                    return entry.getKey().getValue() + ": " + ((Yaml.Scalar) entry.getValue()).getValue();
                }).entries;

        return existingSequence.withEntries(entries);
    }

    private Yaml.Scalar mergeScalar(Yaml.Scalar existingScalar, Yaml.Scalar incomingScalar) {
        BlockScalar.Matcher matcher = new BlockScalar.Matcher();
        BlockScalar existingBs = matcher.get(new Cursor(null, existingScalar)).orElse(null);
        BlockScalar incomingBs = matcher.get(new Cursor(null, incomingScalar)).orElse(null);
        String existingValue = existingBs != null ? existingBs.getBody() : existingScalar.getValue();
        String incomingValue = incomingBs != null ? incomingBs.getBody() : incomingScalar.getValue();
        if (existingValue.equals(incomingValue) && existingScalar.getStyle() == incomingScalar.getStyle() || acceptTheirs) {
            return existingScalar;
        }
        // Adopt the incoming scalar's format.
        if (incomingBs != null) {
            if (existingBs != null && existingScalar.getStyle() == incomingScalar.getStyle()) {
                return existingBs.withBody(incomingValue);
            }
            recordBoundaryRepair(BoundaryRepair.PLAIN_TO_BLOCK);
            return existingScalar.withStyle(incomingScalar.getStyle()).withValue(incomingScalar.getValue());
        }
        if (existingBs != null) {
            recordBoundaryRepair(BoundaryRepair.BLOCK_TO_PLAIN);
        }
        return existingScalar.withStyle(incomingScalar.getStyle()).withValue(incomingScalar.getValue());
    }

    private enum BoundaryRepair { BLOCK_TO_PLAIN, PLAIN_TO_BLOCK }

    private void recordBoundaryRepair(BoundaryRepair repair) {
        Cursor entryCursor = getCursor().dropParentUntil(v -> v == ROOT_VALUE || v instanceof Yaml.Mapping.Entry);
        if (!(entryCursor.getValue() instanceof Yaml.Mapping.Entry)) {
            return;
        }
        Cursor mappingCursor = entryCursor.dropParentUntil(v -> v == ROOT_VALUE || v instanceof Yaml.Mapping);
        if (!(mappingCursor.getValue() instanceof Yaml.Mapping)) {
            return;
        }
        Map<UUID, BoundaryRepair> repairs = mappingCursor.getMessage(SIBLING_BOUNDARY_REPAIR);
        if (repairs == null) {
            repairs = new HashMap<>();
            mappingCursor.putMessage(SIBLING_BOUNDARY_REPAIR, repairs);
        }
        repairs.put(((Yaml.Mapping.Entry) entryCursor.getValue()).getId(), repair);
    }

    private static final String SIBLING_BOUNDARY_REPAIR = "org.openrewrite.yaml.MergeYamlVisitor.siblingBoundaryRepair";

    /**
     * Concatenates entries while honoring the configured insertion point.
     */
    private <T> ListConcat<T> concatAll(List<T> existingEntries, List<T> newEntries, Function<T, String> getKey) {
        if (insertMode == null || insertMode == Last || insertProperty == null || newEntries.isEmpty()) {
            return new ListConcat<>(ListUtils.concatAll(existingEntries, newEntries), -1, -1);
        }

        List<T> mergedEntries = new ArrayList<>();
        boolean inserted = false;
        int firstNewlyAddedIndex = -1;
        int lastNewlyAddedIndex = -1;
        for (int i = 0; i < existingEntries.size(); i++) {
            T existingEntry = existingEntries.get(i);
            if (!inserted && insertMode == Before && insertProperty.equals(getKey.apply(existingEntry))) {
                inserted = true;
                mergedEntries.addAll(newEntries);
                firstNewlyAddedIndex = i;
                lastNewlyAddedIndex = i + newEntries.size() - 1;
            }
            mergedEntries.add(existingEntry);
            if (!inserted && insertMode == After && insertProperty.equals(getKey.apply(existingEntry))) {
                inserted = true;
                mergedEntries.addAll(newEntries);
                firstNewlyAddedIndex = i + 1;
                lastNewlyAddedIndex = i + newEntries.size();
            }
        }
        if (!inserted) {
            mergedEntries.addAll(newEntries);
        }
        return new ListConcat<>(mergedEntries, firstNewlyAddedIndex, lastNewlyAddedIndex);
    }

    @Value
    private static class ListConcat<T> {
        List<T> entries;
        int firstNewlyAddedIndex;
        int lastNewlyAddedIndex;
    }

    @Value
    private static class CommentSource {
        Cursor cursor;
        String comment;
    }

    private String substringOfBeforeFirstLineBreak(String s) {
        String[] lines = LINE_BREAK.split(s);
        return lines.length > 0 ? lines[0] : "";
    }

    private String substringOfAfterFirstLineBreak(String s) {
        String[] lines = LINE_BREAK.split(s, -1);
        return lines.length > 1 ? String.join(linebreak(), Arrays.copyOfRange(lines, 1, lines.length)) : "";
    }

    private static String inlineCommentOf(@Nullable String prefix) {
        if (prefix == null) {
            return "";
        }
        String[] lines = LINE_BREAK.split(prefix, -1);
        String firstLine = lines.length > 0 ? lines[0] : "";
        return firstLine.contains("#") ? firstLine : "";
    }

    /**
     * Relocates an inline comment stored on the end of a document after a merge changes the element
     * that originally followed the comment's owner. Prefixes are the YAML tree's representation of
     * the whitespace and comments between adjacent elements, so placing the comment on the new
     * successor preserves its inline relationship with the original value.
     *
     * @param before          The document tree before the merge.
     * @param after           The document tree after the merge.
     * @param movedEntry      A copied entry to use when the original comment owner was moved.
     */
    static Yaml.Documents relocateDocumentEndComment(Yaml.Documents before, Yaml.Documents after, Yaml.Mapping.@Nullable Entry movedEntry) {
        if (before.getDocuments().isEmpty() || after.getDocuments().isEmpty()) {
            return after;
        }

        List<Yaml.Document> relocatedDocuments = new ArrayList<>(after.getDocuments());
        boolean changed = false;
        int documentCount = Math.min(before.getDocuments().size(), relocatedDocuments.size());
        for (int i = 0; i < documentCount; i++) {
            Yaml.Document beforeDocument = before.getDocuments().get(i);
            Yaml.Document relocated = relocateDocumentEndComment(
                    beforeDocument,
                    relocatedDocuments.get(i),
                    movedEntry
            );
            if (relocated != relocatedDocuments.get(i)) {
                relocatedDocuments.set(i, relocated);
                changed = true;
            }
        }
        return changed ? after.withDocuments(relocatedDocuments) : after;
    }

    private static Yaml.Document relocateDocumentEndComment(Yaml.Document before, Yaml.Document after, Yaml.Mapping.@Nullable Entry movedEntry) {
        String comment = inlineCommentOf(before.getEnd().getPrefix());
        if (comment.isEmpty()) {
            return after;
        }

        UUID originalOwnerId = finalOwnerId(before.getBlock());
        if (originalOwnerId == null) {
            return after;
        }

        String linebreak = linebreakFor(after);
        RelocationResult relocation = relocateToSuccessor(after.getBlock(), originalOwnerId, comment, linebreak);
        if (!relocation.ownerFound && movedEntry != null) {
            UUID movedOwnerId = finalOwnerId(movedEntry);
            relocation = relocateToSuccessor(after.getBlock(), movedOwnerId, comment, linebreak);
        }

        if (!relocation.relocated) {
            return after;
        }

        String endPrefix = after.getEnd().getPrefix();
        String withoutComment = sameInlineComment(endPrefix, comment) ? removeInlineComment(endPrefix) : endPrefix;
        if (endPrefix.equals(withoutComment)) {
            return relocation.block == after.getBlock() ? after : after.withBlock(relocation.block);
        }
        return after.withBlock(relocation.block).withEnd(after.getEnd().withPrefix(withoutComment));
    }

    private static boolean sameInlineComment(String prefix, String comment) {
        String existingComment = inlineCommentOf(prefix);
        return comment.equals(existingComment) || comment.trim().equals(existingComment.trim());
    }

    private static RelocationResult relocateToSuccessor(Yaml.Block block, UUID ownerId, String comment, String linebreak) {
        SuccessorPrefixVisitor visitor = new SuccessorPrefixVisitor(ownerId, comment, linebreak);
        Yaml.Block relocated = (Yaml.Block) visitor.visitNonNull(block, 0);
        return new RelocationResult(relocated, visitor.ownerFound, visitor.relocated);
    }

    private static @Nullable UUID finalOwnerId(Yaml.Block block) {
        if (block instanceof Yaml.Mapping) {
            List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) block).getEntries();
            return entries.isEmpty() ? null : finalOwnerId(entries.get(entries.size() - 1));
        } else if (block instanceof Yaml.Sequence) {
            List<Yaml.Sequence.Entry> entries = ((Yaml.Sequence) block).getEntries();
            return entries.isEmpty() ? null : finalOwnerId(entries.get(entries.size() - 1));
        }
        return block.getId();
    }

    private static UUID finalOwnerId(Yaml.Mapping.Entry entry) {
        Yaml.Block value = entry.getValue();
        if (hasEntries(value)) {
            UUID ownerId = finalOwnerId(value);
            if (ownerId != null) {
                return ownerId;
            }
        }
        return entry.getId();
    }

    private static UUID finalOwnerId(Yaml.Sequence.Entry entry) {
        Yaml.Block block = entry.getBlock();
        if (hasEntries(block)) {
            UUID ownerId = finalOwnerId(block);
            if (ownerId != null) {
                return ownerId;
            }
        }
        return entry.getId();
    }

    private static boolean hasEntries(Yaml.Block block) {
        return block instanceof Yaml.Mapping && !((Yaml.Mapping) block).getEntries().isEmpty() ||
                block instanceof Yaml.Sequence && !((Yaml.Sequence) block).getEntries().isEmpty();
    }

    private static String linebreakFor(Yaml.Document document) {
        String linebreak = linebreakOf(document.getEnd().getPrefix());
        if (linebreak != null) {
            return linebreak;
        }
        linebreak = linebreakOf(document.getBlock());
        return linebreak == null ? "\n" : linebreak;
    }

    private static @Nullable String linebreakOf(Yaml yaml) {
        String linebreak = linebreakOf(yaml.getPrefix());
        if (linebreak != null) {
            return linebreak;
        }
        if (yaml instanceof Yaml.Mapping) {
            return linebreakOf(((Yaml.Mapping) yaml).getEntries(), Yaml.Mapping.Entry::getValue);
        } else if (yaml instanceof Yaml.Sequence) {
            return linebreakOf(((Yaml.Sequence) yaml).getEntries(), Yaml.Sequence.Entry::getBlock);
        }
        return null;
    }

    private static <T extends Yaml> @Nullable String linebreakOf(List<T> entries, Function<T, Yaml.Block> child) {
        for (T entry : entries) {
            String linebreak = linebreakOf(entry);
            if (linebreak != null) {
                return linebreak;
            }
            linebreak = linebreakOf(child.apply(entry));
            if (linebreak != null) {
                return linebreak;
            }
        }
        return null;
    }

    private static @Nullable String linebreakOf(String text) {
        if (text.contains("\r\n")) {
            return "\r\n";
        } else if (text.contains("\r")) {
            return "\r";
        } else if (text.contains("\n")) {
            return "\n";
        }
        return null;
    }

    @Value
    private static class RelocationResult {
        Yaml.Block block;
        boolean ownerFound;
        boolean relocated;
    }

    private static class SuccessorPrefixVisitor extends YamlIsoVisitor<Integer> {
        private final UUID ownerId;
        private final String comment;
        private final String linebreak;
        private boolean ownerFound;
        private boolean relocated;

        private SuccessorPrefixVisitor(UUID ownerId, String comment, String linebreak) {
            this.ownerId = ownerId;
            this.comment = comment;
            this.linebreak = linebreak;
        }

        @Override
        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Integer p) {
            boolean isSuccessor = ownerFound && !relocated;
            if (isSuccessor) {
                relocated = true;
                return super.visitMappingEntry(addCommentToSuccessor(entry), p);
            }
            Yaml.Mapping.Entry visited = super.visitMappingEntry(entry, p);
            if (ownerId.equals(entry.getId())) {
                ownerFound = true;
            }
            return visited;
        }

        @Override
        public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, Integer p) {
            boolean isSuccessor = ownerFound && !relocated;
            if (isSuccessor) {
                relocated = true;
                return super.visitSequenceEntry(addCommentToSuccessor(entry), p);
            }
            Yaml.Sequence.Entry visited = super.visitSequenceEntry(entry, p);
            if (ownerId.equals(entry.getId())) {
                ownerFound = true;
            }
            return visited;
        }

        private Yaml.Mapping.Entry addCommentToSuccessor(Yaml.Mapping.Entry entry) {
            String prefix = prefixWithComment(entry.getPrefix());
            return entry.getPrefix().equals(prefix) ? entry :
                    new Yaml.Mapping.Entry(entry.getId(), prefix, entry.getMarkers(), entry.getKey(),
                            entry.getBeforeMappingValueIndicator(), entry.getValue());
        }

        private Yaml.Sequence.Entry addCommentToSuccessor(Yaml.Sequence.Entry entry) {
            String prefix = prefixWithComment(entry.getPrefix());
            return entry.getPrefix().equals(prefix) ? entry :
                    new Yaml.Sequence.Entry(entry.getId(), prefix, entry.getMarkers(), entry.getBlock(),
                            entry.isDash(), entry.getTrailingCommaPrefix());
        }

        private String prefixWithComment(String prefix) {
            if (sameInlineComment(prefix, comment)) {
                return prefix;
            }
            return comment + (LINE_BREAK.matcher(prefix).find() ? "" : linebreak) + prefix;
        }
    }

    /**
     * Strips the inline comment that was copied onto a newly inserted entry from the prefix of the
     * last entry of the mapping it was copied from, so that it is not rendered twice.
     */
    static Yaml.Mapping removeInlineCommentFromLastEntry(Yaml.Mapping mapping) {
        List<Yaml.Mapping.Entry> entries = mapping.getEntries();
        if (entries.isEmpty()) {
            return mapping;
        }

        int lastIndex = entries.size() - 1;
        Yaml.Mapping.Entry lastEntry = entries.get(lastIndex);
        String prefix = removeInlineComment(lastEntry.getPrefix());
        if (prefix.equals(lastEntry.getPrefix())) {
            return mapping;
        }

        List<Yaml.Mapping.Entry> updatedEntries = new ArrayList<>(entries);
        updatedEntries.set(lastIndex, lastEntry.withPrefix(prefix));
        return mapping.withEntries(updatedEntries);
    }

    /**
     * Strips the inline comment from the prefix of the document end, which is where a comment on the
     * last line of a document is stored. Later lines are retained.
     */
    static Yaml.Document removeInlineCommentFromEnd(Yaml.Document document) {
        String prefix = document.getEnd().getPrefix();
        String withoutComment = removeInlineComment(prefix);
        return prefix.equals(withoutComment) ? document : document.withEnd(document.getEnd().withPrefix(withoutComment));
    }

    private static String removeInlineComment(String prefix) {
        String[] lines = LINE_BREAK.split(prefix, -1);
        if (lines.length <= 1) {
            return lines.length == 1 && lines[0].contains("#") ? "" : prefix;
        }
        String linebreak = linebreakOf(prefix);
        if (linebreak == null) {
            linebreak = "\n";
        }
        return linebreak + String.join(linebreak, Arrays.copyOfRange(lines, 1, lines.length));
    }

    /**
     * The indentation column shared by the entries of an existing mapping, or {@code -1} when it
     * cannot be determined (e.g. an empty mapping or a mapping whose only entry is on the same line
     * as its parent key).
     */
    private static int blockIndent(Yaml.Mapping mapping) {
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            int indent = lastLineIndent(entry.getPrefix());
            if (indent >= 0) {
                return indent;
            }
        }
        return -1;
    }

    /**
     * Re-indents a newly added entry (and its nested content) so it lines up with the existing
     * sibling entries, preserving the relative indentation produced by {@code autoFormat}.
     */
    private Yaml.Mapping.Entry alignToIndent(Yaml.Mapping.Entry entry, int targetIndent) {
        if (targetIndent < 0) {
            return entry;
        }
        return (Yaml.Mapping.Entry) ShiftIndentVisitor.<Integer>toIndent(entry, targetIndent).visitNonNull(entry, 0);
    }

    /**
     * The number of whitespace characters after the last line break of a prefix, or {@code -1} when
     * the prefix has no line break (i.e. the element is not on its own line).
     */
    private static int lastLineIndent(String prefix) {
        int idx = Math.max(prefix.lastIndexOf('\n'), prefix.lastIndexOf('\r'));
        return idx < 0 ? -1 : prefix.length() - idx - 1;
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
