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
package org.openrewrite.coldfusion;

import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfc;

/**
 * Tests whose sources depend on ForgeBox packages, which download once into {@code ~/.rewrite/cache/forgebox}.
 */
class ForgeBoxDependenciesTest implements RewriteTest {

    @Test
    void installsWhereCommandBoxWould() {
        ForgeBoxPackages forgeBox = new ForgeBoxPackages(new HttpUrlConnectionSender());
        ForgeBoxPackages.Installed testbox = forgeBox.install("testbox@^7");
        assertThat(testbox.getInstallPath()).isEqualTo("testbox");
        assertThat(testbox.getVersion()).startsWith("7.");
        assertThat(testbox.getDirectory().resolve("system/BaseSpec.cfc")).isRegularFile();
        assertThat(forgeBox.install("coldbox@^7").getInstallPath()).isEqualTo("coldbox");
        // a ColdBox module installs under modules
        ForgeBoxPackages.Installed cbvalidation = forgeBox.install("cbvalidation");
        assertThat(cbvalidation.getInstallPath()).isEqualTo("modules/cbvalidation");
        assertThat(cbvalidation.getDirectory().resolve("ModuleConfig.cfc")).isRegularFile();
        // the newest release, though ForgeBox writes each release with its build number, as 4.9.1+32
        assertThat(Integer.parseInt(cbvalidation.getVersion().substring(0, cbvalidation.getVersion().indexOf('.'))))
          .isGreaterThanOrEqualTo(4);
    }

    @Test
    void cachedVersionsStandInWhenForgeBoxCannotBeReached() {
        Path cache = Paths.get(System.getProperty("user.home"), ".rewrite", "cache", "forgebox");
        String cached = new ForgeBoxPackages(new HttpUrlConnectionSender(), ForgeBoxPackages.FORGEBOX, cache)
          .install("testbox@^7").getVersion();
        ForgeBoxPackages.Installed offline = new ForgeBoxPackages(new HttpUrlConnectionSender(), "http://127.0.0.1:9/api/v1", cache)
          .install("testbox@^7");
        assertThat(offline.getVersion()).isEqualTo(cached);
        assertThat(offline.getInstallPath()).isEqualTo("testbox");
    }

    @Test
    void javaRecipesFindCallsIntoADependency() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("coldbox.system.FrameworkSupertype getInstance(..)", false))
            .parser(ColdFusionParser.builder().dependencies("coldbox@^7")),
          cfc(
            """
              component extends="coldbox.system.EventHandler" {
                  function index(event) {
                      return getInstance("UserService");
                  }
              }
              """,
            """
              component extends="coldbox.system.EventHandler" {
                  function index(event) {
                      return /*~~>*/getInstance("UserService");
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc")
          )
        );
    }

    @Test
    void testBoxSpecsAreTyped() {
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().dependencies("testbox@^7")),
          cfc(
            """
              component extends="testbox.system.BaseSpec" {
                  function run() {
                      describe("arithmetic", function() {
                          it("adds", function() {
                              expect(1 + 1).toBe(2);
                          });
                      });
                  }
              }
              """,
            spec -> spec.path("tests/specs/MathSpec.cfc").beforeRecipe(cu -> {
                List<String> calls = new ArrayList<>();
                new ColdFusionIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        JavaType.Method type = method.getMethodType();
                        calls.add(type == null ? null : type.getDeclaringType().getFullyQualifiedName() + "." + type.getName());
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);
                // expect returns an Expectation, so the call on what it returns is typed too
                assertThat(calls).containsExactly("testbox.system.BaseSpec.describe", "testbox.system.BaseSpec.it",
                  "testbox.system.Expectation.toBe", "testbox.system.BaseSpec.expect");
            })
          )
        );
    }
}
