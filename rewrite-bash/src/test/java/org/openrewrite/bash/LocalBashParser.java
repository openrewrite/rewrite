/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.bash;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LocalBashParser {

    public static void main(String... args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: LocalBashParser <directory> <output-file>");
            System.exit(1);
        }

        Path dir = Path.of(args[0]);
        Path outputFile = Path.of(args[1]);
        if (!Files.isDirectory(dir)) {
            System.err.println("Error: " + dir + " is not a directory");
            System.exit(1);
        }

        ParseResult result = parse(dir);
        printSummary(outputFile, result.parsedFiles(), result.parsedErrors());
        System.out.println("Output written to: " + outputFile);
    }

    static ParseResult parse(Path dir) {
        BashParser parser = BashParser.builder().build();

        List<Path> bashFiles = findBashFiles(dir, parser);
        if (bashFiles.isEmpty()) {
            System.err.println("No bash scripts found in " + dir);
            return new ParseResult(List.of(), Map.of());
        }

        List<Parser.Input> inputs = bashFiles.stream()
                .map(Parser.Input::fromFile)
                .collect(toList());

        List<SourceFile> parsedFiles = new ArrayList<>();
        Map<Path, String> parsedErrors = new LinkedHashMap<>();
        List<Throwable> throwables = new ArrayList<>();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(throwables::add);
        parser.parseInputs(inputs, dir, ctx).forEach(sourceFile -> {
            if (sourceFile instanceof ParseError parseError) {
                String errorMsg = parseError.getMarkers()
                        .findFirst(ParseExceptionResult.class)
                        .map(ex -> {
                            String msg = ex.getMessage();
                            int newlineIdx = msg.indexOf('\n');
                            return newlineIdx > 0 ? msg.substring(0, newlineIdx) : msg;
                        })
                        .orElse("Unknown error");
                parsedErrors.put(parseError.getSourcePath(), errorMsg);
            } else {
                parsedFiles.add(sourceFile);
            }
        });
        return new ParseResult(parsedFiles, parsedErrors);
    }

    record ParseResult(
            List<SourceFile> parsedFiles,
            Map<Path, String> parsedErrors) {
    }

    private static List<Path> findBashFiles(Path dir, BashParser parser) {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(parser::accept)
                    .sorted()
                    .collect(toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + dir, e);
        }
    }

    private static void printSummary(Path outputFile, List<SourceFile> parsedFiles, Map<Path, String> parsedErrors) throws Exception {
        try (PrintStream out = new PrintStream(new FileOutputStream(outputFile.toFile()))) {
            out.println("Files parsed successfully: " + parsedFiles.size());
            out.println("Files failed to parse: " + parsedErrors.size());
            out.println();

            if (!parsedErrors.isEmpty()) {
                Map<String, List<Path>> errorGroups = new LinkedHashMap<>();
                for (Map.Entry<Path, String> entry : parsedErrors.entrySet()) {
                    errorGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                            .add(entry.getKey());
                }

                for (Map.Entry<String, List<Path>> group : errorGroups.entrySet()) {
                    out.println("  ERROR: " + group.getKey());
                    out.println("  Files affected (" + group.getValue().size() + "):");
                    for (Path path : group.getValue()) {
                        out.println("    - " + path);
                    }
                    out.println();
                }
            }
        }
    }
}
