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
package org.openrewrite.python;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.rpc.PythonValidator;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.python.tree.Py;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Parser for Python source files.
 * <p>
 * This parser uses RPC to communicate with a Python process that performs the actual parsing.
 * The Python process uses the Python AST module to parse source code and converts it to the
 * OpenRewrite LST format.
 */
@RequiredArgsConstructor
public class PythonParser implements Parser {
    private final long maxSizeBytes;

    /**
     * Per-parse Python language version override. When {@code null}, the RPC
     * server uses its process-wide default (controlled by
     * {@code REWRITE_PYTHON_VERSION} via
     * {@link PythonRewriteRpc.Builder#pythonVersion(String)}).
     */
    private final @Nullable PythonLanguageLevel languageLevel;

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        // Split inputs based on file size threshold
        List<Input> smallFiles = new ArrayList<>();
        List<Input> largeFiles = new ArrayList<>();

        for (Input input : sources) {
            if (input.getFileAttributes() != null && input.getFileAttributes().getSize() > maxSizeBytes) {
                largeFiles.add(input);
            } else {
                smallFiles.add(input);
            }
        }

        // Parse large files with PlainTextParser
        Stream<SourceFile> largeFileStream = Stream.empty();
        if (!largeFiles.isEmpty()) {
            PlainTextParser plainTextParser = PlainTextParser.builder().build();
            largeFileStream = plainTextParser.parseInputs(largeFiles, relativeTo, ctx);
        }

        // Parse small files with the Python parser via RPC
        Stream<SourceFile> smallFileStream = Stream.empty();
        if (!smallFiles.isEmpty()) {
            PythonValidator<Integer> validator = new PythonValidator<>();
            Map<String, String> options = languageLevel != null
                    ? Collections.singletonMap("languageLevel", languageLevel.version())
                    : null;
            smallFileStream = PythonRewriteRpc.getOrStart().parse(smallFiles, relativeTo, this,
                    Py.CompilationUnit.class.getName(), ctx, options).map(source -> {
                try {
                    validator.visit(source, 0);
                    return source;
                } catch (Throwable t) {
                    Optional<Input> input = smallFiles.stream()
                            .filter(i -> i.getRelativePath(relativeTo).equals(source.getSourcePath()))
                            .findFirst();
                    return ParseError.build(this, input.orElseThrow(NoSuchElementException::new), relativeTo, ctx, t);
                }
            });
        }

        return Stream.concat(largeFileStream, smallFileStream);
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.toString();
        return fileName.endsWith(".py") || fileName.endsWith(".pyi");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.py");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        private long maxSizeBytes = 2 * 1024 * 1024; // 2MB default
        private @Nullable PythonLanguageLevel languageLevel;

        public Builder() {
            super(Py.CompilationUnit.class);
        }

        /**
         * Set the maximum file size (in bytes) for files to be parsed as Python.
         * Files exceeding this size will be parsed as plain text to avoid performance issues.
         *
         * @param maxSizeBytes The maximum size of a file in bytes that will be parsed as Python.
         * @return This builder.
         */
        public Builder maxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        /**
         * Per-parse Python language version. Overrides the RPC server's
         * process-wide default for the inputs parsed through this parser
         * instance only.
         * <p>
         * When unset, the server falls back to the version configured on
         * {@link PythonRewriteRpc.Builder#pythonVersion(String)} (default
         * {@link PythonLanguageLevel#PYTHON_3}).
         *
         * @param languageLevel The Python version to parse with.
         * @return This builder.
         */
        public Builder languageLevel(@Nullable PythonLanguageLevel languageLevel) {
            this.languageLevel = languageLevel;
            return this;
        }

        @Override
        public PythonParser build() {
            return new PythonParser(maxSizeBytes, languageLevel);
        }

        @Override
        public String getDslName() {
            return "python";
        }
    }

    /**
     * Python language version selectable on the parser.
     * <p>
     * Only the levels the parser actually branches on are exposed: the Py2.7
     * grammar (via the parso-based path) is distinct from the Py3 grammar
     * (via the {@code ast} module). New minor versions are added as the
     * parser starts to distinguish them.
     */
    public enum PythonLanguageLevel {
        PYTHON_2_7("2.7"),
        PYTHON_3("3");

        private final String version;

        PythonLanguageLevel(String version) {
            this.version = version;
        }

        /**
         * @return The version string sent over the RPC parse request to
         * select the corresponding parser path on the server.
         */
        public String version() {
            return version;
        }
    }
}
