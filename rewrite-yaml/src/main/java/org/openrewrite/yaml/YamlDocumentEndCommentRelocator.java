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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.UUID;

final class YamlDocumentEndCommentRelocator {
    private YamlDocumentEndCommentRelocator() {
    }

    static Yaml.Document relocate(Yaml.Document before, Yaml.Document after) {
        String comment = inlineCommentOf(before.getEnd().getPrefix());
        UUID ownerId = finalEntryId(before.getBlock());
        if (comment.isEmpty() || ownerId == null) {
            return after;
        }

        CommentRelocator visitor = new CommentRelocator(ownerId, comment, linebreak(before.getEnd().getPrefix()));
        Yaml.Document relocated = after.withBlock((Yaml.Block) visitor.visitNonNull(after.getBlock(), 0));
        if (!visitor.relocated || !sameInlineComment(after.getEnd().getPrefix(), comment)) {
            return relocated;
        }
        return relocated.withEnd(after.getEnd().withPrefix(removeInlineComment(after.getEnd().getPrefix())));
    }

    private static @Nullable UUID finalEntryId(Yaml.Block block) {
        if (block instanceof Yaml.Mapping) {
            List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) block).getEntries();
            return entries.isEmpty() ? null : finalEntryId(entries.get(entries.size() - 1));
        }
        if (block instanceof Yaml.Sequence) {
            List<Yaml.Sequence.Entry> entries = ((Yaml.Sequence) block).getEntries();
            return entries.isEmpty() ? null : finalEntryId(entries.get(entries.size() - 1));
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

    private static class CommentRelocator extends YamlIsoVisitor<Integer> {
        private final UUID ownerId;
        private final String comment;
        private final String linebreak;
        private boolean ownerFound;
        private boolean relocated;

        private CommentRelocator(UUID ownerId, String comment, String linebreak) {
            this.ownerId = ownerId;
            this.comment = comment;
            this.linebreak = linebreak;
        }

        @Override
        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Integer p) {
            if (ownerFound && !relocated) {
                relocated = true;
                return super.visitMappingEntry(entry.withPrefix(prefixWithComment(entry.getPrefix())), p);
            }
            Yaml.Mapping.Entry visited = super.visitMappingEntry(entry, p);
            if (ownerId.equals(entry.getId())) {
                ownerFound = true;
            }
            return visited;
        }

        @Override
        public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, Integer p) {
            if (ownerFound && !relocated) {
                relocated = true;
                return super.visitSequenceEntry(entry.withPrefix(prefixWithComment(entry.getPrefix())), p);
            }
            Yaml.Sequence.Entry visited = super.visitSequenceEntry(entry, p);
            if (ownerId.equals(entry.getId())) {
                ownerFound = true;
            }
            return visited;
        }

        private String prefixWithComment(String prefix) {
            return sameInlineComment(prefix, comment) ? prefix :
                    comment + (StringUtils.hasLineBreak(prefix) ? "" : linebreak) + prefix;
        }
    }

    private static boolean sameInlineComment(String prefix, String comment) {
        String existingComment = inlineCommentOf(prefix);
        return comment.equals(existingComment) || comment.trim().equals(existingComment.trim());
    }

    private static String inlineCommentOf(String prefix) {
        int linebreak = firstLineBreak(prefix);
        String firstLine = linebreak == -1 ? prefix : prefix.substring(0, linebreak);
        return firstLine.contains("#") ? firstLine : "";
    }

    private static String removeInlineComment(String prefix) {
        int linebreak = firstLineBreak(prefix);
        return linebreak == -1 ? "" : prefix.substring(linebreak);
    }

    private static String linebreak(String text) {
        return text.contains("\r\n") ? "\r\n" : text.indexOf('\r') >= 0 ? "\r" : "\n";
    }

    private static int firstLineBreak(String text) {
        int linefeed = text.indexOf('\n');
        int carriageReturn = text.indexOf('\r');
        return linefeed == -1 ? carriageReturn :
                carriageReturn == -1 ? linefeed : Math.min(linefeed, carriageReturn);
    }
}
