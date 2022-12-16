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

import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Value
public class AddProperty extends Recipe {

    @Option(displayName = "Property key",
            description = "The property key to add.",
            example = "management.metrics.enable.process.files")
    String property;

    @Option(displayName = "Property value",
            description = "The value of the new property key.",
            example = "true")
    String value;

    @Option(displayName = "Optional delimiter",
            description = "Property entries support different delimiters (`=`, `:`, or whitespace). The default value is `=` unless provided the delimiter of the new property entry.",
            required = false,
            example = ":")
    @Nullable
    String delimiter;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.properties")
    @Nullable
    String fileMatcher;

    public AddProperty(String property, String value, @Nullable String fileMatcher) {
        this.property = property;
        this.value = value;
        this.delimiter = null;
        this.fileMatcher = fileMatcher;
    }

    @JsonCreator
    public AddProperty(String property, String value, @Nullable String delimiter, @Nullable String fileMatcher) {
        this.property = property;
        this.value = value;
        this.delimiter = delimiter;
        this.fileMatcher = fileMatcher;
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
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext executionContext) {
                Properties p = super.visitFile(file, executionContext);
                if (!StringUtils.isBlank(property) && !StringUtils.isBlank(value)) {
                    Set<Properties.Entry> properties = FindProperties.find(p, property, false);
                    if (properties.isEmpty()) {
                        Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, value);
                        Properties.Entry.Delimiter delimitedBy = delimiter != null && !delimiter.isEmpty() ? Properties.Entry.Delimiter.getDelimiter(delimiter) : Properties.Entry.Delimiter.EQUALS;
                        String beforeEquals = delimitedBy == Properties.Entry.Delimiter.NONE ? delimiter : "";
                        Properties.Entry entry = new Properties.Entry(Tree.randomId(), "\n", Markers.EMPTY, property, beforeEquals, delimitedBy, propertyValue);
                        List<Properties.Content> contentList = ListUtils.concat(((Properties.File) p).getContent(), entry);
                        p = ((Properties.File) p).withContent(contentList);
                    }
                }
                return p;
            }
        };
    }
}
