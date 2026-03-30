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
package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Trace {
    private static final Trace.StackElementSourceLookup sourceLookup = new Trace.StackElementSourceLookup();

    public static void traceReceiver(RpcObjectData message, @Nullable PrintStream logFile) {
        if (logFile != null && message.getTrace() != null) {
            logFile.println(message.withoutTrace());
            logFile.println("  " + message.getTrace());
            logFile.println("  " + trace("Receiver"));
            logFile.flush();
        }
    }

    public static String traceSender() {
        return trace("Sender");
    }

    private static String trace(String type) {
        for (StackTraceElement stackElement : Thread.currentThread().getStackTrace()) {
            if (stackElement.getClassName().endsWith(type)) {
                return stackElement.getClassName().substring(stackElement.getClassName().lastIndexOf('.') + 1) +
                       ":" + stackElement.getLineNumber() + " => " +
                       sourceLookup.getSource(stackElement).trim();
            }
        }

        return "Unknown";
    }

    /**
     * A utility class to look up the source code of a given class based on its stack trace element.
     */
    public static class StackElementSourceLookup {
        private final Map<String, Path> sourceByClassName = new HashMap<>();
        private @Nullable Path repositoryRoot = Paths.get(".").toAbsolutePath();

        public StackElementSourceLookup() {
            while (repositoryRoot != null && !repositoryRoot.resolve(".git").toFile().exists()) {
                repositoryRoot = repositoryRoot.getParent();
            }
        }

        public String getSource(StackTraceElement element) {
            String className = element.getClassName();
            Path sourcePath = sourceByClassName.computeIfAbsent(className, k -> lookupSourcePath(className));
            if (sourcePath == null) {
                return "Source code unavailable";
            }
            try {
                List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
                int lineNum = element.getLineNumber();
                if (lineNum <= lines.size()) {
                    return lines.get(lineNum - 1);
                } else {
                    return "Line number " + lineNum + " is out of bounds for " + sourcePath;
                }
            } catch (IOException ignored) {
                return "Source code unavailable";
            }
        }

        private @Nullable Path lookupSourcePath(String className) {
            if (repositoryRoot == null) {
                // In case the current working directory was not inside a Git repository
                return null;
            }
            String relativePath = className.replace('.', File.separatorChar) + ".java";
            try (Stream<Path> paths = Files.walk(repositoryRoot)) {
                return paths
                        .filter(p -> p.toString().endsWith(relativePath))
                        .findFirst()
                        .orElse(null);
            } catch (IOException ignored) {
                return null;
            }
        }
    }
}
