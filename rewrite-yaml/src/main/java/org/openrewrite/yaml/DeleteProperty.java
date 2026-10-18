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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.NameCaseConvention.Compiled;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteProperty extends Recipe {
    /**
     * The ids of children whose visit decided they're deleted, kept on their parent's cursor for its visit to act on.
     */
    private static final String TO_BE_REMOVED = DeleteProperty.class.getName() + ".TO_BE_REMOVED";

    /**
     * The inline comment that ends the last line of a child, by the child's id, kept on its parent's cursor once the
     * deleted entry that held it is gone. The parser puts such a comment in the prefix of whatever follows.
     */
    private static final String TRAILING_COMMENT = DeleteProperty.class.getName() + ".TRAILING_COMMENT";

    @Option(displayName = "Property key",
            description = "The key to be deleted. Supports glob patterns.",
            example = "management.metrics.binders.files.*")
    String propertyKey;

    @Deprecated
    @Option(displayName = "Coalesce",
            description = "(Deprecated: in a future version, this recipe will always use the `false` behavior)" +
                    " Simplify nested map hierarchies into their simplest dot separated property form.",
            required = false)
    @Nullable
    Boolean coalesce;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Defaults to `true`. If you want to use exact matching in your search, set this to `false`.",
            required = false)
    @Nullable
    Boolean relaxedBinding;


    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    String displayName = "Delete property";

    String description = "Delete a YAML property. Nested YAML mappings are interpreted as dot separated property names, i.e. " +
                "as Spring Boot interprets application.yml files like `a.b.c.d` or `a.b.c:d`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Compiled keyMatcher = (!Boolean.FALSE.equals(relaxedBinding) ?
                NameCaseConvention.LOWER_CAMEL :
                NameCaseConvention.EXACT).compile(propertyKey);

        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {

            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                if (documents != new ReplaceAliasWithAnchorValueVisitor<ExecutionContext>().visit(documents, ctx)) {
                    return documents;
                }
                return super.visitDocuments(documents, ctx);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);

                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                        .filter(Yaml.Mapping.Entry.class::isInstance)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .collect(toCollection(ArrayDeque::new));

                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                        .map(e2 -> e2.getKey().getValue())
                        .collect(joining("."));

                if (keyMatcher.matchesGlob(prop)) {
                    e = toBeRemoved(e);
                    if (Boolean.TRUE.equals(coalesce)) {
                        maybeCoalesceProperties();
                    }
                } else if (isToBeRemoved(e.getValue())) {
                    // a mapping or sequence emptied of its entries goes with its key
                    e = toBeRemoved(e);
                } else {
                    trailingComment(e, trailingComment(e.getValue()));
                }

                return e;
            }

            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                Yaml.Document d = super.visitDocument(document, ctx);
                String comment = trailingComment(d.getBlock());
                return comment == null ? d : d.withEnd(d.getEnd().withPrefix(comment + d.getEnd().getPrefix()));
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                Yaml.Sequence s = super.visitSequence(sequence, ctx);
                boolean childModified = (s != sequence);
                List<Yaml.Sequence.Entry> entries = s.getEntries();
                if (entries.isEmpty()) {
                    return s;
                }

                boolean changed = false;
                String[] carried = {null};
                entries = ListUtils.map(entries, entry -> {
                    if (isToBeRemoved(entry)) {
                        return null;
                    }
                    Yaml.Sequence.Entry kept = carried[0] == null ? entry :
                            entry.withPrefix(carried[0] + entry.getPrefix());
                    carried[0] = trailingComment(entry);
                    return kept;
                });
                trailingComment(s, carried[0]);
                if (entries.size() != s.getEntries().size()) {
                    changed = true;
                }

                if ((changed || childModified) && s.getOpeningBracketPrefix() == null) {
                    List<Yaml.Sequence.Entry> fixedEntries = null;
                    for (int i = 1; i < entries.size(); i++) {
                        Yaml.Sequence.Entry entry = entries.get(i);
                        Yaml.Sequence.Entry prevEntry = entries.get(i - 1);
                        if (!containsNewline(entry.getPrefix()) && !endsWithBlockScalar(prevEntry.getBlock())) {
                            if (fixedEntries == null) {
                                fixedEntries = new ArrayList<>(entries);
                            }
                            fixedEntries.set(i, entry.withPrefix("\n" + entry.getPrefix()));
                        }
                    }
                    if (fixedEntries != null) {
                        entries = fixedEntries;
                    }
                }

                return entries.isEmpty() ? toBeRemoved(s) : s.withEntries(entries);
            }

            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                Yaml.Sequence.Entry e = super.visitSequenceEntry(entry, ctx);
                if (e.getBlock() instanceof Yaml.Mapping && isToBeRemoved(e.getBlock())) {
                    return toBeRemoved(e);
                }
                trailingComment(e, trailingComment(e.getBlock()));
                return e;
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                boolean childModified = (m != mapping);

                boolean changed = false;
                boolean commentMoved = false;
                List<Yaml.Mapping.Entry> entries = new ArrayList<>();
                String firstDeletedPrefix = null;
                boolean previousWasDeleted = false;
                // the inline comment that ends the line of the last kept entry, which the parser puts in the prefix
                // of what follows, so it moves on when the entry after it is deleted
                String carried = null;
                for (Yaml.Mapping.Entry entry : m.getEntries()) {
                    if (isToBeRemoved(entry)) {
                        // Entry is being deleted - capture prefix from the first deleted entry before any kept entries
                        if (entries.isEmpty() && firstDeletedPrefix == null) {
                            firstDeletedPrefix = entry.getPrefix();
                        }
                        if (carried == null && !previousWasDeleted && !entries.isEmpty()) {
                            carried = extractInlineComment(entry.getPrefix());
                        }
                        changed = true;
                        previousWasDeleted = true;
                    } else {
                        if (entries.isEmpty() && firstDeletedPrefix != null && containsOnlyWhitespace(entry.getPrefix())) {
                            entry = entry.withPrefix(firstDeletedPrefix);
                        } else if (previousWasDeleted && !entries.isEmpty() && !containsNewline(entry.getPrefix())) {
                            entry = entry.withPrefix("\n" + entry.getPrefix());
                        } else if (previousWasDeleted && !entries.isEmpty()
                                && endsWithBlockScalar(entries.get(entries.size() - 1))
                                && containsNewline(entry.getPrefix())) {
                            // Block-scalar predecessor already owns the boundary newline; strip the duplicate.
                            entry = entry.withPrefix(stripLeadingLineBreak(entry.getPrefix()));
                        } else if (previousWasDeleted && containsNewline(entry.getPrefix()) &&
                                   extractInlineComment(entry.getPrefix()) != null) {
                            // the inline comment of the deleted line before goes with it
                            entry = entry.withPrefix(entry.getPrefix().substring(entry.getPrefix().indexOf('\n')));
                        }
                        if (carried != null) {
                            entry = entry.withPrefix(carried + entry.getPrefix());
                            commentMoved = true;
                        }
                        entries.add(entry);
                        previousWasDeleted = false;
                        carried = trailingComment(entry);
                    }
                }
                trailingComment(m, carried);

                if (commentMoved && !changed) {
                    m = m.withEntries(entries);
                }
                if (changed) {
                    m = m.withEntries(entries);
                    if (entries.isEmpty()) {
                        m = toBeRemoved(m);
                    }

                    if (getCursor().getParentOrThrow().getValue() instanceof Yaml.Document) {
                        Yaml.Document document = getCursor().getParentOrThrow().getValue();
                        if (!document.isExplicit()) {
                            m = m.withEntries(m.getEntries());
                        }
                    }
                }

                if ((changed || childModified) && m.getOpeningBracePrefix() == null) {
                    List<Yaml.Mapping.Entry> currentEntries = m.getEntries();
                    List<Yaml.Mapping.Entry> fixedEntries = null;
                    for (int i = 1; i < currentEntries.size(); i++) {
                        Yaml.Mapping.Entry entry = currentEntries.get(i);
                        Yaml.Mapping.Entry prevEntry = currentEntries.get(i - 1);
                        if (!containsNewline(entry.getPrefix()) && !endsWithBlockScalar(prevEntry)) {
                            if (fixedEntries == null) {
                                fixedEntries = new ArrayList<>(currentEntries);
                            }
                            fixedEntries.set(i, entry.withPrefix("\n" + entry.getPrefix()));
                        }
                    }
                    if (fixedEntries != null) {
                        m = m.withEntries(fixedEntries);
                    }
                }

                return m;
            }

            /**
             * Asks the visit of the parent of the element being visited to delete {@code y}, which takes its place.
             */
            private <Y extends Yaml> Y toBeRemoved(Y y) {
                getCursor().getParentTreeCursor().<Set<UUID>>computeMessageIfAbsent(TO_BE_REMOVED, k -> new HashSet<>()).add(y.getId());
                return y;
            }

            /**
             * Whether the visit of {@code y}, a child of the element being visited, asked to delete it.
             */
            private boolean isToBeRemoved(Yaml y) {
                Set<UUID> removed = getCursor().getMessage(TO_BE_REMOVED);
                return removed != null && removed.contains(y.getId());
            }

            /**
             * Asks the visit of the parent of the element being visited to put {@code comment}, which ends the last
             * line of {@code y}, before what follows {@code y}.
             */
            private void trailingComment(Yaml y, @Nullable String comment) {
                if (comment != null) {
                    getCursor().getParentTreeCursor()
                            .<Map<UUID, String>>computeMessageIfAbsent(TRAILING_COMMENT, k -> new HashMap<>())
                            .put(y.getId(), comment);
                }
            }

            /**
             * The comment that ends the last line of {@code y}, a child of the element being visited, which the
             * visit of {@code y} asked to put before what follows it.
             */
            private @Nullable String trailingComment(Yaml y) {
                Map<UUID, String> comments = getCursor().getMessage(TRAILING_COMMENT);
                return comments == null ? null : comments.get(y.getId());
            }
        });
    }

    private static boolean containsOnlyWhitespace(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }

        return true;
    }

    private static String stripLeadingLineBreak(String s) {
        if (s.startsWith("\r\n")) {
            return s.substring(2);
        }
        if (s.startsWith("\n") || s.startsWith("\r")) {
            return s.substring(1);
        }
        // Line break not at the start — strip the first one found.
        int idx = -1;
        int idxN = s.indexOf('\n');
        int idxR = s.indexOf('\r');
        if (idxN >= 0 && (idxR < 0 || idxN < idxR)) {
            idx = idxN;
        } else if (idxR >= 0) {
            idx = idxR;
        }
        if (idx < 0) {
            return s;
        }
        int after = idx + 1;
        if (s.charAt(idx) == '\r' && after < s.length() && s.charAt(after) == '\n') {
            after++;
        }
        return s.substring(0, idx) + s.substring(after);
    }

    private static boolean containsNewline(@Nullable String str) {
        return str != null && str.indexOf('\n') >= 0;
    }

    /**
     * Extract an inline comment from a prefix string. Inline comments appear before the
     * first newline in the prefix (e.g. {@code " # comment\n  "} → {@code " # comment"}).
     */
    private static @Nullable String extractInlineComment(String prefix) {
        int newlineIndex = prefix.indexOf('\n');
        String beforeNewline = newlineIndex >= 0 ? prefix.substring(0, newlineIndex) : prefix;
        return beforeNewline.contains("#") ? beforeNewline : null;
    }

    private static boolean endsWithBlockScalar(Yaml.Mapping.Entry entry) {
        return endsWithBlockScalar(entry.getValue());
    }

    private static boolean endsWithBlockScalar(Yaml.Block block) {
        if (block instanceof Yaml.Scalar) {
            Yaml.Scalar.Style style = ((Yaml.Scalar) block).getStyle();
            return style == Yaml.Scalar.Style.FOLDED || style == Yaml.Scalar.Style.LITERAL;
        } else if (block instanceof Yaml.Mapping) {
            List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) block).getEntries();
            if (!entries.isEmpty()) {
                return endsWithBlockScalar(entries.get(entries.size() - 1));
            }
        } else if (block instanceof Yaml.Sequence) {
            List<Yaml.Sequence.Entry> entries = ((Yaml.Sequence) block).getEntries();
            if (!entries.isEmpty()) {
                return endsWithBlockScalar(entries.get(entries.size() - 1).getBlock());
            }
        }
        return false;
    }

}
