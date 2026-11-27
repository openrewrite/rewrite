/*
 * Copyright 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The comment that ends the last line of a node. The parser puts it in the prefix of whatever follows the node: the
 * next entry of the nearest enclosing block collection that has one, or the end of the document. A visitor that asks
 * for a change with {@link #replace} applies it with {@code apply} in its visits of mappings, sequences and documents.
 */
final class TrailingComments {
    private static final String REPLACEMENTS = TrailingComments.class.getName() + ".REPLACEMENTS";

    private TrailingComments() {
    }

    /**
     * The comment that ends the last line of the node at {@code cursor}, or null. Asks the visit of the collection or
     * document holding it to replace it with {@code replacement}, or to remove it when that is null.
     */
    static @Nullable String replace(Cursor cursor, @Nullable String replacement) {
        if (endsWithBlockScalar(cursor.getValue())) {
            return null;
        }
        return edit(cursor, prefix -> withComment(prefix, replacement), replacement == null);
    }

    /**
     * Asks for the entries of a block collection after the last of them is removed: the comment that ends the removed
     * entry's last line goes, {@code carried}, the comment that ends the line before it, takes its place, and the line
     * break a block scalar holds moves with the entry that now ends the collection.
     */
    static void afterRemovingLast(Cursor removed, @Nullable Yaml lastKept, @Nullable String carried) {
        boolean removedBlock = endsWithBlockScalar(removed.getValue());
        boolean keptBlock = lastKept != null && endsWithBlockScalar(lastKept);
        if (!removedBlock && !keptBlock) {
            replace(removed, carried);
        } else if (removedBlock && !keptBlock) {
            String lineBreak = lineBreakOf(removed.getValue());
            edit(removed, prefix -> (carried == null ? "" : carried) + lineBreak + prefix, false);
        } else if (!removedBlock) {
            edit(removed, prefix -> stripLeadingLineBreak(withoutInlineComment(prefix)), false);
        }
    }

    /**
     * Asks for what follows the node at {@code cursor} to start on a new line, as an entry that doesn't end with a
     * block scalar now ends the node, and the block scalar held that line break.
     */
    static void breakLineAfter(Cursor cursor) {
        String lineBreak = lineBreakOf(cursor.getValue());
        edit(cursor, prefix -> lineBreak + prefix, false);
    }

    /**
     * Asks the visit of the collection or document holding what follows the node at {@code cursor} to change its
     * prefix with {@code edit}. Returns the comment that ends the node's last line, or null.
     */
    private static @Nullable String edit(Cursor cursor, UnaryOperator<String> edit, boolean onlyIfCommented) {
        Cursor child = cursor;
        Cursor parent = cursor.getParentTreeCursor();
        while (!parent.isRoot()) {
            Object value = parent.getValue();
            if (value instanceof Yaml.Document) {
                Yaml.Document.End end = ((Yaml.Document) value).getEnd();
                return editIn(parent, end.getId(), end.getPrefix(), edit, onlyIfCommented);
            }
            List<? extends Yaml> entries = value instanceof Yaml.Mapping ? ((Yaml.Mapping) value).getEntries() :
                    value instanceof Yaml.Sequence ? ((Yaml.Sequence) value).getEntries() : null;
            if (entries != null) {
                if (isFlow(value)) {
                    return null;
                }
                UUID id = ((Yaml) child.getValue()).getId();
                for (int i = 0; i < entries.size() - 1; i++) {
                    if (entries.get(i).getId().equals(id)) {
                        Yaml next = entries.get(i + 1);
                        return editIn(parent, next.getId(), next.getPrefix(), edit, onlyIfCommented);
                    }
                }
            }
            child = parent;
            parent = parent.getParentTreeCursor();
        }
        return null;
    }

    /**
     * Removes the entries of a block collection that {@code removed} matches, each with the comment that ends its last
     * line. The comment that ends the line before a removed entry moves to what follows it.
     */
    @SuppressWarnings("unchecked")
    static <E extends Yaml> List<E> remove(Cursor cursor, List<E> entries, Predicate<E> removed) {
        List<E> kept = new ArrayList<>(entries.size());
        String carried = null;
        String firstPrefix = null;
        boolean previousRemoved = false;
        for (E entry : entries) {
            if (removed.test(entry)) {
                if (!previousRemoved) {
                    carried = inlineComment(entry.getPrefix());
                }
                if (kept.isEmpty() && firstPrefix == null) {
                    firstPrefix = entry.getPrefix();
                }
                previousRemoved = true;
                continue;
            }
            if (previousRemoved) {
                String own = withoutInlineComment(entry.getPrefix());
                if (kept.isEmpty()) {
                    String first = withoutInlineComment(firstPrefix);
                    own = own.trim().isEmpty() ? first : first.indexOf('\n') < 0 ? stripLeadingLineBreak(own) : own;
                }
                entry = (E) entry.withPrefix((carried == null ? "" : carried) + own);
                carried = null;
            }
            kept.add(entry);
            previousRemoved = false;
        }
        if (previousRemoved) {
            afterRemovingLast(new Cursor(cursor, entries.get(entries.size() - 1)), kept.isEmpty() ? null : kept.get(kept.size() - 1), carried);
        }
        return kept;
    }

