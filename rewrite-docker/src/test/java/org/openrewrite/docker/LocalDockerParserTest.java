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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.openrewrite.SourceFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;

class LocalDockerParserTest {

    static final Path DOCKER_FILES = Path.of("/home/tim/Documents/workspace/docker-images/");

    static boolean dockerImagesDirExists() {
        return Files.isDirectory(DOCKER_FILES);
    }

    @Test
    @EnabledIf("dockerImagesDirExists")
    void parseDockerImagesDirectory() {
        var parseResult = LocalDockerParser.parse(DOCKER_FILES);

        List<SourceFile> parsedFiles = parseResult.parsedFiles();
        Map<Path, String> parsedErrors = parseResult.parsedErrors();
        System.out.println("Files parsed successfully: " + parsedFiles.size());
        System.out.println("Files failed to parse: " + parsedErrors.size());

        parsedErrors.entrySet().stream()
          .collect(groupingBy(e -> e.getValue()
            .replace(e.getKey().toString(), "{}")
            .replaceAll("at line \\d+:\\d+", "at line {}:{}")))
          .forEach((errorMsg, entries) -> {
              System.out.println(entries.size() + "x : " + errorMsg);
              entries.forEach(e -> System.out.println("  " + e.getValue().replace(DOCKER_FILES.toString(), "")));
          });

        assertThat(parsedFiles).hasSizeGreaterThanOrEqualTo(1492);
        assertThat(parsedErrors).hasSizeLessThanOrEqualTo(20);
    }
}
