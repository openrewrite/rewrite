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
