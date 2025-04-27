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
package org.openrewrite.rpc.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A utility class to look up the source code of a given class based on its stack trace element.
 */
public class StackElementSourceLookup {
    private final Map<String, Path> sourceByClassName = new HashMap<>();
    private Path repositoryRoot = Paths.get(".").toAbsolutePath();

    public StackElementSourceLookup() {
        if (isSourceAvailable()) {
            while (!repositoryRoot.resolve(".git").toFile().exists()) {
                repositoryRoot = repositoryRoot.getParent();
            }
        }
    }

    public static boolean isSourceAvailable() {
        return System.getProperties().containsKey("intellij.debug.agent");
    }

    public String getSource(StackTraceElement element) {
        if (!isSourceAvailable()) {
            return "Source code unavailable";
        }
        String className = element.getClassName();
        Path sourcePath = sourceByClassName.computeIfAbsent(className, k -> lookupSourcePath(className));
        try {
            List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
            int lineNum = element.getLineNumber();
            if (lineNum <= lines.size()) {
                return lines.get(lineNum - 1);
            } else {
                return "Line number " + lineNum + " is out of bounds for " + sourcePath;
            }
        } catch (IOException e) {
            return "Error reading source file for class " + className + ": " + e.getMessage();
        }
    }

    private Path lookupSourcePath(String className) {
        String relativePath = className.replace('.', File.separatorChar) + ".java";
        try (Stream<Path> paths = Files.walk(repositoryRoot)) {
            return paths
                    .filter(p -> p.toString().endsWith(relativePath))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source file for class " + className + " not found under " + repositoryRoot));
        } catch (IOException e) {
            throw new RuntimeException("Error searching for source file for class " + className, e);
        }
    }
}
