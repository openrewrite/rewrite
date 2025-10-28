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

@SuppressWarnings({"unused", "DataFlowIssue"})
public class Assertions {

    private Assertions() {
    }

    public static SourceSpecs npm(Path relativeTo, SourceSpecs... sources) {
        // Second pass: run npm install if needed
        boolean alreadyInstalled = false;

        // First pass: write package.json files
        for (SourceSpecs multiSpec : sources) {
            if (multiSpec instanceof SourceSpec) {
                SourceSpec<?> spec = (SourceSpec<?>) multiSpec;
                Path sourcePath = spec.getSourcePath();
                if (sourcePath != null && "package.json".equals(sourcePath.toFile().getName())) {
                    try {
                        Path packageJson = relativeTo.resolve(sourcePath);
                        if (Files.exists(packageJson)) {
                            // If relativeTo is a non-transient directory we can optimize not having
                            // to do npm install if the package.json hasn't changed.
                            if (new String(Files.readAllBytes(packageJson), StandardCharsets.UTF_8).equals(spec.getBefore())) {
                                alreadyInstalled = true;
                                continue;
                            }
                        }
                        Files.write(packageJson, requireNonNull(spec.getBefore()).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }

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
        SourceSpec<JS.CompilationUnit> js = new SourceSpec<>(
                JS.CompilationUnit.class, null, JavaScriptParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        acceptSpec(spec, js);
        return js;
    }

    public static SourceSpecs javascript(@Language("js") @Nullable String before, @Language("js") @Nullable String after) {
        return javascript(before, after, s -> {
        });
    }

    public static SourceSpecs javascript(@Language("js") @Nullable String before, @Language("js") @Nullable String after,
                                         Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        SourceSpec<JS.CompilationUnit> js = new SourceSpec<>(
                JS.CompilationUnit.class, null, JavaScriptParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        ).after(s -> after);
        acceptSpec(spec, js);
        return js;
    }

    public static SourceSpecs jsx(@Language("jsx") @Nullable String before) {
        //noinspection LanguageMismatch
        return javascript(before, s -> {
        });
    }

    public static SourceSpecs jsx(@Language("jsx") @Nullable String before, Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return javascript(before, spec2 -> {
            spec2.path(System.nanoTime() + ".jsx");
            spec.accept(spec2);
        });
    }

    public static SourceSpecs jsx(@Language("jsx") @Nullable String before, @Language("jsx") @Nullable String after) {
        //noinspection LanguageMismatch
        return javascript(before, after, s -> {
        });
    }

    public static SourceSpecs jsx(@Language("jsx") @Nullable String before, @Language("jsx") @Nullable String after,
                                  Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return javascript(before, after, spec2 -> {
            spec2.path(System.nanoTime() + ".jsx");
            spec.accept(spec2);
        });
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before) {
        return typescript(before, s -> {
        });
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before, Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return javascript(before, spec2 -> {
            spec2.path(System.nanoTime() + ".ts");
            spec.accept(spec2);
        });
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before, @Language("ts") @Nullable String after) {
        return typescript(before, after, s -> {
        });
    }

    public static SourceSpecs typescript(@Language("ts") @Nullable String before, @Language("ts") @Nullable String after,
                                         Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return javascript(before, after, spec2 -> {
            spec2.path(System.nanoTime() + ".tsx");
            spec.accept(spec2);
        });
    }

    public static SourceSpecs tsx(@Language("tsx") @Nullable String before) {
        //noinspection LanguageMismatch
        return typescript(before, s -> {
        });
    }

    public static SourceSpecs tsx(@Language("tsx") @Nullable String before, Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return typescript(before, spec2 -> {
            spec2.path(System.nanoTime() + ".tsx");
            spec.accept(spec2);
        });
    }

    public static SourceSpecs tsx(@Language("tsx") @Nullable String before, @Language("tsx") @Nullable String after) {
        return tsx(before, after, s -> {
        });
    }

    public static SourceSpecs tsx(@Language("tsx") @Nullable String before, @Language("tsx") @Nullable String after,
                                  Consumer<SourceSpec<JS.CompilationUnit>> spec) {
        //noinspection LanguageMismatch
        return typescript(before, after, spec2 -> {
            spec2.path(System.nanoTime() + ".tsx");
            spec.accept(spec2);
        });
    }

    private static void acceptSpec(Consumer<SourceSpec<JS.CompilationUnit>> spec, SourceSpec<JS.CompilationUnit> js) {
        Consumer<JS.CompilationUnit> userSuppliedAfterRecipe = js.getAfterRecipe();
        js.afterRecipe(userSuppliedAfterRecipe::accept);
        spec.accept(js);
    }
}
