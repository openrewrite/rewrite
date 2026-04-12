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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.NameCaseConvention.Compiled;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Iterator;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddCommentToProperty extends Recipe {
    @Option(displayName = "Property key",
            description = "The property key to add a comment to. Supports glob patterns.",
            example = "management.metrics.binders.*.enabled")
    String propertyKey;

    @Option(displayName = "Comment",
            description = "The comment to add to the property.",
            example = "This property is deprecated")
    String comment;

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

    @Override
    public String getDisplayName() {
        return "Add comment to a YAML property";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", propertyKey);
    }

    @Override
    public String getDescription() {
        return "Add a comment to a YAML property. The comment will be added on the line before the property.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Compiled keyMatcher = (!Boolean.FALSE.equals(relaxedBinding) ?
                NameCaseConvention.LOWER_CAMEL :
                NameCaseConvention.EXACT).compile(propertyKey);

        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                String prop = getProperty(getCursor());
                if (keyMatcher.matchesGlob(prop)) {
                    String prefix = e.getPrefix();
                    // Don't add duplicate comments
                    if (!prefix.contains("# " + comment)) {
                        String indentation = extractIndentation(prefix);
                        String newPrefix = prefix + "# " + comment + "\n" + indentation;
                        e = e.withPrefix(newPrefix);
                    }
                }
                return e;
            }
        });
    }

    private static String extractIndentation(String prefix) {
        int lastNewline = prefix.lastIndexOf('\n');
        return lastNewline >= 0 ? prefix.substring(lastNewline + 1) : prefix;
    }

    private static String getProperty(Cursor cursor) {
        StringBuilder asProperty = new StringBuilder();
        Iterator<Object> path = cursor.getPath();
        int i = 0;
        while (path.hasNext()) {
            Object next = path.next();
            if (next instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) next;
                if (i++ > 0) {
                    asProperty.insert(0, '.');
                }
                asProperty.insert(0, entry.getKey().getValue());
            }
        }
        return asProperty.toString();
    }
}
