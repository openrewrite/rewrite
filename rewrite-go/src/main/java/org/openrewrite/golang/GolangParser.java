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
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.tree.Go;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Parser for Go source files.
 * <p>
 * This parser uses RPC to communicate with a Go process that performs the actual parsing.
 * The Go process uses Go's standard library parser to parse source code and converts it to
 * the OpenRewrite LST format.
 * <p>
 * When constructed with module + go.mod context (via {@link Builder#module(String)} and
 * {@link Builder#goMod(String)}), the parser routes batches through
 * {@link GoRewriteRpc#parseWithProject} so the Go server builds a ProjectImporter
 * for type attribution: intra-project imports resolve against sibling sources, and
 * imports of go.mod-declared third-party modules resolve to stub
 * {@code *types.Package} objects.
 */
public class GolangParser implements Parser {

    private final @Nullable String module;
    private final @Nullable String goModContent;

    GolangParser(@Nullable String module, @Nullable String goModContent) {
        this.module = module;
        this.goModContent = goModContent;
    }

    public GolangParser() {
        this(null, null);
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        if (module != null && !module.isEmpty()) {
            return rpc.parseWithProject(sources, relativeTo, this,
                    Go.CompilationUnit.class.getName(), ctx, module, goModContent);
        }
        return rpc.parse(sources, relativeTo, this,
                Go.CompilationUnit.class.getName(), ctx);
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".go");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.go");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        private @Nullable String module;
        private @Nullable String goModContent;

        public Builder() {
            super(Go.CompilationUnit.class);
        }

        /**
         * Set the Go module path (e.g. {@code example.com/foo}) so the
         * parser asks the Go server to build a project-aware Importer.
         */
        public Builder module(@Nullable String module) {
            this.module = module;
            return this;
        }

        /**
         * Set the raw go.mod content. Used by the Go server to register
         * {@code require} directives so imports of those modules resolve
         * to stub packages even when their sources aren't present.
         */
        public Builder goMod(@Nullable String goModContent) {
            this.goModContent = goModContent;
            return this;
        }

        @Override
        public GolangParser build() {
            return new GolangParser(module, goModContent);
        }

        @Override
        public String getDslName() {
            return "go";
        }
    }
}
