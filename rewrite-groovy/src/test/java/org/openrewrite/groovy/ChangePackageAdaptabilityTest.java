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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.PathUtils;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

/**
 * Prove that {@link ChangePackage}, written for Java, can adapt to working on Groovy code.
 */
class ChangePackageAdaptabilityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePackage("a.b", "x.y", false));
    }

    @DocumentExample
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

    @SuppressWarnings("GrPackage")
    @Test
    void renamePackageRecursive() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.foo", "org.foo.test", true)),
          groovy(
            """
              package org.foo.internal
              class Test {
              }
              """,
            """
              package org.foo.test.internal
              class Test {
              }
              """,
            spec -> spec.path("org/foo/internal/Test.groovy").afterRecipe(cu -> {
                assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("org/foo/test/internal/Test.groovy");
                assertThat(TypeUtils.isOfClassType(cu.getClasses().get(0).getType(), "org.foo.test.internal.Test")).isTrue();
            })
          )
        );
    }

    @SuppressWarnings("GrPackage")
    @Test
    void changeDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.foo", "x.y.z", false)),
          groovy(
            """
              package org.foo
              class Test {
              }
              """,
            """
              package x.y.z
              class Test {
              }
              """,
            spec -> spec.path("org/foo/Test.groovy").afterRecipe(cu -> {
                assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("x/y/z/Test.groovy");
                assertThat(TypeUtils.isOfClassType(cu.getClasses().get(0).getType(), "x.y.z.Test")).isTrue();
            })
          )
        );
    }
}
