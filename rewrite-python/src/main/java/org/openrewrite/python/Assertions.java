/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class Assertions {
    private Assertions() {
    }

    /**
     * Sets up a Python project with dependencies installed for type attribution.
     * Similar to the JavaScript npm() helper, this creates a cached workspace
     * with dependencies from pyproject.toml and symlinks the virtual environment.
     *
     * @param relativeTo The test directory where files should be written
     * @param sources    Source specs including Python files and optionally pyproject.toml
     * @return SourceSpecs wrapped in a directory context
     */
    public static SourceSpecs uv(Path relativeTo, SourceSpecs... sources) {
        String pyprojectContent = null;

        // First pass: find pyproject.toml content and write it
        for (SourceSpecs multiSpec : sources) {
            if (multiSpec instanceof SourceSpec) {
                SourceSpec<?> spec = (SourceSpec<?>) multiSpec;
                Path sourcePath = spec.getSourcePath();
                if (sourcePath != null && "pyproject.toml".equals(sourcePath.toFile().getName())) {
                    pyprojectContent = spec.getBefore();
                    try {
                        Path pyproject = relativeTo.resolve(sourcePath);
                        Files.write(pyproject, requireNonNull(pyprojectContent).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    break;
                }
            }
        }

        // Second pass: create workspace and symlink .venv and uv.lock
        if (pyprojectContent != null) {
            Path workspaceDir = DependencyWorkspace.getOrCreateWorkspace(pyprojectContent);
            Path venvSource = workspaceDir.resolve(".venv");
            Path venvTarget = relativeTo.resolve(".venv");
            Path lockFileSource = workspaceDir.resolve("uv.lock");
            Path lockFileTarget = relativeTo.resolve("uv.lock");

            try {
                if (Files.exists(venvSource) && !Files.exists(venvTarget)) {
                    Files.createSymbolicLink(venvTarget, venvSource);
                }
                if (Files.exists(lockFileSource) && !Files.exists(lockFileTarget)) {
                    Files.createSymbolicLink(lockFileTarget, lockFileSource);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create symlink for .venv", e);
            }
        }

        return SourceSpecs.dir(relativeTo.toString(), sources);
    }

    public static SourceSpecs pyproject(@Language("toml") @Nullable String before) {
        return pyproject(before, s -> {
        });
    }

    public static SourceSpecs pyproject(@Language("toml") @Nullable String before,
                                        Consumer<SourceSpec<Toml.Document>> spec) {
        SourceSpec<Toml.Document> toml = new SourceSpec<>(
                Toml.Document.class, null, PyProjectTomlParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        toml.path("pyproject.toml");
        spec.accept(toml);
        return toml;
    }

    public static SourceSpecs pyproject(@Language("toml") @Nullable String before,
                                        @Language("toml") @Nullable String after) {
        return pyproject(before, after, s -> {
        });
    }

    public static SourceSpecs pyproject(@Language("toml") @Nullable String before,
                                        @Language("toml") @Nullable String after,
                                        Consumer<SourceSpec<Toml.Document>> spec) {
        SourceSpec<Toml.Document> toml = new SourceSpec<>(
                Toml.Document.class, null, PyProjectTomlParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        toml.path("pyproject.toml");
        toml.after(s -> after);
        spec.accept(toml);
        return toml;
    }

    public static SourceSpecs uvLock(@Language("toml") @Nullable String before) {
        return uvLock(before, s -> {
        });
    }

    public static SourceSpecs uvLock(@Language("toml") @Nullable String before,
                                     Consumer<SourceSpec<Toml.Document>> spec) {
        SourceSpec<Toml.Document> toml = new SourceSpec<>(
                Toml.Document.class, null, org.openrewrite.toml.TomlParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        toml.path("uv.lock");
        spec.accept(toml);
        return toml;
    }

    public static SourceSpecs python(@Language("py") @Nullable String before) {
        return python(before, s -> {
        });
    }

    public static SourceSpecs python(@Language("py") @Nullable String before, Consumer<SourceSpec<Py.CompilationUnit>> spec) {
        SourceSpec<Py.CompilationUnit> python = new SourceSpec<>(Py.CompilationUnit.class, null, PythonParser.builder(), before, null);
        spec.accept(python);
        return python;
    }

    public static SourceSpecs python(@Language("py") @Nullable String before, @Language("py") String after) {
        return python(before, after, s -> {
        });
    }

    public static SourceSpecs python(@Language("py") @Nullable String before, @Language("py") String after,
                                     Consumer<SourceSpec<Py.CompilationUnit>> spec) {
        SourceSpec<Py.CompilationUnit> python = new SourceSpec<>(Py.CompilationUnit.class, null, PythonParser.builder(), before, s -> after);
        spec.accept(python);
        return python;
    }
}
