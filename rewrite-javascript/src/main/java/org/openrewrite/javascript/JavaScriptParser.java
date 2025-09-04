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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.rpc.JavaScriptValidator;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.unmodifiableList;

@RequiredArgsConstructor
public class JavaScriptParser implements Parser {
    private final long maxSizeBytes;

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        // Split inputs based on file size (2MB threshold)
        List<Input> smallFiles = new ArrayList<>();
        List<Input> largeFiles = new ArrayList<>();

        for (Input input : sources) {
            if (input.getFileAttributes() != null && input.getFileAttributes().getSize() > maxSizeBytes) {
                // File is larger than 2MB, parse with PlainTextParser
                largeFiles.add(input);
            } else {
                // File is 2MB or smaller, parse normally
                smallFiles.add(input);
            }
        }

        // Parse large files with PlainTextParser
        Stream<SourceFile> largeFileStream = Stream.empty();
        if (!largeFiles.isEmpty()) {
            PlainTextParser plainTextParser = PlainTextParser.builder().build();
            largeFileStream = plainTextParser.parseInputs(largeFiles, relativeTo, ctx);
        }

        // Parse small files with the normal JavaScript parser
        Stream<SourceFile> smallFileStream = Stream.empty();
        if (!smallFiles.isEmpty()) {
            JavaScriptValidator<Integer> validator = new JavaScriptValidator<>();
            smallFileStream = JavaScriptRewriteRpc.getOrStart().parse(smallFiles, relativeTo, this, ctx).map(source -> {
                try {
                    validator.visit(source, 0);
                    return source;
                } catch (Exception e) {
                    Optional<Input> input = StreamSupport.stream(smallFiles.spliterator(), false)
                            .filter(i -> i.getRelativePath(relativeTo).equals(source.getSourcePath()))
                            .findFirst();
                    return ParseError.build(this, input.orElseThrow(NoSuchElementException::new), relativeTo, ctx, e);
                }
            });
        }

        // Combine both streams
        return Stream.concat(largeFileStream, smallFileStream);
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

    public static class Builder extends Parser.Builder {
        long maxSizeBytes = 1024 * 1024; // 1MB

        Builder() {
            super(JS.CompilationUnit.class);
        }

        /**
         * Set the maximum file size (in bytes) for files to be parsed as JavaScript/TypeScript.
         * Files exceeding this size will be parsed as plain text to avoid performance issues.
         * An example of such a huge file (>100k lines of code) is in
         * <a href="https://github.com/denoland/deno/blob/main/cli/tsc/00_typescript.js">deno</a>.
         *
         * @param maxSizeBytes The maximum size of a file in bytes that will be parsed as JavaScript.
         * @return This builder.
         */
        public Builder maxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        @Override
        public JavaScriptParser build() {
            return new JavaScriptParser(maxSizeBytes);
        }

        @Override
        public String getDslName() {
            return "javascript";
        }
    }
}
