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
 */
public class GolangParser implements Parser {

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return GoRewriteRpc.getOrStart().parse(sources, relativeTo, this,
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

        public Builder() {
            super(Go.CompilationUnit.class);
        }

        @Override
        public GolangParser build() {
            return new GolangParser();
        }

        @Override
        public String getDslName() {
            return "go";
        }
    }
}
