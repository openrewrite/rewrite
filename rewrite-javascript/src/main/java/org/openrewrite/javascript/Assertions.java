/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.json.tree.Json;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.json.Assertions.json;

public class Assertions {

    private Assertions() {
    }

    public static SourceSpecs npm(Path relativeTo, SourceSpecs... sources) {
        // First pass: write package.json files
        for (SourceSpecs multiSpec : sources) {
            if (multiSpec instanceof SourceSpec) {
                SourceSpec<?> spec = (SourceSpec<?>) multiSpec;
                Path sourcePath = spec.getSourcePath();
                if (sourcePath != null && sourcePath.toFile().getName().equals("package.json")) {
                    try {
                        Files.write(relativeTo.resolve(sourcePath), requireNonNull(spec.getBefore()).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }

        // Second pass: run npm install if needed
        boolean alreadyInstalled = false;
        for (SourceSpecs multiSpec : sources) {
            if (multiSpec instanceof SourceSpec) {
                SourceSpec<?> spec = (SourceSpec<?>) multiSpec;
                if (!alreadyInstalled && spec.getParser() instanceof JavaScriptParser.Builder) {
                    // Execute npm install to ensure dependencies are available
                    // First check if package.json exists
                    Path packageJsonPath = relativeTo.resolve("package.json");
                    if (!Files.exists(packageJsonPath)) {
                        // Skip npm install if no package.json exists
                        alreadyInstalled = true;
                        continue;
                    }

                    try {
                        ProcessBuilder pb = new ProcessBuilder("npm", "install");
                        pb.directory(relativeTo.toFile());
                        pb.inheritIO();
                        Process process = pb.start();
                        int exitCode = process.waitFor();
                        if (exitCode != 0) {
                            throw new RuntimeException("npm install failed with exit code: " + exitCode + " in directory: " + relativeTo.toFile().getAbsolutePath());
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException("Failed to run npm install in directory: " + relativeTo.toFile().getAbsolutePath(), e);
                    }

                    alreadyInstalled = true;
                }
            }
        }

        return SourceSpecs.dir(relativeTo.toString(), sources);
    }

    public static SourceSpecs packageJson(@Language("json") @Nullable String before) {
        return json(before, spec -> spec.path("package.json"));
    }

    public static SourceSpecs packageJson(@Language("json") @Nullable String before, @Language("json") @Nullable String after) {
        return json(before, after, spec -> spec.path("package.json"));
    }

    public static SourceSpecs packageJson(@Language("json") @Nullable String before, @Language("json") @Nullable String after,
                                          Consumer<SourceSpec<Json.Document>> spec) {
        return json(before, after, s -> {
            s.path("package.json");
            spec.accept(s);
        });
    }

    public static SourceSpecs javascript(@Language("js") @Nullable String before) {
        return javascript(before, s -> {
        });
    }

    public static SourceSpecs javascript(@Language("js") @Nullable String before, Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        SourceSpec<JS.CompilationUnit> js = new SourceSpec<>(JS.CompilationUnit.class, null, JavaScriptParser.builder(), before, null);
        spec.accept(js);
        return js;
    }

    public static SourceSpecs javascript(@Language("js") @Nullable String before, @Language("js") @Nullable String after) {
        return javascript(before, after, s -> {
        });
    }

    public static SourceSpecs javascript(@Language("js") @Nullable String before, @Language("js") @Nullable String after,
                                         Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        SourceSpec<JS.CompilationUnit> js = new SourceSpec<>(JS.CompilationUnit.class, null, JavaScriptParser.builder(), before, s -> after);
        spec.accept(js);
        return js;
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before) {
        return typescript(before, s -> {
        });
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before, Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return javascript(before, spec);
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before, @Language("ts") @Nullable String after) {
        return typescript(before, after, s -> {
        });
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before, @Language("ts") @Nullable String after,
                                         Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return javascript(before, after, spec);
    }
}
