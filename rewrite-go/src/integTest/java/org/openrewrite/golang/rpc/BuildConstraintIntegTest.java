/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.golang.marker.BuildConstraint;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class BuildConstraintIntegTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(Paths.get("build/rewrite-go-rpc").toAbsolutePath())
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void goBuildConstraintIsMarkedOnTheCompilationUnit() {
        SourceFile cu = GolangParser.builder().build().parse(
          """
            //go:build linux

            package main

            func main() {
            }
            """).findFirst().orElseThrow();

        assertThat(cu.getMarkers().findFirst(BuildConstraint.class))
          .hasValueSatisfying(m -> assertThat(m.getConstraint()).contains("//go:build linux"));
    }

    @Test
    void unconstrainedCompilationUnitIsUnmarked() {
        SourceFile cu = GolangParser.builder().build().parse(
          """
            package main

            func main() {
            }
            """).findFirst().orElseThrow();

        assertThat(cu.getMarkers().findFirst(BuildConstraint.class)).isEmpty();
    }
}
