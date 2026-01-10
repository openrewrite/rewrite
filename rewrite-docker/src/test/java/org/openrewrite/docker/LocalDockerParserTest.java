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
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class LocalDockerParserTest {

    private static final String DOCKER_IMAGES_DIR = "/home/tim/Documents/workspace/docker-images/";

    static boolean dockerImagesDirExists() {
        return Files.isDirectory(Path.of(DOCKER_IMAGES_DIR));
    }

    @Test
    @EnabledIf("dockerImagesDirExists")
    void parseDockerImagesDirectory(@TempDir(cleanup = CleanupMode.NEVER) Path tempDir) throws Exception {
        LocalDockerParser.main(
          DOCKER_IMAGES_DIR,
          tempDir.resolve("docker-parser-output.txt").toString());
    }
}
