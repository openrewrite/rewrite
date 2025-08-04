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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.PathUtils;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class ChangePackageTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePackage("a.b", "x.y", false));
    }

    @DocumentExample
    @Test
    void changePackage() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original
              """,
            """
              package x.y
              class Original
              """
          ),
          kotlin(
            """
              import a.b.Original

              class A {
                  val type = Original()
              }
              """,
            """
              import x.y.Original

              class A {
                  val type = Original()
              }
              """
          )
        );
    }

    @Test
    void fullyQualified() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original
              """,
            """
              package x.y
              class Original
              """
          ),
          kotlin(
            """
              class A {
                  val type = a.b.Original()
              }
              """,
            """
              class A {
                  val type = x.y.Original()
              }
              """
          )
        );
    }

    @Test
    void renamePackageRecursive() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.foo", "org.foo.test", true)),
          kotlin(
            """
              package org.foo.internal
              class Test
              """,
            """
              package org.foo.test.internal
              class Test
              """,
            spec -> spec.path("org/foo/internal/Test.kt").afterRecipe(cu -> {
                assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("org/foo/test/internal/Test.kt");
                assertThat(TypeUtils.isOfClassType(cu.getClasses().getFirst().getType(), "org.foo.test.internal.Test")).isTrue();
            })
          )
        );
    }

    @Test
    void changeDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.foo", "x.y.z", false)),
          kotlin(
            """
              package org.foo
              class Test
              """,
            """
              package x.y.z
              class Test
              """,
            spec -> spec.path("org/foo/Test.kt").afterRecipe(cu -> {
                assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("x/y/z/Test.kt");
                assertThat(TypeUtils.isOfClassType(cu.getClasses().getFirst().getType(), "x.y.z.Test")).isTrue();
            })
          )
        );
    }

    @Test
    void changePackageNameWithInheritance() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.a", "org.b", false)),
          kotlin(
            """
              package org.a

              open class Animal() {
              }

              class Dog : Animal() {
              }
              """,
            """
              package org.b

              open class Animal() {
              }

              class Dog : Animal() {
              }
              """,
            spec -> spec.path("org/a/Dog.kt").afterRecipe(cu -> {
                assertThat(PathUtils.separatorsToUnix(cu.getSourcePath().toString())).isEqualTo("org/b/Dog.kt");
                assertThat(TypeUtils.isOfClassType(cu.getClasses().getFirst().getType(), "org.b.Animal")).isTrue();
                assertThat(TypeUtils.isOfClassType(cu.getClasses().get(1).getType(), "org.b.Dog")).isTrue();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/433")
    @Test
    void changePackageSecondaryConstructor() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original
              """,
            """
              package x.y
              class Original
              """
          ),
          kotlin(
            """
              import a.b.Original

              class A() : Original() {
                    constructor(s1: String): this()
              }
              """,
            """
              import x.y.Original

              class A() : Original() {
                    constructor(s1: String): this()
              }
              """
          )
        );
    }
}
