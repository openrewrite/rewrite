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
package org.openrewrite.golang;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.golang.internal.ModuleCache;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.golang.Assertions.expectMethodType;
import static org.openrewrite.golang.Assertions.expectType;
import static org.openrewrite.golang.Assertions.go;
import static org.openrewrite.golang.Assertions.goMod;
import static org.openrewrite.golang.Assertions.goProject;

/**
 * A recipe that matches calls into a dependency needs those calls attributed, and the go.mod
 * declaring the dependency is all a test gives it to go on.
 */
@Timeout(value = 300, unit = TimeUnit.SECONDS)
class ThirdPartyTypeAttributionTest implements RewriteTest {

    private static final String GIN_VERSION = "v1.10.0";

    private static final String GO_MOD = """
            module example.com/app

            go 1.21

            require github.com/gin-gonic/gin %s
            """.formatted(GIN_VERSION);

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
          .goBinaryPath(binaryPath)
          .log(tempDir.resolve("go-rpc.log"))
          .traceRpcMessages());
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
          .allowNonWhitespaceInWhitespace(true)
          .build());
    }

    @Test
    void callsIntoARequiredModuleCarryTheirDeclaringType() {
        SourceSpecs project = goProject("app",
          goMod(GO_MOD),
          go(
            """
              package main

              import "github.com/gin-gonic/gin"

              func main() {
              	r := gin.Default()
              	r.GET("/ping", func(c *gin.Context) {
              		c.JSON(200, gin.H{"message": "pong"})
              	})
              }
              """,
            s -> s.afterRecipe(cu -> {
                expectMethodType(cu, "Default", "github.com/gin-gonic/gin");
                expectMethodType(cu, "GET", "github.com/gin-gonic/gin.RouterGroup");
                expectMethodType(cu, "JSON", "github.com/gin-gonic/gin.Context");
                expectType(cu, "r", "github.com/gin-gonic/gin.Engine");
            })));

        // Constructing the project is what downloads gin, so the check for it belongs after.
        assumeTrue(ModuleCache.contains("github.com/gin-gonic/gin", GIN_VERSION),
          "github.com/gin-gonic/gin " + GIN_VERSION + " is not in the Go module cache");

        rewriteRun(project);
    }
}
