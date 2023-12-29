/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.ChangePropertyValue;
import org.openrewrite.properties.PropertiesParser;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProperty extends ScanningRecipe<AddProperty.NeedsProperty> {

    @Option(displayName = "Property name",
            description = "The name of the property to add.",
            example = "org.gradle.caching")
    String key;

    @Option(displayName = "Property value",
            description = "The value of the property to add.")
    String value;

    @Option(displayName = "Overwrite if exists",
            description = "If a property with the same key exists, overwrite.",
            example = "Enable the Gradle build cache")
    @Nullable
    Boolean overwrite;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                          "When not set, all source files are searched.",
            example = "**/*.properties")
    @Nullable
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Add Gradle property";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s=%s`", key, value);
    }

    @Override
    public String getDescription() {
        return "Add a property to the `gradle.properties` file.";
    }

    public static class NeedsProperty {
        boolean isGradleProject;
        boolean hasGradleProperties;
    }

    @Override
    public NeedsProperty getInitialValue(ExecutionContext ctx) {
        return new NeedsProperty();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(NeedsProperty acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (filePattern != null) {
                    if (new FindSourceFiles(filePattern).getVisitor().visitNonNull(tree, ctx) != tree &&
                        sourceFile.getSourcePath().endsWith("gradle.properties")) {
                        acc.hasGradleProperties = true;
                    }
                } else if (sourceFile.getSourcePath().endsWith("gradle.properties")) {
                    acc.hasGradleProperties = true;
                }

                if (IsBuildGradle.matches(sourceFile.getSourcePath())) {
                    acc.isGradleProject = true;
                }

                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(NeedsProperty acc, ExecutionContext ctx) {
        if (!acc.hasGradleProperties) {
            return PropertiesParser.builder().build()
                    .parseInputs(singletonList(Parser.Input.fromString(Paths.get("gradle.properties"),
                            key + "=" + value)), null, ctx)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NeedsProperty acc) {
        return Preconditions.check(acc.isGradleProject && acc.hasGradleProperties, new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (filePattern != null) {
                    if (new FindSourceFiles(filePattern).getVisitor().visitNonNull(sourceFile, ctx) != sourceFile &&
                        sourceFile.getSourcePath().endsWith("gradle.properties")) {
                        Tree t = !Boolean.TRUE.equals(overwrite) ?
                                sourceFile :
                                new ChangePropertyValue(key, value, null, false, null)
                                        .getVisitor().visitNonNull(sourceFile, ctx);
                        return new org.openrewrite.properties.AddProperty(key, value, null, null)
                                .getVisitor()
                                .visitNonNull(t, ctx);
                    }
                } else if (sourceFile.getSourcePath().endsWith("gradle.properties")) {
                    Tree t = !Boolean.TRUE.equals(overwrite) ?
                            sourceFile :
                            new ChangePropertyValue(key, value, null, false, null)
                                    .getVisitor().visitNonNull(sourceFile, ctx);
                    return new org.openrewrite.properties.AddProperty(key, value, null, null)
                            .getVisitor()
                            .visitNonNull(t, ctx);
                }
                return sourceFile;
            }
        });
    }
}
