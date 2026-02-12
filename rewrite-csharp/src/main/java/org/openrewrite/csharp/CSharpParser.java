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
package org.openrewrite.csharp;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.csharp.tree.Cs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CSharpParser implements Parser {
    private final List<String> assemblyReferences;

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        List<Input> inputs = new ArrayList<>();
        for (Input input : sources) {
            inputs.add(input);
        }

        if (inputs.isEmpty()) {
            return Stream.empty();
        }

        return CSharpRewriteRpc.getOrStart().parse(
                inputs, relativeTo,
                assemblyReferences.isEmpty() ? null : assemblyReferences,
                ctx
        );
    }

    @Override
    public boolean accept(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".cs");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("source.cs");
    }

    /**
     * Parses an entire C# project from a .csproj file.
     * Discovers source files, resolves NuGet references, and includes source-generator
     * output from obj/ for complete type attribution.
     *
     * @param csprojPath Path to the .csproj file
     * @param ctx        Execution context for parsing
     * @return Stream of parsed source files
     */
    public static Stream<SourceFile> parseProject(Path csprojPath, ExecutionContext ctx) {
        return CSharpRewriteRpc.getOrStart().parseProject(csprojPath, ctx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        private final List<String> assemblyReferences = new ArrayList<>();

        Builder() {
            super(Cs.CompilationUnit.class);
        }

        /**
         * Set NuGet package references for type attribution. Each entry is either:
         * <ul>
         *   <li>A NuGet package name with version (e.g., "Newtonsoft.Json@13.0.1")</li>
         *   <li>A direct path to a DLL file</li>
         * </ul>
         *
         * @param assemblyReferences The assembly references
         * @return This builder
         */
        public Builder assemblyReferences(String... assemblyReferences) {
            this.assemblyReferences.addAll(Arrays.asList(assemblyReferences));
            return this;
        }

        @Override
        public CSharpParser build() {
            return new CSharpParser(assemblyReferences);
        }

        @Override
        public String getDslName() {
            return "csharp";
        }
    }
}
