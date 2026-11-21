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
import org.openrewrite.internal.EncodingDetectingInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CSharpParser implements Parser {
    private final List<String> assemblyReferences;

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        List<Input> inputs = new ArrayList<>();
        for (Input input : sources) {
            inputs.add(keepingByteOrderMark(input, ctx));
        }
        if (inputs.isEmpty()) {
            return Stream.empty();
        }

        Map<String, String> options = new HashMap<>();
        options.put(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT,
                String.valueOf((boolean) ctx.getMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, true)));
        options.put("org.openrewrite.csharp.assemblyReferences", String.join("\n", assemblyReferences));
        return CSharpRewriteRpc.getOrStart().parse(inputs, relativeTo == null ? null : relativeTo.toAbsolutePath(),
                this, Cs.CompilationUnit.class.getName(), ctx, options);
    }

    /**
     * The text sent for an input not read from disk is what its stream decodes, and the stream
     * consumes a byte order mark. The C# side takes a leading one in that text as the file being
     * marked, so a marked input gets a second mark for the stream to consume.
     */
    private static Input keepingByteOrderMark(Input input, ExecutionContext ctx) {
        if (!input.isSynthetic() && Files.isRegularFile(input.getPath())) {
            return input;
        }
        try (EncodingDetectingInputStream is = input.getSource(ctx)) {
            // The first read settles whether the input carries a byte order mark.
            //noinspection ResultOfMethodCallIgnored
            is.read();
            if (!is.isCharsetBomMarked()) {
                return input;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (EncodingDetectingInputStream is = input.getSource(ctx)) {
            String source = is.readFully();
            return new Input(input.getPath(), input.getFileAttributes(),
                    () -> new ByteArrayInputStream(("\uFEFF\uFEFF" + source).getBytes(StandardCharsets.UTF_8)),
                    input.isSynthetic());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
     * Parses a .sln or .csproj file via MSBuildWorkspace.
     * The C# side loads the solution/project, resolves all references,
     * discovers source files, and parses with correct type attribution.
     *
     * @param path    Path to the .sln or .csproj file
     * @param rootDir Repository root for computing relative source paths
     * @param ctx     Execution context for parsing
     * @return Stream of parsed source files
     */
    public static Stream<SourceFile> parseSolution(Path path, Path rootDir, ExecutionContext ctx) {
        return CSharpRewriteRpc.getOrStart().parseSolution(path, rootDir, ctx);
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
            this.assemblyReferences.addAll(java.util.Arrays.asList(assemblyReferences));
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
