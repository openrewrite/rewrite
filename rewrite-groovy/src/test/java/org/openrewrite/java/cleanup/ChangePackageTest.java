/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;

public class ChangePackageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePackage("a.b", "x.y", true));
    }

    @SuppressWarnings("GrPackage")
    @Test
    void changePackage() {
        rewriteRun(
          groovy(
            """
            package a.b
            class Original {}
            """,
            """
            package x.y
            class Original {}
            """
          ),
          groovy(
            """
            import a.b.Original
            
            class A {
                Original type
            }
            """,
            """
            import x.y.Original
            
            class A {
                Original type
            }
            """
          )
        );
    }

    @SuppressWarnings("GrPackage")
    @Test
    void fullyQualified() {
        rewriteRun(
          groovy(
            """
            package a.b
            class Original {}
            """,
            """
            package x.y
            class Original {}
            """
          ),
          groovy(
            """
            class A {
                a.b.Original type
            }
            """,
            """
            class A {
                x.y.Original type
            }
            """
          )
        );
    }

    @Test
    void renamePackageRecursive() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.openrewrite", "org.openrewrite.test", true)),
          java(
            """
              package org.openrewrite.internal;
              class Test {
              }
              """,
            """
              package org.openrewrite.test.internal;
              class Test {
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getSourcePath()).isEqualTo(Paths.get("org/openrewrite/test/internal/Test.java"));
                assertThat(cu.findType("org.openrewrite.internal.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.internal.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void changeDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.foo", "x.y.z", false)),
          groovy(
            """
            package org.foo
            class file {
            }
            """,
            """
            package x.y.z
            class file {
            }
            """,
            spec -> spec.path("org/foo/file.groovy").afterRecipe(cu -> {
              assertThat("x/y/z/file.groovy").isEqualTo(cu.getSourcePath().toString());
              assertThat(TypeUtils.isOfClassType(cu.getClasses().get(0).getType(), "x.y.z.file")).isTrue();
            })
          )
        );
    }
}
