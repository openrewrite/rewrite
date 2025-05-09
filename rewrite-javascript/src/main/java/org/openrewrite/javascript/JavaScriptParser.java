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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.javascript.tree.JS;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class JavaScriptParser implements Parser {

    private final JavaScriptRewriteRpc client;

    private JavaScriptParser(Path nodePath, Path installationDir) {
        this.client = JavaScriptRewriteRpc.start(
                Environment.builder().build(),
                nodePath.toString(),
                "--enable-source-maps",
                // Uncomment this to debug the server
//                "--inspect-brk",
                installationDir.resolve("server.js").toString()
        );
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return client.parse("javascript", sources, relativeTo, ctx).stream();
    }

    private final static List<String> EXTENSIONS = Collections.unmodifiableList(Arrays.asList(
            ".js", /*".jsx",*/ ".mjs", ".cjs",
            ".ts", /*".tsx",*/ ".mts", ".cts"
    ));

    // Exclude Yarn's Plug'n'Play loader files (https://yarnpkg.com/features/pnp)
    private final static List<String> EXCLUSIONS = Collections.unmodifiableList(Arrays.asList(
            ".pnp.cjs", ".pnp.loader.mjs"
    ));

    @Override
    public boolean accept(Path path) {
        if (path.toString().contains("/dist/")) {
            // FIXME this is a workaround to not having tsconfig info
            return false;
        }

        final String filename = path.getFileName().toString().toLowerCase();
        for (String ext : EXTENSIONS) {
            if (filename.endsWith(ext) && !EXCLUSIONS.contains(filename)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("source.js");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends org.openrewrite.Parser.Builder {
        private Path nodePath = Paths.get("node");
        private @Nullable Path installationDir;

        Builder() {
            super(JS.CompilationUnit.class);
        }

        public Builder nodePath(Path path) {
            this.nodePath = path;
            return this;
        }

        public Builder installationDir(Path installationDir) {
            this.installationDir = installationDir;
            return this;
        }

        @Override
        public JavaScriptParser build() {
            return new JavaScriptParser(nodePath, requireNonNull(installationDir));
        }

        @Override
        public String getDslName() {
            return "javascript";
        }
    }
}
