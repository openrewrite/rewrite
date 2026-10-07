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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.properties.ChangePropertyValue;
import org.openrewrite.properties.PropertiesParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProperty extends ScanningRecipe<AddProperty.NeedsProperty> {

    @Option(displayName = "Property name",
            description = "The name of the property to add.",
            example = "org.gradle.caching")
    String key;

    @Option(example = "true", displayName = "Property value",
            description = "The value of the property to add.")
    String value;

    @Option(displayName = "Overwrite if exists",
            description = "If a property with the same key exists, overwrite.",
            example = "true")
    @Nullable
    Boolean overwrite;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                          "When not set, the property is written only to the root project's `gradle.properties`.",
            required = false,
            example = "**/*.properties")
    @Nullable
    String filePattern;

    String displayName = "Add Gradle property";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s=%s`", key, value);
    }

    String description = "Add a property to the root project's `gradle.properties`. Subprojects inherit root project " +
                         "properties, so writing to each submodule's `gradle.properties` would be redundant.";

    public static class NeedsProperty {
        boolean isGradleProject;
        @Nullable
        Path rootProjectDir;
        Set<Path> gradlePropertiesPaths = new HashSet<>();
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
                if (sourceFile.getSourcePath().endsWith("gradle.properties") &&
                        (filePattern == null ||
                                new FindSourceFiles(filePattern).getVisitor().visitNonNull(tree, ctx) != tree)) {
                    acc.gradlePropertiesPaths.add(sourceFile.getSourcePath());
                }
                if (IsBuildGradle.matches(sourceFile.getSourcePath())) {
                    acc.isGradleProject = true;
                    sourceFile.getMarkers().findFirst(GradleProject.class)
                            .filter(gp -> ":".equals(gp.getPath()))
                            .ifPresent(gp -> {
                                Path parent = sourceFile.getSourcePath().getParent();
                                acc.rootProjectDir = parent == null ? Paths.get("") : parent;
                            });
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(NeedsProperty acc, ExecutionContext ctx) {
        if (!acc.isGradleProject) {
            return emptyList();
        }
        Path rootPath = rootGradlePropertiesPath(acc);
        if (acc.gradlePropertiesPaths.contains(rootPath)) {
            return emptyList();
        }
        return PropertiesParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(rootPath, key + "=" + value)), null, ctx)
                .collect(toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NeedsProperty acc) {
        return Preconditions.check(acc.isGradleProject, new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (!sourceFile.getSourcePath().endsWith("gradle.properties")) {
                    return sourceFile;
                }
                if (filePattern != null) {
                    if (new FindSourceFiles(filePattern).getVisitor().visitNonNull(sourceFile, ctx) == sourceFile) {
                        return sourceFile;
                    }
                } else if (acc.rootProjectDir != null && !sourceFile.getSourcePath().equals(rootGradlePropertiesPath(acc))) {
                    return sourceFile;
                }
                Tree t = !Boolean.TRUE.equals(overwrite) ?
                        sourceFile :
                        new ChangePropertyValue(key, value, null, false, null)
                                .getVisitor().visitNonNull(sourceFile, ctx);
                return org.openrewrite.properties.AddProperty.builder()
                        .property(key).value(value).build()
                        .getVisitor()
                        .visitNonNull(t, ctx);
            }
        });
    }

    private static Path rootGradlePropertiesPath(NeedsProperty acc) {
        return acc.rootProjectDir == null ?
                Paths.get("gradle.properties") :
                acc.rootProjectDir.resolve("gradle.properties");
    }
}
