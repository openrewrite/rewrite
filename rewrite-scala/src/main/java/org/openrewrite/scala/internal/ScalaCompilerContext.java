/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.Parser;
import org.openrewrite.ParseWarning;
import org.openrewrite.Tree;
import org.openrewrite.scala.ScalaParsingException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Manages the Scala 3 compiler context and provides methods to parse Scala source files.
 * This class delegates to the Scala bridge for compiler interaction.
 */
public class ScalaCompilerContext {
    private final SimpleScalaCompilerBridge bridge;
    private final ExecutionContext executionContext;

    public ScalaCompilerContext(@Nullable Collection<Path> classpath, 
                                boolean logCompilationWarningsAndErrors,
                                ExecutionContext executionContext) {
        this.executionContext = executionContext;
        
        // Convert classpath to Java list
        List<Path> classpathList = classpath != null ? new ArrayList<>(classpath) : new ArrayList<>();
        
        // Initialize the simple Scala compiler bridge
        this.bridge = new SimpleScalaCompilerBridge();
    }

    /**
     * Parses a single Scala source file and returns its parse result.
     */
    public ParseResult parse(Parser.Input input) throws IOException {
        // Get the source content
        String content = input.getSource(executionContext).readFully();
        String path = input.getPath().toString();
        
        // Parse using the simple Scala bridge
        SimpleParseResult result = bridge.parse(path, content);
        
        // Convert warnings
        List<ParseWarning> warnings = new ArrayList<>();
        for (int i = 0; i < result.warnings().size(); i++) {
            SimpleWarning w = result.warnings().get(i);
            warnings.add(new ParseWarning(Tree.randomId(), 
                input.getPath().toString() + " at line " + w.line() + ":" + w.column() + " " + w.message()));
        }
        
        return new ParseResult(result, warnings);
    }


    /**
     * Result of parsing a Scala source file.
     */
    public static class ParseResult {
        private final SimpleParseResult parseResult;
        private final List<ParseWarning> warnings;

        public ParseResult(SimpleParseResult parseResult, List<ParseWarning> warnings) {
            this.parseResult = parseResult;
            this.warnings = warnings;
        }

        public SimpleParseResult getParseResult() {
            return parseResult;
        }

        public List<ParseWarning> getWarnings() {
            return warnings;
        }
    }
}