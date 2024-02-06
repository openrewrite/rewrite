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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreateEmptyJavaClass extends ScanningRecipe<AtomicBoolean> {

    @JsonIgnore
    private static final String NEW_CLASS_TEMPLATE = "package %s;\n\n%sclass %s {\n}";

    @Option(displayName = "Source root",
            description = "The source root of the new class file.",
            example = "src/main/java")
    String sourceRoot;

    @Option(displayName = "Package name",
            description = "The package of the new class.",
            example = "org.openrewrite.example")
    String packageName;

    @Option(displayName = "Modifier",
            description = "The class modifier.",
            valid = {"public", "private", "protected", "package-private"},
            example = "public")
    String modifier;

    @Option(displayName = "Class name",
            description = "File path of new file.",
            example = "ExampleClass")
    String className;

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

    @Override
    public String getDisplayName() {
        return "Create Java class";
    }

    @Override
    public String getDescription() {
        return "Create a new, empty Java class.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        return new CreateFileVisitor(getSourcePath(), shouldCreate);
    }

    @Override
    public Collection<SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.get()) {
            return createEmptyClass().collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = getSourcePath();
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if ((created.get() || Boolean.TRUE.equals(overwriteExisting)) && path.equals(cu.getSourcePath())) {
                    Optional<SourceFile> sourceFile = createEmptyClass().findFirst();
                    if (sourceFile.isPresent() && sourceFile.get() instanceof J.CompilationUnit) {
                        J.CompilationUnit newCu = (J.CompilationUnit) sourceFile.get();
                        return cu.withClasses(newCu.getClasses()).withSourcePath(path);
                    }
                }

                return cu;
            }
        };
    }

    private Stream<SourceFile> createEmptyClass() {
        String packageModifier = "package-private".equals(modifier) ? "" : modifier + " ";
        return JavaParser.fromJavaVersion().build()
                .parse(String.format(CreateEmptyJavaClass.NEW_CLASS_TEMPLATE, packageName, packageModifier, className))
                .map(brandNewFile -> brandNewFile.withSourcePath(getSourcePath()));
    }

    @SuppressWarnings("java:S1075")
    private Path getSourcePath() {
        String path = relativePath;
        if (path == null) {
            path = "";
        }

        if (!path.isEmpty() && !path.endsWith("/")) {
            path = path + "/";
        }

        return Paths.get(String.format(
                "%s%s/%s/%s.java",
                path,
                sourceRoot,
                packageName.replace('.', '/'),
                className));
    }
}
