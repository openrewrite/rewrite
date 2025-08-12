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
import org.openrewrite.internal.ManagedThreadLocal;
import org.openrewrite.javascript.internal.rpc.JavaScriptValidator;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public class JavaScriptParser implements Parser {

    private final JavaScriptRewriteRpc rewriteRpc;

    private JavaScriptParser(JavaScriptRewriteRpc rewriteRpc) {
        this.rewriteRpc = rewriteRpc;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        // Registering `RewriteRpc` due to print-idempotence check
        // Scope is closed using `Stream#onClose()`
        ManagedThreadLocal.Scope<JavaScriptRewriteRpc> scope = JavaScriptRewriteRpc.current().using(rewriteRpc);
        try {
            JavaScriptValidator<Integer> validator = new JavaScriptValidator<>();
            return rewriteRpc.parse(sources, relativeTo, this, ctx)
                    .map(source -> {
                        try {
                            validator.visit(source, 0);
                            return source;
                        } catch (Exception e) {
                            Optional<Input> input = StreamSupport.stream(sources.spliterator(), false)
                                    .filter(i -> i.getRelativePath(relativeTo).equals(source.getSourcePath()))
                                    .findFirst();
                            return ParseError.build(this, input.orElseThrow(NoSuchElementException::new), relativeTo, ctx, e);
                        }
                    })
                    .onClose(scope::close);
        } catch (Exception e) {
            scope.close();
            throw e;
        }
    }

    private final static List<String> EXTENSIONS = unmodifiableList(Arrays.asList(
            ".js", ".jsx", ".mjs", ".cjs",
            ".ts", ".tsx", ".mts", ".cts"
    ));

    // Exclude Yarn's Plug'n'Play loader files (https://yarnpkg.com/features/pnp)
    private final static List<String> EXCLUSIONS = unmodifiableList(Arrays.asList(
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
        return prefix.resolve("source.ts");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends org.openrewrite.Parser.Builder {
        private @Nullable JavaScriptRewriteRpc client;

        Builder() {
            super(JS.CompilationUnit.class);
        }

        public Builder rewriteRpc(JavaScriptRewriteRpc rewriteRpc) {
            this.client = rewriteRpc;
            return this;
        }

        @Override
        public JavaScriptParser build() {
            return new JavaScriptParser(requireNonNull(client));
        }

        @Override
        public String getDslName() {
            return "javascript";
        }
    }
}
