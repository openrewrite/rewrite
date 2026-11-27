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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/// The comment that ends the last line of a node. The parser puts it in the prefix of whatever follows the node: the
/// next entry of the nearest enclosing block collection that has one, or the end of the document. A visitor that asks
/// for a change with [#replace] wraps its visits of mappings, sequences and documents in [#visit], which applies it.
/// A request is only made when the node holding what follows is one of those visits, as a visit that starts below it
/// never gets back to it.
final class TrailingComments {
    private static final String REPLACEMENTS = TrailingComments.class.getName() + ".REPLACEMENTS";
    private static final String VISITING = TrailingComments.class.getName() + ".VISITING";

    private TrailingComments() {
    }

    /// The comment that ends the last line of the node at `cursor`, or null. Asks the visit of the collection or
    /// document holding it to replace it with `replacement`, or to remove it when that is null.
    static @Nullable String replace(Cursor cursor, @Nullable String replacement) {
        if (endsWithBlockScalar(cursor.getValue())) {
            return null;
        }
        return edit(cursor, prefix -> withComment(prefix, replacement), replacement == null);
    }

    /// Asks for the entries of a block collection after the last of them is removed: the comment that ends the removed
    /// entry's last line goes, `carried`, the comment that ends the line before it, takes its place, and the line
    /// break a block scalar holds moves with the entry that now ends the collection.
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

    /// Asks for what follows the node at `cursor` to start on a new line, as an entry that doesn't end with a block
    /// scalar now ends the node, and the block scalar held that line break.
    static void breakLineAfter(Cursor cursor) {
        String lineBreak = lineBreakOf(cursor.getValue());
        edit(cursor, prefix -> lineBreak + prefix, false);
    }

    private static @Nullable String edit(Cursor cursor, UnaryOperator<String> edit, boolean onlyIfCommented) {
        Cursor child = cursor;
        Cursor parent = cursor.getParentTreeCursor();
        while (!parent.isRoot()) {
            Object value = parent.getValue();
            if (value instanceof Yaml.Document) {
                if (parent.getMessage(VISITING) == null) {
                    return null;
                }
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
                        if (parent.getMessage(VISITING) == null) {
                            return null;
                        }
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

    /// Removes the entries of a block collection that `removed` matches, each with the comment that ends its last line
    /// and the whole-line comments above it. The comment that ends the line before a removed entry moves to what
    /// follows it.
    @SuppressWarnings("unchecked")
    static <E extends Yaml> List<E> remove(Cursor cursor, List<E> entries, Predicate<E> removed) {
        List<E> kept = new ArrayList<>(entries.size());
        String carried = null;
        String firstPrefix = null;
        E previousRemoved = null;
        for (E entry : entries) {
            if (removed.test(entry)) {
                if (previousRemoved == null) {
                    carried = inlineComment(entry.getPrefix());
                }
                if (kept.isEmpty() && firstPrefix == null) {
                    firstPrefix = entry.getPrefix();
                }
                previousRemoved = entry;
                continue;
            }
            if (previousRemoved != null) {
                String own = withoutInlineComment(entry.getPrefix());
                if (kept.isEmpty()) {
                    String first = withoutCommentLines(withoutInlineComment(firstPrefix));
                    own = own.trim().isEmpty() ? first : first.indexOf('\n') < 0 ? stripLeadingLineBreak(own) : own;
                } else if (endsWithBlockScalar(kept.get(kept.size() - 1))) {
                    own = stripLeadingLineBreak(own);
                } else if (own.indexOf('\n') < 0) {
                    own = lineBreakOf(previousRemoved) + own;
                }
                entry = (E) entry.withPrefix((carried == null ? "" : carried) + own);
                carried = null;
            }
            kept.add(entry);
            previousRemoved = null;
        }
        if (previousRemoved != null) {
            afterRemovingLast(new Cursor(cursor, entries.get(entries.size() - 1)), kept.isEmpty() ? null : kept.get(kept.size() - 1), carried);
        }
        return kept;
    }

    /// Runs the visit of the mapping, sequence or document at `cursor`, then applies the changes that the visits of
    /// its descendants asked for.
    @SuppressWarnings("unchecked")
    static <T extends Yaml> T visit(Cursor cursor, Supplier<T> visit) {
        cursor.putMessage(VISITING, true);
        T visited = visit.get();
        Map<UUID, UnaryOperator<String>> edits = cursor.pollMessage(REPLACEMENTS);
        if (edits == null) {
            return visited;
        }
        if (visited instanceof Yaml.Mapping) {
            Yaml.Mapping mapping = (Yaml.Mapping) visited;
            return (T) mapping.withEntries(ListUtils.map(mapping.getEntries(), e -> apply(edits, e)));
        }
        if (visited instanceof Yaml.Sequence) {
            Yaml.Sequence sequence = (Yaml.Sequence) visited;
            return (T) sequence.withEntries(ListUtils.map(sequence.getEntries(), e -> apply(edits, e)));
        }
        if (visited instanceof Yaml.Document) {
            Yaml.Document document = (Yaml.Document) visited;
            return (T) document.withEnd(apply(edits, document.getEnd()));
        }
        return visited;
    }

    @SuppressWarnings("unchecked")
    private static <Y extends Yaml> Y apply(Map<UUID, UnaryOperator<String>> edits, Y y) {
        UnaryOperator<String> edit = edits.get(y.getId());
        return edit == null ? y : (Y) y.withPrefix(edit.apply(y.getPrefix()));
    }

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
        holder.<Map<UUID, UnaryOperator<String>>>computeMessageIfAbsent(REPLACEMENTS, k -> new HashMap<>())
                .merge(owner, edit, (first, then) -> p -> then.apply(first.apply(p)));
        return comment;
    }

    private static String lineBreakOf(@Nullable Object node) {
        while (node instanceof Yaml.Mapping.Entry || node instanceof Yaml.Sequence.Entry || node instanceof Yaml.Mapping || node instanceof Yaml.Sequence) {
            if (node instanceof Yaml.Mapping.Entry) {
                node = ((Yaml.Mapping.Entry) node).getValue();
            } else if (node instanceof Yaml.Sequence.Entry) {
                node = ((Yaml.Sequence.Entry) node).getBlock();
            } else {
                List<? extends Yaml> entries = node instanceof Yaml.Mapping ? ((Yaml.Mapping) node).getEntries() :
                        ((Yaml.Sequence) node).getEntries();
                node = entries.isEmpty() ? null : entries.get(entries.size() - 1);
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

    /// The prefix without the lines that hold only a comment, which needs the comment that ends the line it starts on
    /// to be removed first.
    static String withoutCommentLines(String prefix) {
        StringBuilder kept = new StringBuilder(prefix.length());
        int start = 0;
        for (int end = prefix.indexOf('\n'); end >= 0; end = prefix.indexOf('\n', start)) {
            String line = prefix.substring(start, end + 1);
            if (!line.trim().startsWith("#")) {
                kept.append(line);
            }
            start = end + 1;
        }
        return kept.append(prefix, start, prefix.length()).toString();
    }

    static String stripLeadingLineBreak(String s) {
        return s.startsWith("\r\n") ? s.substring(2) : s.startsWith("\n") ? s.substring(1) : s;
    }
}
