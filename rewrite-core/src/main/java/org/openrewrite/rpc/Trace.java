package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;

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

public class Trace {
    private static final Trace.StackElementSourceLookup sourceLookup = new Trace.StackElementSourceLookup();

    public static String traceReceiver() {
        return trace("Receiver");
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
        private Path repositoryRoot = Paths.get(".").toAbsolutePath();

        public StackElementSourceLookup() {
            while (!repositoryRoot.resolve(".git").toFile().exists()) {
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
