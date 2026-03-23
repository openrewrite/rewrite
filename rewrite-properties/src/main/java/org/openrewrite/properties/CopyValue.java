/*
 * Copyright 2025 the original author or authors.
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class CopyValue extends ScanningRecipe<CopyValue.Accumulator> {

    @Option(displayName = "Old property key",
            description = "The property key to copy the value from. Supports glob patterns.",
            example = "app.source.property")
    String oldPropertyKey;

    @Option(displayName = "Old file path",
            description = "The file path to the properties file to copy the value from. " +
                          "If `null` then the value will be copied from any properties file it appears within.",
            example = "src/main/resources/application.properties",
            required = false)
    @Nullable
    String oldFilePath;

    @Option(displayName = "New property key",
            description = "The property key to copy the value to.",
            example = "app.destination.property")
    String newPropertyKey;

    @Option(displayName = "New file path",
            description = "The file path to the properties file to copy the value to. " +
                          "If `null` then the value will be copied only into the same file it was found in.",
            example = "src/main/resources/application.properties",
            required = false)
    @Nullable
    String newFilePath;

    @Option(displayName = "Create new keys",
            description = "When the destination key does _not_ already exist, create it. Default is `true`.",
            required = false)
    @Nullable
    Boolean createNewKeys;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `oldPropertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                          "rules. Default is `true`. Set to `false` to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    String displayName = "Copy property value";

    String description = "Copies a property value from one key to another. " +
               "The existing key/value pair remains unaffected by this change. " +
               "If the destination key already exists, its value will be replaced. " +
               "By default, creates the destination key if it does not exist.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%s`%s` to %s`%s`",
                (oldFilePath == null) ? "" : oldFilePath + ":",
                oldPropertyKey,
                (newFilePath == null) ? "" : newFilePath + ":",
                newPropertyKey);
    }

    @Data
    public static class Accumulator {
        @Nullable
        String value;

        @Nullable
        Path path;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        NameCaseConvention.Compiled keyMatcher = (!Boolean.FALSE.equals(relaxedBinding) ?
                NameCaseConvention.LOWER_CAMEL :
                NameCaseConvention.EXACT).compile(oldPropertyKey);

        TreeVisitor<?, ExecutionContext> visitor = new PropertiesIsoVisitor<ExecutionContext>() {
            @Override
            public Properties.Entry visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                if (acc.value == null && keyMatcher.matchesGlob(entry.getKey())) {
                    acc.value = entry.getValue().getText();
                    acc.path = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                }
                return super.visitEntry(entry, ctx);
            }
        };

        if (oldFilePath != null) {
            visitor = Preconditions.check(new FindSourceFiles(oldFilePath), visitor);
        }

        return visitor;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.value == null) {
            return TreeVisitor.noop();
        }

        return Preconditions.check(
                new FindSourceFiles(newFilePath == null ? acc.path.toString() : newFilePath),
                new PropertiesIsoVisitor<ExecutionContext>() {
                    boolean found;

                    @Override
                    public Properties.File visitFile(Properties.File file, ExecutionContext ctx) {
                        found = false;
                        Properties.File f = super.visitFile(file, ctx);
                        if (!found && !Boolean.FALSE.equals(createNewKeys)) {
                            Properties.Value propertyValue = new Properties.Value(randomId(), "", Markers.EMPTY, acc.value);
                            Properties.Entry newEntry = new Properties.Entry(
                                    randomId(), "\n", Markers.EMPTY,
                                    newPropertyKey, "", Properties.Entry.Delimiter.EQUALS, propertyValue);

                            List<Properties.Content> newContent = new ArrayList<>(f.getContent());
                            newContent.add(newEntry);
                            f = f.withContent(newContent);
                        }
                        return f;
                    }

                    @Override
                    public Properties.Entry visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                        if (entry.getKey().equals(newPropertyKey)) {
                            found = true;
                            if (!entry.getValue().getText().equals(acc.value)) {
                                return entry.withValue(entry.getValue().withText(acc.value));
                            }
                        }
                        return super.visitEntry(entry, ctx);
                    }
                });
    }
}
