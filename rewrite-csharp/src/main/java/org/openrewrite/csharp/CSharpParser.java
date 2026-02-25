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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CSharpParser implements Parser {
    private final List<String> assemblyReferences;

    private static final String TEMP_CSPROJ_TEMPLATE =
            "<Project Sdk=\"Microsoft.NET.Sdk\">\n" +
            "  <PropertyGroup>\n" +
            "    <TargetFramework>net10.0</TargetFramework>\n" +
            "  </PropertyGroup>%s\n" +
            "</Project>\n";

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        List<Input> inputs = new ArrayList<>();
        for (Input input : sources) {
            inputs.add(input);
        }

        if (inputs.isEmpty()) {
            return Stream.empty();
        }

        try {
            // Create a temporary project directory with a .csproj and the source files
            Path tempDir = Files.createTempDirectory("csharp-parse-");
            Path csproj = tempDir.resolve("Temp.csproj");

            // Build PackageReference elements for assembly references
            StringBuilder packageRefs = new StringBuilder();
            if (!assemblyReferences.isEmpty()) {
                packageRefs.append("\n  <ItemGroup>");
                for (String ref : assemblyReferences) {
                    if (ref.contains("@")) {
                        String[] parts = ref.split("@", 2);
                        packageRefs.append(String.format(
                                "\n    <PackageReference Include=\"%s\" Version=\"%s\" />",
                                parts[0], parts[1]));
                    }
                }
                packageRefs.append("\n  </ItemGroup>");
            }

            writeString(csproj, String.format(TEMP_CSPROJ_TEMPLATE, packageRefs));

            // Write source files to the temp directory
            for (Input input : inputs) {
                String sourcePath = relativeTo != null
                        ? relativeTo.relativize(input.getPath()).toString()
                        : input.getPath().getFileName().toString();
                Path target = tempDir.resolve(sourcePath);
                Files.createDirectories(target.getParent());
                try (InputStream is = input.getSource(ctx)) {
                    byte[] buf = new byte[4096];
                    StringBuilder sb = new StringBuilder();
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                    }
                    writeString(target, sb.toString());
                }
            }

            return CSharpRewriteRpc.getOrStart().parseSolution(csproj, tempDir, ctx);
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

    private static void writeString(Path path, String content) throws IOException {
        try (OutputStream os = Files.newOutputStream(path)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
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
