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
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class YamlDocumentEndCommentRelocator {
    private YamlDocumentEndCommentRelocator() {
    }

    static Yaml.Document relocate(Yaml.Document before, Yaml.Document after, UUID movedEntryId) {
        String comment = inlineCommentOf(before.getEnd().getPrefix());
        if (comment.isEmpty()) {
            return after;
        }

        UUID ownerId = finalEntryId(before.getBlock());
        if (ownerId == null) {
            return after;
        }

        List<Yaml> entries = new ArrayList<>();
        collectEntries(after.getBlock(), entries);
        int ownerIndex = indexOf(entries, ownerId);
        if (ownerIndex == -1) {
            int movedEntryIndex = indexOf(entries, movedEntryId);
            if (movedEntryIndex == -1) {
                return after;
            }
            ownerIndex = indexOf(entries, finalEntryId(entries.get(movedEntryIndex)));
        }

        if (ownerIndex == -1 || ownerIndex == entries.size() - 1) {
            return after;
        }

        Yaml successor = entries.get(ownerIndex + 1);
        String successorPrefix = successor.getPrefix();
        String prefix = sameInlineComment(successorPrefix, comment) ? successorPrefix :
                comment + (hasLineBreak(successorPrefix) ? "" : linebreak(after, entries)) + successorPrefix;
        Yaml.Block relocatedBlock = withEntryPrefix(after.getBlock(), successor.getId(), prefix);

        String endPrefix = after.getEnd().getPrefix();
        if (sameInlineComment(endPrefix, comment)) {
            endPrefix = removeInlineComment(endPrefix);
        }
        return after.withBlock(relocatedBlock).withEnd(after.getEnd().withPrefix(endPrefix));
    }

    private static void collectEntries(Yaml.Block block, List<Yaml> entries) {
        if (block instanceof Yaml.Mapping) {
            for (Yaml.Mapping.Entry entry : ((Yaml.Mapping) block).getEntries()) {
                entries.add(entry);
                collectEntries(entry.getValue(), entries);
            }
        } else if (block instanceof Yaml.Sequence) {
            for (Yaml.Sequence.Entry entry : ((Yaml.Sequence) block).getEntries()) {
                entries.add(entry);
                collectEntries(entry.getBlock(), entries);
            }
        }
    }

    private static @Nullable UUID finalEntryId(Yaml.Block block) {
        if (block instanceof Yaml.Mapping) {
            List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) block).getEntries();
            return entries.isEmpty() ? null : finalEntryId(entries.get(entries.size() - 1));
        } else if (block instanceof Yaml.Sequence) {
            List<Yaml.Sequence.Entry> entries = ((Yaml.Sequence) block).getEntries();
            return entries.isEmpty() ? null : finalEntryId(entries.get(entries.size() - 1));
        }
        return null;
    }

    private static @Nullable UUID finalEntryId(Yaml entry) {
        if (entry instanceof Yaml.Mapping.Entry) {
            return finalEntryId((Yaml.Mapping.Entry) entry);
        } else if (entry instanceof Yaml.Sequence.Entry) {
            return finalEntryId((Yaml.Sequence.Entry) entry);
        }
        return null;
    }

    private static UUID finalEntryId(Yaml.Mapping.Entry entry) {
        UUID childId = finalEntryId(entry.getValue());
        return childId == null ? entry.getId() : childId;
    }

    private static UUID finalEntryId(Yaml.Sequence.Entry entry) {
        UUID childId = finalEntryId(entry.getBlock());
        return childId == null ? entry.getId() : childId;
    }

    private static int indexOf(List<Yaml> entries, @Nullable UUID id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private static Yaml.Block withEntryPrefix(Yaml.Block block, UUID entryId, String prefix) {
        if (block instanceof Yaml.Mapping) {
            Yaml.Mapping mapping = (Yaml.Mapping) block;
            List<Yaml.Mapping.Entry> entries = mapping.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                Yaml.Mapping.Entry entry = entries.get(i);
                if (entryId.equals(entry.getId())) {
                    if (prefix.equals(entry.getPrefix())) {
                        return block;
                    }
                    List<Yaml.Mapping.Entry> updatedEntries = new ArrayList<>(entries);
                    updatedEntries.set(i, entry.withPrefix(prefix));
                    return mapping.withEntries(updatedEntries);
                }
                Yaml.Block value = withEntryPrefix(entry.getValue(), entryId, prefix);
                if (value != entry.getValue()) {
                    List<Yaml.Mapping.Entry> updatedEntries = new ArrayList<>(entries);
                    updatedEntries.set(i, entry.withValue(value));
                    return mapping.withEntries(updatedEntries);
                }
            }
        } else if (block instanceof Yaml.Sequence) {
            Yaml.Sequence sequence = (Yaml.Sequence) block;
            List<Yaml.Sequence.Entry> entries = sequence.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                Yaml.Sequence.Entry entry = entries.get(i);
                if (entryId.equals(entry.getId())) {
                    if (prefix.equals(entry.getPrefix())) {
                        return block;
                    }
                    List<Yaml.Sequence.Entry> updatedEntries = new ArrayList<>(entries);
                    updatedEntries.set(i, entry.withPrefix(prefix));
                    return sequence.withEntries(updatedEntries);
                }
                Yaml.Block child = withEntryPrefix(entry.getBlock(), entryId, prefix);
                if (child != entry.getBlock()) {
                    List<Yaml.Sequence.Entry> updatedEntries = new ArrayList<>(entries);
                    updatedEntries.set(i, entry.withBlock(child));
                    return sequence.withEntries(updatedEntries);
                }
            }
        }
        return block;
    }

    private static boolean sameInlineComment(String prefix, String comment) {
        String existingComment = inlineCommentOf(prefix);
        return comment.equals(existingComment) || comment.trim().equals(existingComment.trim());
    }

    private static String inlineCommentOf(String prefix) {
        int lineBreak = firstLineBreak(prefix);
        String firstLine = lineBreak == -1 ? prefix : prefix.substring(0, lineBreak);
        return firstLine.contains("#") ? firstLine : "";
    }

    private static String removeInlineComment(String prefix) {
        int lineBreak = firstLineBreak(prefix);
        return lineBreak == -1 ? "" : prefix.substring(lineBreak);
    }

    private static boolean hasLineBreak(String text) {
        return firstLineBreak(text) != -1;
    }

    private static String linebreak(Yaml.Document document, List<Yaml> entries) {
        String linebreak = linebreakOf(document.getEnd().getPrefix());
        if (linebreak != null) {
            return linebreak;
        }
        for (Yaml entry : entries) {
            linebreak = linebreakOf(entry.getPrefix());
            if (linebreak != null) {
                return linebreak;
            }
        }
        return "\n";
    }

    private static @Nullable String linebreakOf(String text) {
        int lineBreak = firstLineBreak(text);
        if (lineBreak == -1) {
            return null;
        }
        if (text.charAt(lineBreak) == '\r' && lineBreak + 1 < text.length() && text.charAt(lineBreak + 1) == '\n') {
            return "\r\n";
        }
        return String.valueOf(text.charAt(lineBreak));
    }

    private static int firstLineBreak(String text) {
        int lineFeed = text.indexOf('\n');
        int carriageReturn = text.indexOf('\r');
        if (lineFeed == -1) {
            return carriageReturn;
        }
        if (carriageReturn == -1) {
            return lineFeed;
        }
        return Math.min(lineFeed, carriageReturn);
    }
}
