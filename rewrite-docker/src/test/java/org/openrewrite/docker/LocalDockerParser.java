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
package org.openrewrite.docker;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalDockerParser {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: LocalDockerParser <directory>");
            System.exit(1);
        }

        Path dir = Path.of(args[0]);
        if (!Files.isDirectory(dir)) {
            System.err.println("Error: " + dir + " is not a directory");
            System.exit(1);
        }

        DockerParser parser = DockerParser.builder().build();

        List<Path> dockerFiles = findDockerFiles(dir, parser);
        if (dockerFiles.isEmpty()) {
            System.out.println("No Dockerfiles or Containerfiles found in " + dir);
            return;
        }

        List<Parser.Input> inputs = dockerFiles.stream()
                .map(Parser.Input::fromFile)
                .collect(Collectors.toList());

        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        List<SourceFile> parsedFiles = new ArrayList<>();
        Map<Path, String> failedFiles = new LinkedHashMap<>();

        parser.parseInputs(inputs, dir, ctx).forEach(sourceFile -> {
            if (sourceFile instanceof ParseError parseError) {
                String errorMsg = parseError.getMarkers()
                        .findFirst(ParseExceptionResult.class)
                        .map(ex -> ex.getExceptionType() + ": " + ex.getMessage())
                        .orElse("Unknown error");
                failedFiles.put(parseError.getSourcePath(), errorMsg);
            } else {
                parsedFiles.add(sourceFile);
            }
        });

        printSummary(parsedFiles, failedFiles);
    }

    private static List<Path> findDockerFiles(Path dir, DockerParser parser) {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(parser::accept)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + dir, e);
        }
    }

    private static void printSummary(List<SourceFile> parsedFiles, Map<Path, String> failedFiles) {
        System.out.println("Files parsed successfully: " + parsedFiles.size());
        System.out.println("Files failed to parse: " + failedFiles.size());
        System.out.println();

        if (!failedFiles.isEmpty()) {
            System.out.println("FAILED FILES:");
            System.out.println("-".repeat(60));

            // Group failures by error message
            Map<String, List<Path>> errorGroups = new LinkedHashMap<>();
            for (Map.Entry<Path, String> entry : failedFiles.entrySet()) {
                errorGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                        .add(entry.getKey());
            }

            for (Map.Entry<String, List<Path>> group : errorGroups.entrySet()) {
                System.out.println("  ERROR: " + group.getKey());
                System.out.println("  Files affected (" + group.getValue().size() + "):");
                for (Path path : group.getValue()) {
                    System.out.println("    - " + path);
                }
                System.out.println();
            }
        }
    }
}
