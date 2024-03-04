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
package org.openrewrite.properties;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProperty extends Recipe {

    @Option(displayName = "Property key",
            description = "The property key to add.",
            example = "management.metrics.enable.process.files")
    String property;

    @Option(displayName = "Property value",
            description = "The value of the new property key.")
    String value;

    @Option(displayName = "Optional comment to be prepended to the property",
            description = "A comment that will be added to the new property.",
            required = false,
            example = "This is a comment")
    @Nullable
    String comment;

    @Option(displayName = "Optional delimiter",
            description = "Property entries support different delimiters (`=`, `:`, or whitespace). The default value is `=` unless provided the delimiter of the new property entry.",
            required = false,
            example = ":")
    @Nullable
    String delimiter;

    @Override
    public String getDisplayName() {
        return "Add a new property";
    }

    @Override
    public String getDescription() {
        return "Adds a new property to a property file. " +
               "Attempts to place the new property in alphabetical order by the property keys. " +
               "Whitespace before and after the `=` must be included in the property and value.";
    }

    @Override
    public Validated<Object> validate() {
        return Validated.none()
                .and(Validated.required("property", property))
                .and(Validated.required("value", value));
    }

    @Override
    public PropertiesIsoVisitor<ExecutionContext> getVisitor() {
        return new PropertiesIsoVisitor<ExecutionContext>() {
            @Override
            public Properties.File visitFile(Properties.File file, ExecutionContext ctx) {
                Properties.File p = super.visitFile(file, ctx);
                if (StringUtils.isBlank(property) || StringUtils.isBlank(value)) {
                    return p;
                }
                Set<Properties.Entry> properties = FindProperties.find(p, property, false);
                if (!properties.isEmpty()) {
                    return p;
                }

                Properties.Value propertyValue = new Properties.Value(randomId(), "", Markers.EMPTY, value);
                Properties.Entry.Delimiter delimitedBy = StringUtils.isNotEmpty(delimiter) ? Properties.Entry.Delimiter.getDelimiter(delimiter) : Properties.Entry.Delimiter.EQUALS;
                String beforeEquals = delimitedBy == Properties.Entry.Delimiter.NONE ? delimiter : "";
                Properties.Entry entry = new Properties.Entry(randomId(), "\n", Markers.EMPTY, property, beforeEquals, delimitedBy, propertyValue);
                int insertionIndex = sortedInsertionIndex(entry, p.getContent());

                List<Properties.Content> newContents;
                if(StringUtils.isBlank(comment)) {
                    newContents = Collections.singletonList(entry);
                } else {
                    newContents = Arrays.asList(
                            new Properties.Comment(
                                    randomId(),
                                    "\n",
                                    Markers.EMPTY,
                                    Properties.Comment.Delimiter.HASH_TAG,
                                " " + comment.trim()),
                            entry);
                }

                List<Properties.Content> contentList = new ArrayList<>(p.getContent().size() + 1);
                contentList.addAll(p.getContent().subList(0, insertionIndex));
                contentList.addAll(newContents);
                contentList.addAll(p.getContent().subList(insertionIndex, p.getContent().size()));

                // First entry in the file does not need a newline, but every other entry does
                contentList = ListUtils.map(contentList, (i, c) -> {
                    if(i == 0) {
                        return (Properties.Content) c.withPrefix("");
                    } else if(!c.getPrefix().contains("\n")) {
                        return (Properties.Content) c.withPrefix("\n" + c.getPrefix());
                    }
                    return c;
                });

                p = p.withContent(contentList);
                return p;
            }
        };
    }

    private static int sortedInsertionIndex(Properties.Entry entry, List<Properties.Content> contentsList) {
        if (contentsList.isEmpty()) {
            return 0;
        }
        List<Properties.Entry> sorted =
                Stream.concat(
                                Stream.of(entry),
                                contentsList.stream()
                                        .filter(Properties.Entry.class::isInstance)
                                        .map(Properties.Entry.class::cast))
                        .sorted(Comparator.comparing(Properties.Entry::getKey))
                        .collect(Collectors.toList());
        int indexInSorted = sorted.indexOf(entry);
        if (indexInSorted == 0) {
            return 0;
        }
        Properties.Entry previous = sorted.get(indexInSorted - 1);
        return contentsList.indexOf(previous) + 1;
    }
}
