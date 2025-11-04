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
        String packageJsonContent = null;

        // First pass: find package.json content and write it to relativeTo
        for (SourceSpecs multiSpec : sources) {
            if (multiSpec instanceof SourceSpec) {
                SourceSpec<?> spec = (SourceSpec<?>) multiSpec;
                Path sourcePath = spec.getSourcePath();
                if (sourcePath != null && "package.json".equals(sourcePath.toFile().getName())) {
                    packageJsonContent = spec.getBefore();
                    try {
                        Path packageJson = relativeTo.resolve(sourcePath);
                        Files.write(packageJson, requireNonNull(packageJsonContent).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    break;
                }
            }
        }

        // Second pass: get or create cached workspace and symlink node_modules
        if (packageJsonContent != null) {
            Path workspaceDir = DependencyWorkspace.getOrCreateWorkspace(packageJsonContent);
            Path nodeModulesSource = workspaceDir.resolve("node_modules");
            Path nodeModulesTarget = relativeTo.resolve("node_modules");

            try {
                if (Files.exists(nodeModulesSource) && !Files.exists(nodeModulesTarget)) {
                    Files.createSymbolicLink(nodeModulesTarget, nodeModulesSource);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create symlink for node_modules", e);
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
