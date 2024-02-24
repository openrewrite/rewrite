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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;


@Value
@EqualsAndHashCode(callSuper = false)
public class CommentOutProperty extends Recipe {
    @Option(displayName = "Property key",
            description = "The key to be commented out.",
            example = "applicability.singleSource")
    String propertyKey;

    @Option(displayName = "comment text",
            description = "The comment text to be added before the specified key.",
            example = "The `foo` property is deprecated, please migrate")
    String commentText;

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
            private String comment = "";
            private String indentation = "";

            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry,
                                                          ExecutionContext ctx) {
                indentation = entry.getPrefix();
                if (!comment.isEmpty()) {
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

                if (!comment.isEmpty()) {
                    String newPrefix = entry.getPrefix() + "# " + commentText + comment + entry.getPrefix();
                    comment = "";
                    return entry.withPrefix(newPrefix);
                }

                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                        .filter(Yaml.Mapping.Entry.class::isInstance)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .collect(Collectors.toCollection(ArrayDeque::new));

                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                        .map(e2 -> e2.getKey().getValue())
                        .collect(Collectors.joining("."));

                if (prop.equals(propertyKey)) {
                    String prefix = entry.getPrefix();

                    if (prefix.contains("\n")) {
                        comment = entry.print(getCursor()).replace(prefix, prefix + "# ");
                    } else {
                        // getCursor().getParent().getValue()
                        comment = lastIndentation + "#" + entry.print(getCursor());
                    }

                    doAfterVisit(new DeleteProperty(propertyKey, null, null).getVisitor());
                    return entry;
                }

                return super.visitMappingEntry(entry, ctx);
            }
        };
    }
}
