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
package org.openrewrite.zig;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.zig.rpc.ZigRewriteRpc;
import org.openrewrite.zig.tree.Zig;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Parser for Zig source files.
 * <p>
 * This parser uses RPC to communicate with a Zig process that performs the actual parsing.
 * The Zig process uses Zig's standard library parser to parse source code and converts it to
 * the OpenRewrite LST format.
 */
public class ZigParser implements Parser {

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return ZigRewriteRpc.getOrStart().parse(sources, relativeTo, this,
                Zig.CompilationUnit.class.getName(), ctx);
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".zig");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.zig");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        public Builder() {
            super(Zig.CompilationUnit.class);
        }

        @Override
        public ZigParser build() {
            return new ZigParser();
        }

        @Override
        public String getDslName() {
            return "zig";
        }
    }
}
