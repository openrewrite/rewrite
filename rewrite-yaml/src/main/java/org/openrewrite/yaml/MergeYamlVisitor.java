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

            java.util.Map<java.util.UUID, BoundaryRepair> repairs = getCursor().pollMessage(SIBLING_BOUNDARY_REPAIR);
            if (repairs != null) {
                mapping = applySiblingBoundaryRepair(mapping, repairs);
            }

            if (getCursor().getMessage(REMOVE_PREFIX, false)) {
                List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) getCursor().getValue()).getEntries();
                return mapping.withEntries(mapLast(mapping.getEntries(), it ->
                        it.withPrefix(linebreak() + substringOfAfterFirstLineBreak(entries.get(entries.size() - 1).getPrefix()))));
            }

            return mapping;
        }
        return super.visitMapping(existingMapping, p);
    }

    // When mergeScalar swaps between block and plain styles, the sibling-boundary linebreak moves
    // from the block scalar's trailing content to the next entry's prefix (or vice versa). Apply
    // that shift here where we have entry-neighbour context.
    private Yaml.Mapping applySiblingBoundaryRepair(Yaml.Mapping mapping, java.util.Map<java.util.UUID, BoundaryRepair> repairs) {
        List<Yaml.Mapping.Entry> entries = mapping.getEntries();
        List<Yaml.Mapping.Entry> patched = new ArrayList<>(entries);
        String lineBreak = linebreak();
        for (int i = 0; i < patched.size() - 1; i++) {
            BoundaryRepair repair = repairs.get(patched.get(i).getId());
            if (repair == null) {
                continue;
            }
            Yaml.Mapping.Entry next = patched.get(i + 1);
            if (repair == BoundaryRepair.BLOCK_TO_PLAIN) {
                if (!next.getPrefix().startsWith("\n") && !next.getPrefix().startsWith("\r")) {
                    patched.set(i + 1, next.withPrefix(lineBreak + next.getPrefix()));
                }
            }
            // PLAIN_TO_BLOCK: the next entry's prefix already carries the sibling linebreak and
            // the block scalar's own value may or may not end with one; leave the next entry's
            // prefix alone rather than risk gluing siblings together.
        }
        return mapping.withEntries(patched);
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

        // The indentation of the existing entries, so newly added entries can be aligned to match them.
        // `autoFormat` derives indentation from the file's auto-detected indent size, which does not
        // necessarily match a block whose actual indentation deviates from that size.
        int existingIndent = shouldAutoFormat ? blockIndent(m1) : -1;

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
            if (!shouldAutoFormat) {
                return it;
            }
            return alignToIndent((Yaml.Mapping.Entry) autoFormat(it, p, cursor), existingIndent);
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

                    String newFirstPrefix = partOne + firstNewlyAddedEntry.getPrefix();
                    if (afterInsertEntry.getPrefix().isEmpty() && partOne.isEmpty() && newFirstPrefix.startsWith("\n")) {
                        // Remove leading newline since the previous element already provides line separation
                        newFirstPrefix = newFirstPrefix.substring(1);
                    }

                    mutatedEntries.ls.set(mutatedEntries.firstNewlyAddedItemIndex, firstNewlyAddedEntry.withPrefix(newFirstPrefix));
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
                    boolean foundDirectSibling = false;
                    for (int i = 0; i < entries.size() - 1; i++) {
                        if (entries.get(i).getValue().equals(getCursor().getValue())) {
                            comment = substringOfBeforeFirstLineBreak(entries.get(i + 1).getPrefix());
                            foundDirectSibling = true;
                            break;
                        }
                    }
                    // OR retrieve it for last item from next element (could potentially be much higher in the tree).
                    if (comment == null && hasLineBreak(entries.get(entries.size() - 1).getPrefix())) {
                        comment = substringOfBeforeFirstLineBreak(entries.get(entries.size() - 1).getPrefix());
                    }

                    // If the current mapping is not a direct child of the found parent mapping,
                    // fall back to the Document.End prefix. This handles the case where the mapping
                    // being merged is deeply nested (e.g., inside a sequence entry) and the inline
                    // comment is stored on the Document.End node.
                    if (!foundDirectSibling && !isNotEmpty(comment)) {
                        Cursor docCursor = c.dropParentUntil(it -> ROOT_VALUE.equals(it) || it instanceof Yaml.Document);
                        if (docCursor.getValue() instanceof Yaml.Document) {
                            Yaml.Document doc = docCursor.getValue();
                            if (!preserveDocumentSeparator(doc)) {
                                String endPrefix = doc.getEnd().getPrefix();
                                // Only use Document.End prefix if it contains a comment;
                                // plain trailing whitespace should not be copied as a comment
                                if (endPrefix != null && endPrefix.contains("#")) {
                                    comment = endPrefix;
                                    c = docCursor;
                                }
                            }
                        }
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

            boolean isFlowStyle = s1.getOpeningBracketPrefix() != null;
            List<Yaml.Sequence.Entry> newEntries;
            if (isFlowStyle) {
                newEntries = ListUtils.map(incomingEntries, it ->
                        it.withPrefix("").withBlock(it.getBlock().withPrefix(" ")).withTrailingCommaPrefix(null));
            } else {
                String existingEntryPrefix = s1.getEntries().get(0).getPrefix();
                String newEntryPrefix = existingEntryPrefix.substring(existingEntryPrefix.lastIndexOf('\n'));
                newEntries = ListUtils.map(incomingEntries, it -> it.withPrefix(newEntryPrefix));
            }
            List<Yaml.Sequence.Entry> mutatedEntries = concatAll(s1.getEntries(), newEntries, it -> {
                if (it.getBlock() instanceof Yaml.Scalar) {
                    return ((Yaml.Scalar) it.getBlock()).getValue();
                } else if (it.getBlock() instanceof Yaml.Mapping) {
                    Yaml.Mapping.Entry entry = ((Yaml.Mapping) it.getBlock()).getEntries().get(0);
                    return entry.getKey().getValue();
                }
                return "";
            }).ls;

            // For flow-style sequences, ensure commas are correct: add a trailing comma
            // on the entry before the first new entry, and remove any trailing comma from the last entry
            if (isFlowStyle && !newEntries.isEmpty() && mutatedEntries.size() > s1.getEntries().size()) {
                int lastExistingIdx = s1.getEntries().size() - 1;
                mutatedEntries.set(lastExistingIdx, mutatedEntries.get(lastExistingIdx).withTrailingCommaPrefix(""));
                int lastIdx = mutatedEntries.size() - 1;
                mutatedEntries.set(lastIdx, mutatedEntries.get(lastIdx).withTrailingCommaPrefix(null));
            }

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
        BlockScalar.Matcher matcher = new BlockScalar.Matcher();
        BlockScalar existingBs = matcher.get(new Cursor(null, y1)).orElse(null);
        BlockScalar incomingBs = matcher.get(new Cursor(null, y2)).orElse(null);
        String s1 = existingBs != null ? existingBs.getBody() : y1.getValue();
        String s2 = incomingBs != null ? incomingBs.getBody() : y2.getValue();
        if (s1.equals(s2) && y1.getStyle() == y2.getStyle() || acceptTheirs) {
            return y1;
        }
        // Adopt the incoming scalar's format: the incoming snippet's style wins.
        if (incomingBs != null) {
            // Result is a block scalar. If existing was also a block scalar with the same
            // header/style, preserve its envelope (chomp indicator, indent, trailing whitespace
            // bounding the next sibling) and swap only the body.
            if (existingBs != null && y1.getStyle() == y2.getStyle()) {
                return existingBs.withBody(s2);
            }
            // Existing was plain (or a differently-styled block). Preserve y1's identity (id,
            // markers, prefix) but adopt y2's style and value verbatim so the block envelope is
            // whatever the incoming snippet supplied.
            recordBoundaryRepair(BoundaryRepair.PLAIN_TO_BLOCK);
            return y1.withStyle(y2.getStyle()).withValue(y2.getValue());
        }
        // Result is a plain (or quoted) scalar.
        if (existingBs != null) {
            // Existing block scalar's trailing content held the sibling-separator linebreak; the
            // enclosing mapping needs to add a leading linebreak to the next entry's prefix so the
            // switched-to-plain value does not glue onto the following sibling.
            recordBoundaryRepair(BoundaryRepair.BLOCK_TO_PLAIN);
        }
        return y1.withStyle(y2.getStyle()).withValue(y2.getValue());
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
        java.util.Map<java.util.UUID, BoundaryRepair> repairs = mappingCursor.getMessage(SIBLING_BOUNDARY_REPAIR);
        if (repairs == null) {
            repairs = new java.util.HashMap<>();
            mappingCursor.putMessage(SIBLING_BOUNDARY_REPAIR, repairs);
        }
        repairs.put(((Yaml.Mapping.Entry) entryCursor.getValue()).getId(), repair);
    }

    private static final String SIBLING_BOUNDARY_REPAIR = "org.openrewrite.yaml.MergeYamlVisitor.siblingBoundaryRepair";

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

    /**
     * Strips an inline comment that is stored as the leading part (before the first line break) of
     * the last entry's prefix. This is used to remove a trailing comment that was copied onto a
     * newly-inserted entry, so that the comment is not rendered twice. When the mapping being merged
     * is nested, the comment lives on a sibling entry of an ancestor mapping that is traversed by the
     * outer visitor rather than the {@link MergeYamlVisitor}, hence this method is invoked from there.
     */
    static Yaml.Mapping removeInlineCommentFromLastEntry(Yaml.Mapping mapping) {
        return mapping.withEntries(mapLast(mapping.getEntries(), entry -> {
            String prefix = entry.getPrefix();
            String[] lines = LINE_BREAK.split(prefix, -1);
            if (lines.length <= 1) {
                return entry;
            }
            String linebreak = prefix.contains("\r\n") ? "\r\n" : "\n";
            return entry.withPrefix(linebreak + String.join(linebreak, Arrays.copyOfRange(lines, 1, lines.length)));
        }));
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
