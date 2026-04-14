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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parses real-world Go files from checked-out repos and measures the
 * success rate. This test doesn't assert a specific pass rate — it
 * reports the breakdown so we can track progress over time.
 */
@Timeout(value = 300, unit = TimeUnit.SECONDS)
class GolangRealWorldParseTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void parseRealWorldRepos() {
        // Parse files from real-world repos if available
        Path repoDir = Paths.get(System.getProperty("user.home"), "go-recipe-test");
        if (!Files.isDirectory(repoDir)) {
            System.out.println("SKIP: ~/go-recipe-test not found. Clone repos first.");
            return;
        }

        // Focused set of mid-size repos. Large repos (cli, consul, prometheus, etcd, containerd)
        // exceed the test process memory/timeout limits and are tested via mod build instead.
        String[] repos = {"mux", "cobra", "logrus", "chi", "websocket", "gin", "viper", "testify", "zap"};
        int totalFiles = 0, totalParsed = 0, totalErrors = 0;
        StringBuilder report = new StringBuilder();

        for (String repo : repos) {
            Path repoPath = repoDir.resolve(repo);
            if (!Files.isDirectory(repoPath)) continue;

            List<Path> goFiles = collectGoFiles(repoPath);
            if (goFiles.isEmpty()) continue;

            GolangParser parser = GolangParser.builder().build();
            List<SourceFile> parsed = parser.parse(goFiles, repoPath, new InMemoryExecutionContext())
                    .collect(Collectors.toList());

            int errors = 0;
            int success = 0;
            List<String> errorFiles = new ArrayList<>();
            for (SourceFile sf : parsed) {
                if (sf instanceof ParseError) {
                    errors++;
                    errorFiles.add(sf.getSourcePath().toString());
                } else {
                    success++;
                    // Verify print round-trip
                    try {
                        String printed = GoRewriteRpc.getOrStart().print(sf);
                        String original = new String(Files.readAllBytes(repoPath.resolve(sf.getSourcePath())));
                        if (!printed.equals(original)) {
                            // Print doesn't match — note it but don't fail
                            errorFiles.add(sf.getSourcePath() + " (print mismatch)");
                        }
                    } catch (Exception e) {
                        errorFiles.add(sf.getSourcePath() + " (print error: " + e.getMessage() + ")");
                    }
                }
            }

            totalFiles += goFiles.size();
            totalParsed += success;
            totalErrors += errors;

            int pct = goFiles.isEmpty() ? 0 : (success * 100 / goFiles.size());
            String line = String.format("%-15s %3d files  %3d parsed  %3d errors  %3d%% success",
                    repo, goFiles.size(), success, errors, pct);
            report.append(line).append("\n");

            if (!errorFiles.isEmpty() && errorFiles.size() <= 10) {
                for (String f : errorFiles) {
                    report.append("  FAIL: ").append(f).append("\n");
                }
            } else if (!errorFiles.isEmpty()) {
                report.append("  (").append(errorFiles.size()).append(" failures omitted)\n");
            }
        }

        int totalPct = totalFiles == 0 ? 0 : (totalParsed * 100 / totalFiles);
        String summary = String.format("%n=== TOTAL: %d files, %d parsed, %d errors, %d%% success ===%n",
                totalFiles, totalParsed, totalErrors, totalPct);
        System.out.println(summary);

        // Write results to a file for easy access
        try {
            Files.writeString(Paths.get("/tmp/go-parse-results.txt"), report.toString() + summary);
        } catch (IOException ignored) {}

        // Assert we're above a minimum threshold — bump this as we improve
        assertThat(totalPct).as("Overall parse success rate").isGreaterThanOrEqualTo(10);
    }

    private List<Path> collectGoFiles(Path root) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();
                    if (name.equals("vendor") || name.equals("testdata") || name.equals(".git")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".go")) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }
}
