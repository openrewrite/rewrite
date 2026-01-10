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
import org.openrewrite.SourceFile;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.tree.ParseError;
import org.openrewrite.ParseExceptionResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

        List<String> errors = new ArrayList<>();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {
            errors.add(t.getMessage());
        });

        List<Docker.File> parsedFiles = new ArrayList<>();
        Map<Path, List<String>> baseImagesByFile = new LinkedHashMap<>();
        Map<Path, String> failedFiles = new LinkedHashMap<>();

        parser.parseInputs(inputs, dir, ctx).forEach(sourceFile -> {
            if (sourceFile instanceof Docker.File dockerFile) {
                parsedFiles.add(dockerFile);
                baseImagesByFile.put(dockerFile.getSourcePath(), extractBaseImages(dockerFile));
            } else if (sourceFile instanceof ParseError parseError) {
                String errorMsg = parseError.getMarkers()
                        .findFirst(ParseExceptionResult.class)
                        .map(ex -> ex.getExceptionType() + ": " + ex.getMessage())
                        .orElse("Unknown error");
                failedFiles.put(parseError.getSourcePath(), errorMsg);
            }
        });

        printSummary(parsedFiles, baseImagesByFile, failedFiles);
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

    private static List<String> extractBaseImages(Docker.File file) {
        List<String> baseImages = new ArrayList<>();
        for (Docker.Stage stage : file.getStages()) {
            String baseImage = formatFromInstruction(stage.getFrom());
            baseImages.add(baseImage);
        }
        return baseImages;
    }

    private static String formatFromInstruction(Docker.From from) {
        StringBuilder sb = new StringBuilder();
        sb.append(extractArgumentText(from.getImageName()));

        if (from.getTag() != null) {
            sb.append(":").append(extractArgumentText(from.getTag()));
        } else if (from.getDigest() != null) {
            sb.append("@").append(extractArgumentText(from.getDigest()));
        }

        if (from.getAs() != null) {
            sb.append(" AS ").append(extractArgumentText(from.getAs().getName()));
        }

        return sb.toString();
    }

    private static String extractArgumentText(Docker.Argument arg) {
        StringBuilder sb = new StringBuilder();
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.PlainText) {
                sb.append(((Docker.PlainText) content).getText());
            } else if (content instanceof Docker.QuotedString) {
                sb.append(((Docker.QuotedString) content).getValue());
            } else if (content instanceof Docker.EnvironmentVariable) {
                Docker.EnvironmentVariable env = (Docker.EnvironmentVariable) content;
                sb.append(env.isBraced() ? "${" + env.getName() + "}" : "$" + env.getName());
            }
        }
        return sb.toString();
    }

    private static void printSummary(List<Docker.File> parsedFiles, Map<Path, List<String>> baseImagesByFile,
                                      Map<Path, String> failedFiles) {
        System.out.println("=".repeat(60));
        System.out.println("Docker File Parsing Summary");
        System.out.println("=".repeat(60));
        System.out.println();
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
                for (Path path : group.getValue().stream().limit(10).toList()) {
                    System.out.println("    - " + path);
                }
                if (group.getValue().size() > 10) {
                    System.out.println("    ... and " + (group.getValue().size() - 10) + " more");
                }
                System.out.println();
            }
        }

        System.out.println("Files and Base Images:");
        System.out.println("-".repeat(60));
        for (Map.Entry<Path, List<String>> entry : baseImagesByFile.entrySet()) {
            System.out.println("  " + entry.getKey());
            for (String baseImage : entry.getValue()) {
                System.out.println("    - " + baseImage);
            }
        }
        System.out.println();

        Set<String> uniqueBaseImages = baseImagesByFile.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        System.out.println("Unique Base Images (" + uniqueBaseImages.size() + "):");
        System.out.println("-".repeat(60));
        for (String baseImage : uniqueBaseImages) {
            System.out.println("  - " + baseImage);
        }
        System.out.println();
        System.out.println("=".repeat(60));
    }
}
