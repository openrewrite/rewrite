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
package org.openrewrite.golang.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.openrewrite.golang.Assertions.go;
import static org.openrewrite.test.RewriteTest.toRecipe;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GolangAddImportTest implements RewriteTest {

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

    @Override
    public void defaults(RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder()
          .identifiers(false)
          .methodInvocations(false)
          .build());
    }

    @Test
    void skipsUnreferencedPackage() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v ->
            v.maybeAddImport("org.springframework.security.web.csrf.CookieCsrfTokenRepository"))),
          go(
            """
              package main

              import "fmt"

              func main() {
              \tfmt.Println("hi")
              }
              """
          )
        );
    }

    @Test
    void addsReferencedPackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"Println".equals(m.getSimpleName()) || m.getMethodType() == null ||
                      !(m.getSelect() instanceof J.Identifier)) {
                      return m;
                  }
                  JavaType.FullyQualified strings = JavaType.ShallowClass.build("strings");
                  maybeAddImport("strings", "ToUpper", null, null, true);
                  return m.withSelect(((J.Identifier) m.getSelect()).withSimpleName("strings").withType(strings))
                    .withName(m.getName().withSimpleName("ToUpper"))
                    .withMethodType(m.getMethodType().withDeclaringType(strings).withName("ToUpper"));
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
          go(
            """
              package main

              import "fmt"

              func main() {
              \tfmt.Println("hi")
              }
              """,
            """
              package main

              import (
              \t"fmt"
              \t"strings"
              )

              func main() {
              \tstrings.ToUpper("hi")
              }
              """
          )
        );
    }

    @Test
    void addsLoneAliasedImportUngrouped() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("net/http/pprof", "Handler", null, "_", false))),
          go(
            """
              package main

              func main() {
              }
              """,
            """
              package main

              import _ "net/http/pprof"

              func main() {
              }
              """
          )
        );
    }

    @Test
    void separatesThirdPartyGroup() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("github.com/x/y", "Client", null, null, false))),
          go(
            """
              package main

              import (
              \t"fmt"
              \t"os"
              )

              func main() {
              \tfmt.Println(os.Args)
              }
              """,
            """
              package main

              import (
              \t"fmt"
              \t"os"

              \t"github.com/x/y"
              )

              func main() {
              \tfmt.Println(os.Args)
              }
              """
          )
        );
    }

    @Test
    void addsStdlibAheadOfThirdParty() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("os", "Args", null, null, false))),
          go(
            """
              package main

              import (
              \t"github.com/x/y"
              )

              func main() {
              }
              """,
            """
              package main

              import (
              \t"os"

              \t"github.com/x/y"
              )

              func main() {
              }
              """
          )
        );
    }

    @Test
    void promotesUngroupedLaterDeclaration() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("strings", "ToUpper", null, null, false))),
          go(
            """
              package main

              import "fmt"
              import "os"

              func main() {
              \tfmt.Println(os.Args)
              }
              """,
            """
              package main

              import "fmt"
              import (
              \t"os"
              \t"strings"
              )

              func main() {
              \tfmt.Println(os.Args)
              }
              """
          )
        );
    }

    @Test
    void closesPromotedDeclarationBeforeTheNext() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("os", "Args", null, null, false))),
          go(
            """
              package main

              import "fmt"
              import "github.com/x/y"

              func main() {
              }
              """,
            """
              package main

              import (
              \t"fmt"
              \t"os"
              )
              import "github.com/x/y"

              func main() {
              }
              """
          )
        );
    }

    @Test
    void opensLaterDeclaration() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("os", "Args", null, null, false))),
          go(
            """
              package main

              import "github.com/a/b"

              import (
              \t"github.com/x/y"
              )

              func main() {
              }
              """,
            """
              package main

              import "github.com/a/b"

              import (
              \t"os"

              \t"github.com/x/y"
              )

              func main() {
              }
              """
          )
        );
    }

    @Test
    void blankImportDoesNotSatisfyRegularImport() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("embed", "FS", null, null, false))),
          go(
            """
              package main

              import _ "embed"

              func main() {
              }
              """,
            """
              package main

              import (
              \t_ "embed"
              \t"embed"
              )

              func main() {
              }
              """
          )
        );
    }

    @Test
    void addsAliasedImport() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("github.com/x/y", "Client", null, "yy", false))),
          go(
            """
              package main

              import "fmt"

              func main() {
              \tfmt.Println("hi")
              }
              """,
            """
              package main

              import (
              \t"fmt"

              \tyy "github.com/x/y"
              )

              func main() {
              \tfmt.Println("hi")
              }
              """
          )
        );
    }

    @Test
    void aliasedImportSatisfiesSameAlias() {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport("github.com/x/y", "Client", null, "yy", false))),
          go(
            """
              package main

              import yy "github.com/x/y"

              func main() {
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"github.com/x/y", "gopkg.in/yaml.v3"})
    void addsImportPathPassedAsFullyQualifiedName(String importPath) {
        rewriteRun(
          spec -> spec.recipe(recipeCalling(v -> v.maybeAddImport(importPath, false))),
          go(
            """
              package main

              import "fmt"

              func main() {
              \tfmt.Println("hi")
              }
              """,
            """
              package main

              import (
              \t"fmt"

              \t"%s"
              )

              func main() {
              \tfmt.Println("hi")
              }
              """.formatted(importPath)
          )
        );
    }

    private static Recipe recipeCalling(Consumer<JavaIsoVisitor<ExecutionContext>> maybeAddImport) {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J preVisit(J tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                maybeAddImport.accept(this);
                return tree;
            }
        });
    }
}
