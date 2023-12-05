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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
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

    /**
     * Keeping this constructor just for compatibility purposes
     * @deprecated Use {@link AddProperty#AddProperty(String, String, String, String)}}
     */
    @Deprecated
    public AddProperty(String property, String value, @Nullable String delimiter) {
        this(property, value, null, delimiter);
    }

    @JsonCreator
    public AddProperty(String property, String value, @Nullable String comment, @Nullable String delimiter) {
        this.property = property;
        this.value = value;
        this.comment = comment;
        this.delimiter = delimiter;
    }

    @Override
    public String getDisplayName() {
        return "Add a new property";
    }

    @Override
    public String getDescription() {
        return "Adds a new property to a property file at the bottom of the file if it's missing. Whitespace before and after the `=` must be included in the property and value.";
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
            public Properties.File visitFile(Properties.File file, ExecutionContext executionContext) {
                Properties.File p = super.visitFile(file, executionContext);
                if (!StringUtils.isBlank(property) && !StringUtils.isBlank(value)) {
                    Set<Properties.Entry> properties = FindProperties.find(p, property, false);
                    if (properties.isEmpty()) {
                        Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, value);
                        Properties.Entry.Delimiter delimitedBy = StringUtils.isNotEmpty(delimiter) ? Properties.Entry.Delimiter.getDelimiter(delimiter) : Properties.Entry.Delimiter.EQUALS;
                        String beforeEquals = delimitedBy == Properties.Entry.Delimiter.NONE ? delimiter : "";
                        String prefix = "";
                        if (!p.getContent().isEmpty()) {
                            prefix = "\n";
                        }
                        List<Properties.Content> newContent = StringUtils.isNotEmpty(comment)
                                ? Arrays.asList(
                                        new Properties.Comment(Tree.randomId(), prefix, Markers.EMPTY, Properties.Comment.Delimiter.HASH_TAG, String.format(" %s%n", comment)),
                                        new Properties.Entry(Tree.randomId(), "", Markers.EMPTY, property, beforeEquals, delimitedBy, propertyValue)
                                )
                                : Collections.singletonList(new Properties.Entry(Tree.randomId(), prefix, Markers.EMPTY, property, beforeEquals, delimitedBy, propertyValue));
                        List<Properties.Content> contentList = ListUtils.concatAll(p.getContent(), newContent);
                        p = p.withContent(contentList);
                    }
                }
                return p;
            }
        };
    }
}
