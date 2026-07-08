/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.golang.tree.GoSum;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Parses Go {@code go.sum} files into a lossless {@link GoSum} LST.
 * <p>
 * Like {@link GoModParser}, the actual parsing happens on the Go side (which
 * ships the tree over RPC); this is the Java-side client. go.sum is a flat list
 * of {@code module version[/go.mod] h1:hash} lines with no structured metadata of
 * its own — the module's {@code GoResolutionResult} is attached as a marker by
 * the Go server when a sibling go.mod is available in the same parse batch.
 */
public class GoSumParser implements Parser {

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        return rpc.parse(sources, relativeTo, this, GoSum.class.getName(), ctx);
    }

    @Override
    public boolean accept(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && "go.sum".equals(fileName.toString());
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("go.sum");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        public Builder() {
            super(GoSum.class);
        }

        @Override
        public GoSumParser build() {
            return new GoSumParser();
        }

        @Override
        public String getDslName() {
            return "gosum";
        }
    }
}