    static Yaml.Mapping apply(Cursor cursor, Yaml.Mapping mapping) {
        Map<UUID, UnaryOperator<String>> edits = cursor.pollMessage(REPLACEMENTS);
        return edits == null ? mapping : mapping.withEntries(ListUtils.map(mapping.getEntries(),
                e -> edits.containsKey(e.getId()) ? e.withPrefix(edits.get(e.getId()).apply(e.getPrefix())) : e));
    }

    static Yaml.Sequence apply(Cursor cursor, Yaml.Sequence sequence) {
        Map<UUID, UnaryOperator<String>> edits = cursor.pollMessage(REPLACEMENTS);
        return edits == null ? sequence : sequence.withEntries(ListUtils.map(sequence.getEntries(),
                e -> edits.containsKey(e.getId()) ? e.withPrefix(edits.get(e.getId()).apply(e.getPrefix())) : e));
    }

    static Yaml.Document apply(Cursor cursor, Yaml.Document document) {
        Map<UUID, UnaryOperator<String>> edits = cursor.pollMessage(REPLACEMENTS);
        Yaml.Document.End end = document.getEnd();
        return edits == null || !edits.containsKey(end.getId()) ? document :
                document.withEnd(end.withPrefix(edits.get(end.getId()).apply(end.getPrefix())));
    }

    /**
     * The comment that ends the line a prefix starts on, or null.
     */
    static @Nullable String inlineComment(@Nullable String prefix) {
        if (prefix == null) {
            return null;
        }
        int lineBreak = prefix.indexOf('\n');
        String line = lineBreak < 0 ? prefix : prefix.substring(0, lineBreak);
        if (line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
        }
        // YAML needs white space before a comment that follows a node, and a recipe can put lines in a prefix
        return !line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t') && line.trim().startsWith("#") ? line : null;
    }

    static String withoutInlineComment(@Nullable String prefix) {
        if (prefix == null) {
            return "";
        }
        String comment = inlineComment(prefix);
        return comment == null ? prefix : prefix.substring(comment.length());
    }

    private static @Nullable String editIn(Cursor holder, UUID owner, String prefix, UnaryOperator<String> edit,
                                           boolean onlyIfCommented) {
        String comment = inlineComment(prefix);
        if (comment == null && onlyIfCommented) {
            return null;
        }
        holder.<Map<UUID, UnaryOperator<String>>>computeMessageIfAbsent(REPLACEMENTS, k -> new HashMap<>()).put(owner, edit);
        return comment;
    }

    private static String lineBreakOf(Object node) {
        while (node instanceof Yaml.Mapping.Entry || node instanceof Yaml.Sequence.Entry || node instanceof Yaml.Mapping || node instanceof Yaml.Sequence) {
            if (node instanceof Yaml.Mapping.Entry) {
                node = ((Yaml.Mapping.Entry) node).getValue();
            } else if (node instanceof Yaml.Sequence.Entry) {
                node = ((Yaml.Sequence.Entry) node).getBlock();
            } else if (node instanceof Yaml.Mapping) {
                List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) node).getEntries();
                node = entries.get(entries.size() - 1);
            } else {
                List<Yaml.Sequence.Entry> entries = ((Yaml.Sequence) node).getEntries();
                node = entries.get(entries.size() - 1);
            }
        }
        return node instanceof Yaml.Scalar && ((Yaml.Scalar) node).getValue().endsWith("\r\n") ? "\r\n" : "\n";
    }

    private static String withComment(String prefix, @Nullable String comment) {
        return (comment == null ? "" : comment) + withoutInlineComment(prefix);
    }

    private static boolean isFlow(Object collection) {
        return collection instanceof Yaml.Mapping ? ((Yaml.Mapping) collection).getOpeningBracePrefix() != null :
                ((Yaml.Sequence) collection).getOpeningBracketPrefix() != null;
    }

    static boolean endsWithBlockScalar(Object node) {
        if (node instanceof Yaml.Mapping.Entry) {
            return endsWithBlockScalar(((Yaml.Mapping.Entry) node).getValue());
        }
        if (node instanceof Yaml.Sequence.Entry) {
            return endsWithBlockScalar(((Yaml.Sequence.Entry) node).getBlock());
        }
        if (node instanceof Yaml.Scalar) {
            Yaml.Scalar.Style style = ((Yaml.Scalar) node).getStyle();
            return style == Yaml.Scalar.Style.FOLDED || style == Yaml.Scalar.Style.LITERAL;
        }
        if (node instanceof Yaml.Mapping) {
            List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) node).getEntries();
            return !isFlow(node) && !entries.isEmpty() && endsWithBlockScalar(entries.get(entries.size() - 1));
        }
        if (node instanceof Yaml.Sequence) {
            List<Yaml.Sequence.Entry> entries = ((Yaml.Sequence) node).getEntries();
            return !isFlow(node) && !entries.isEmpty() && endsWithBlockScalar(entries.get(entries.size() - 1));
        }
        return false;
    }

    static String stripLeadingLineBreak(String s) {
        return s.startsWith("\r\n") ? s.substring(2) : s.startsWith("\n") ? s.substring(1) : s;
    }
}
