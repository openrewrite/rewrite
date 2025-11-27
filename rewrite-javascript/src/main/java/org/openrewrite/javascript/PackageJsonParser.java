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
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.json.tree.Json;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Parser for package.json files that delegates to the TypeScript PackageJsonParser via RPC.
 * This parser attaches a {@link org.openrewrite.javascript.marker.NodeResolutionResult} marker
 * to the parsed JSON document containing npm package metadata and resolved dependencies.
 */
public class PackageJsonParser implements Parser {

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return JavaScriptRewriteRpc.getOrStart().parse(sources, relativeTo, this,
                Json.Document.class.getName(), ctx);
    }

    @Override
    public boolean accept(Path path) {
        return "package.json".equals(path.getFileName().toString());
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("package.json");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        Builder() {
            super(Json.Document.class);
        }

        @Override
        public PackageJsonParser build() {
            return new PackageJsonParser();
        }

        @Override
        public String getDslName() {
            return "package.json";
        }
    }
}
