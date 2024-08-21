/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.benchmarks.java;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@State(Scope.Benchmark)
public class JavaFiles {
    List<Path> sourceFiles;

    @Setup(Level.Trial)
    public void setup() throws URISyntaxException, IOException {
        Path dir = Files.createTempDirectory("java-files-benchmark");

        Path test = dir.resolve("test");
        if(!test.toFile().mkdirs()) {
            throw new RuntimeException("Unable to create directory");
        }

        sourceFiles = new ArrayList<>(1_000);
        for (int i = 0; i < 1_000; i++) {
            Files.writeString(test.resolve("Test" + i + ".java"),
                    "package test; class Test" + i + " {}");
        }
    }

    @TearDown(Level.Trial)
    public void tearDown(Blackhole hole) {
        hole.consume(sourceFiles.size());
    }

    public List<Path> getSourceFiles() {
        return sourceFiles;
    }
}
