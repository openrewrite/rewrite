/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;

@Value
@EqualsAndHashCode(callSuper = false)
public class CommentOutProperty extends Recipe {
    @Option(displayName = "Property key",
            description = "The key to be commented out.",
            example = "applicability.singleSource")
    String propertyKey;

    @Option(displayName = "Comment text",
            description = "The comment text to be added before the specified key.",
            example = "The `foo` property is deprecated, please migrate")
    String commentText;

    @Option(example = "true", displayName = "Comment out property",
            description = "If false, property wouldn't be commented out, only comment will be added. By default, set to true",
            required = false)
    @Nullable
    Boolean commentOutProperty;

    @Override
    public String getDisplayName() {
        return "Comment out property";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", propertyKey);
    }

    @Override
    public String getDescription() {
        return "Comment out a YAML property and add a comment in front.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            private boolean nextDocNeedsNewline;
            private String comment = "";
            private String indentation = "";
            private boolean isBlockCommentExists;

            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                Yaml.Document doc = super.visitDocument(document, ctx);

                if (nextDocNeedsNewline) {
                    nextDocNeedsNewline = false;
                    doc = doc.withPrefix("\n" + doc.getPrefix());
                }

                // Add any leftover comment to the end of document
                if (!comment.isEmpty() && !Boolean.FALSE.equals(commentOutProperty)) {
                    String newPrefix = String.format("%s# %s%s%s",
                            indentation,
                            commentText,
                            indentation.contains("\n") ? "" : "\n",
                            indentation.contains("\n") ? comment : comment.replace("#", "# "));
                    nextDocNeedsNewline = !newPrefix.endsWith("\n");
                    comment = "";
                    return document.withEnd(doc.getEnd().withPrefix(newPrefix));
                }
                return doc;
            }

            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry,
                                                          ExecutionContext ctx) {
                indentation = entry.getPrefix();
                if (Boolean.FALSE.equals(commentOutProperty)) {
                    return addBockCommentIfNecessary(entry, ctx);
                } else if (!comment.isEmpty()) {
                    // add comment and return
                    String newPrefix = entry.getPrefix() + "# " + commentText + comment + entry.getPrefix();
                    comment = "";
                    return entry.withPrefix(newPrefix);
                }
                return super.visitSequenceEntry(entry, ctx);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                String lastIndentation = indentation;
                indentation = entry.getPrefix();

                if (!comment.isEmpty() && !Boolean.FALSE.equals(commentOutProperty)) {
                    String newPrefix = entry.getPrefix() + "# " + commentText + comment + entry.getPrefix();
                    comment = "";
                    return entry.withPrefix(newPrefix);
                }

                String prop = calculateCurrentKeyPath();

                if (prop.equals(propertyKey)) {
                    String prefix = entry.getPrefix();

                    if (prefix.contains("\n")) {
                        comment = entry.print(getCursor().getParentTreeCursor()).replace(prefix, prefix + "# ");
                    } else {
                        // getCursor().getParent().getValue()
                        comment = lastIndentation + "#" + entry.print(getCursor().getParentTreeCursor());
                    }

                    if (Boolean.FALSE.equals(commentOutProperty)) {
                        if (!entry.getPrefix().contains(commentText) && !isBlockCommentExists) {
                            return entry.withPrefix(entry.getPrefix() + "# " + commentText + (entry.getPrefix().contains("\n") ? entry.getPrefix() : "\n" + entry.getPrefix()));
                        }
                    } else {
                        doAfterVisit(new DeleteProperty(propertyKey, null, null, null).getVisitor());
                    }
                    return entry;
                }

                return super.visitMappingEntry(entry, ctx);
            }

            private Yaml.Sequence.Entry addBockCommentIfNecessary(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                boolean propertyExistsInSequence = isPropertyExistsInSequence(entry);
                if (propertyExistsInSequence) {
                    isBlockCommentExists = true;
                    if (!entry.getPrefix().contains(commentText)) {
                        return entry.withPrefix(entry.getPrefix() + "# " + commentText + (entry.getPrefix().contains("\n") ? entry.getPrefix() : "\n" + entry.getPrefix()));
                    }
                }
                return super.visitSequenceEntry(entry, ctx);
            }

            private boolean isPropertyExistsInSequence(Yaml.Sequence.Entry entry) {
                if (!(entry.getBlock() instanceof Yaml.Mapping)) {
                    return false;
                }
                Yaml.Mapping mapping = (Yaml.Mapping) entry.getBlock();
                String prop = calculateCurrentKeyPath();
                return mapping.getEntries().stream()
                        .anyMatch(e -> propertyKey.equals(prop + "." + e.getKey().getValue()));
            }

            private String calculateCurrentKeyPath() {
                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                        .filter(Yaml.Mapping.Entry.class::isInstance)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .collect(toCollection(ArrayDeque::new));

                return stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                        .map(e2 -> e2.getKey().getValue())
                        .collect(joining("."));
            }
        };
    }
}
