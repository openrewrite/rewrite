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
package org.openrewrite.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.CreateFileVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.java.tree.Space;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreateJavaClass extends ScanningRecipe<AtomicBoolean> {

    @JsonIgnore
    @Language("java")
    private static final String NEW_CLASS_TEMPLATE = "package %s;\n\npublic class %s {\n}";

    @Option(displayName = "Source root",
            description = "The source root of the new class file.",
            valid = {"main", "test"},
            example = "main")
    @NonNull
    String sourceRoot;

    @Option(displayName = "Package name",
            description = "The package of the new class.",
            example = "org.openrewrite.example")
    @NonNull
    String packageName;

    @Option(displayName = "Class name",
            description = "File path of new file.",
            example = "ExampleClass")
    @NonNull
    String className;

    @Language("java")
    @Option(displayName = "Class template",
            description = "The class template to be used.",
            example = "Some text.")
    @NonNull
    String classTemplate;

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Option(displayName = "Relative directory path",
            description = "Directory path of new class.",
            required = false,
            example = "foo/bar")
    @Nullable
    String relativePath;

    @JsonCreator
    public CreateJavaClass(
            @JsonProperty("sourceRoot") String sourceRoot,
            @JsonProperty("packageName") String packageName,
            @JsonProperty("className") String className,
            @Language("java") @JsonProperty("classTemplate") String classTemplate,
            @JsonProperty("overwriteExisting") @Nullable Boolean overwriteExisting,
            @JsonProperty("relativePath") @Nullable String relativePath
    ) {
        this.sourceRoot = sourceRoot;
        this.packageName = packageName;
        this.className = className;
        this.classTemplate = classTemplate;
        this.overwriteExisting = overwriteExisting;
        this.relativePath = relativePath;
    }

    @Override
    public @NonNull String getDisplayName() {
        return "Create Java class";
    }

    @Override
    public @NonNull String getDescription() {
        return "Create a new Java class.";
    }

    @Override
    public @NonNull AtomicBoolean getInitialValue(@NonNull ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getScanner(@NonNull AtomicBoolean shouldCreate) {
        return new CreateFileVisitor(getSourcePath(), shouldCreate);
    }

    @Override
    public @NonNull Collection<SourceFile> generate(AtomicBoolean shouldCreate, @NonNull ExecutionContext ctx) {
        if (shouldCreate.get()) {
            String classTemplate = StringUtils.isBlank(this.classTemplate) ? NEW_CLASS_TEMPLATE : this.classTemplate;
            return JavaParser.fromJavaVersion().build().parse(String.format(classTemplate, packageName, className))
                    .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(getSourcePath())))
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(getSourcePath());
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                if ((created.get() || Boolean.TRUE.equals(overwriteExisting)) && path.toString().equals(cu.getSourcePath().toString())) {
                    J.CompilationUnit newCu = JavaTemplate.builder(String.format(classTemplate, packageName, className))
                            .contextSensitive()
                            .doBeforeParseTemplate(System.out::println)
                            .build()
                            .apply(getCursor(), new JavaCoordinates(cu, Space.Location.ANY, JavaCoordinates.Mode.REPLACEMENT, null))
                            .withMarkers(cu.getMarkers())
                            .withId(cu.getId());

                    return newCu.withSourcePath(path);
                }
                return cu;
            }
        };
    }

    private @NonNull String getSourcePath() {
        String relativePath = this.getRelativePath();
        if (relativePath == null) {
            relativePath = "";
        }

        if (!relativePath.isEmpty() && !relativePath.endsWith("/")) {
            relativePath = relativePath + "/";
        }

        return String.format(
                "%ssrc/%s/java/%s/%s.java",
                relativePath,
                sourceRoot,
                packageName.replace('.', '/'),
                className
        );
    }
}
