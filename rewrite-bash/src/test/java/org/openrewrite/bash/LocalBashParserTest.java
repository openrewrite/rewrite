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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.openrewrite.SourceFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;

class LocalBashParserTest {

    static final Path BASH_CORPUS = resolveBashCorpus();

    private static Path resolveBashCorpus() {
        // Walk up from user.dir looking for bash-corpus sibling directory
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null) {
            Path candidate = dir.resolve("bash-corpus");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        // Fallback: check system property
        String prop = System.getProperty("bash.corpus.dir");
        return prop != null ? Path.of(prop) : Path.of("bash-corpus");
    }

    static boolean bashCorpusDirExists() {
        return Files.isDirectory(BASH_CORPUS);
    }

    @Test
    @EnabledIf("bashCorpusDirExists")
    void parseBashCorpus() {
        var parseResult = LocalBashParser.parse(BASH_CORPUS);

        List<SourceFile> parsedFiles = parseResult.parsedFiles();
        Map<Path, String> parsedErrors = parseResult.parsedErrors();
        System.out.println("Files parsed successfully: " + parsedFiles.size());
        System.out.println("Files failed to parse: " + parsedErrors.size());
        System.out.println("Total: " + (parsedFiles.size() + parsedErrors.size()));
        System.out.println();

        parsedErrors.entrySet().stream()
                .collect(groupingBy(e -> e.getValue()
                        .replace(e.getKey().toString(), "{}")
                        .replaceAll("at line \\d+:\\d+", "at line {}:{}")))
                .forEach((errorMsg, entries) -> {
                    System.out.println(entries.size() + "x : " + errorMsg);
                    for (var e : entries) {
                        System.out.println("  " + e.getKey());
                    }
                    System.out.println();
                });

        assertThat(parsedFiles).isNotEmpty();
    }
}
