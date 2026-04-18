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
package org.openrewrite.scala.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ParseWarning;
import org.openrewrite.Parser;
import org.openrewrite.Tree;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the Scala 3 compiler context and provides methods to parse Scala source files.
 * This class delegates to the Scala bridge for compiler interaction.
 */
public class ScalaCompilerContext {
    private final ScalaCompilerBridge bridge;
    private final ExecutionContext executionContext;
    private final List<String> classpathStrings;

    public ScalaCompilerContext(@Nullable Collection<Path> classpath,
                                boolean logCompilationWarningsAndErrors,
                                ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.bridge = new ScalaCompilerBridge();

        // Convert classpath paths to strings for the Scala bridge
        if (classpath != null && !classpath.isEmpty()) {
            this.classpathStrings = classpath.stream()
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } else {
            this.classpathStrings = Collections.emptyList();
        }
    }

    /**
     * Parses a single Scala source file and returns its parse result (parse-only, no type checking).
     */
    public ParseResult parse(Parser.Input input) {
        String content = input.getSource(executionContext).readFully();
        String path = input.getPath().toString();

        ScalaParseResult result = bridge.parse(path, content);

        List<ParseWarning> warnings = new ArrayList<>();
        for (int i = 0; i < result.warnings().size(); i++) {
            ScalaWarning w = result.warnings().get(i);
            warnings.add(new ParseWarning(Tree.randomId(),
                    input.getPath().toString() + " at line " + w.line() + ":" + w.column() + " " + w.message()));
        }

        return new ParseResult(result, warnings);
    }

    /**
     * Batch-compiles multiple Scala source files with type checking.
     * Returns a map from path string to ParseResult.
     */
    public Map<String, ParseResult> compileAll(List<Parser.Input> inputs) {
        // Build source entries
        List<SourceEntry> entries = new ArrayList<>();
        Map<String, Parser.Input> inputsByPath = new LinkedHashMap<>();

        for (Parser.Input input : inputs) {
            String content = input.getSource(executionContext).readFully();
            String path = input.getPath().toString();
            entries.add(new SourceEntry(path, content));
            inputsByPath.put(path, input);
        }

        // Convert to Java lists for the bridge
        java.util.List<SourceEntry> sourceList = new ArrayList<>(entries);
        java.util.List<String> cpList = new ArrayList<>(classpathStrings);

        // Get a working directory for compiler output (.class files)
        String outputDir;
        try {
            Path root = executionContext.getMessage(WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT);
            if (root == null) {
                root = java.nio.file.Files.createTempDirectory("rewrite-scala");
            }
            outputDir = java.nio.file.Files.createDirectories(root.resolve("scala-compiler")).toString();
        } catch (java.io.IOException e) {
            outputDir = System.getProperty("java.io.tmpdir");
        }

        // Batch compile
        Map<String, ScalaParseResult> compiled = bridge.compileAll(sourceList, cpList, outputDir);

        // Convert to ParseResult map
        Map<String, ParseResult> results = new LinkedHashMap<>();
        for (Map.Entry<String, ScalaParseResult> entry : compiled.entrySet()) {
            String path = entry.getKey();
            ScalaParseResult result = entry.getValue();

            List<ParseWarning> warnings = new ArrayList<>();
            for (int i = 0; i < result.warnings().size(); i++) {
                ScalaWarning w = result.warnings().get(i);
                Parser.Input input = inputsByPath.get(path);
                String location = (input != null ? input.getPath().toString() : path);
                warnings.add(new ParseWarning(Tree.randomId(),
                        location + " at line " + w.line() + ":" + w.column() + " " + w.message()));
            }

            results.put(path, new ParseResult(result, warnings));
        }

        return results;
    }

    /**
     * Result of parsing a Scala source file.
     */
    public static class ParseResult {
        private final ScalaParseResult parseResult;
        private final List<ParseWarning> warnings;

        public ParseResult(ScalaParseResult parseResult, List<ParseWarning> warnings) {
            this.parseResult = parseResult;
            this.warnings = warnings;
        }

        public ScalaParseResult getParseResult() {
            return parseResult;
        }

        public List<ParseWarning> getWarnings() {
            return warnings;
        }
    }
}
