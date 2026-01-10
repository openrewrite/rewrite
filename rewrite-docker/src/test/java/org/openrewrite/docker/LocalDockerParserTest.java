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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

class LocalDockerParserTest {

    private static final String DOCKER_IMAGES_DIR = "/home/tim/Documents/workspace/docker-images/";
    private static final String OUTPUT_FILE = "/tmp/docker-parser-output.txt";

    static boolean dockerImagesDirExists() {
        return Files.isDirectory(Path.of(DOCKER_IMAGES_DIR));
    }

    @Test
    @EnabledIf("dockerImagesDirExists")
    void parseDockerImagesDirectory() throws Exception {
        try (PrintStream out = new PrintStream(new FileOutputStream(OUTPUT_FILE))) {
            PrintStream originalOut = System.out;
            System.setOut(out);
            try {
                LocalDockerParser.main(new String[]{DOCKER_IMAGES_DIR});
            } finally {
                System.setOut(originalOut);
            }
        }
        System.out.println("Output written to: " + OUTPUT_FILE);
    }
}
