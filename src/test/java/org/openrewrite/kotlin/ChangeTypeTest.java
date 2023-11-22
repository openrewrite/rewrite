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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class ChangeTypeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeType("a.b.Original", "x.y.Target", true));
    }

    @Test
    void changeImport() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original
              """),
          kotlin(
            """
              import a.b.Original
              
              class A {
                  val type : Original = Original()
              }
              """,
            """
              import x.y.Target
              
              class A {
                  val type : Target = Target()
              }
              """
          )
        );
    }


    @Test
    void changeImportAlias() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original
              """),
          kotlin(
            """
              import a.b.Original as MyAlias
              
              class A {
                  val type : MyAlias = MyAlias()
              }
              """,
            """
              import x.y.Target as MyAlias
              
              class A {
                  val type : MyAlias = MyAlias()
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/42")
    @Test
    void changeTypeWithGenericArgument() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original<A>
              """),
          kotlin(
            """
              package x.y
              class Target<A>
              """),
          kotlin(
            """
              package example
              
              import a.b.Original
              
              fun test(original: Original<String>) { }
              """,
            """
              package example
              
              import x.y.Target
              
              fun test(original: Target<String>) { }
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/42")
    @Test
    void changeTypeWithGenericArgumentAlias() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original<A>
              """),
          kotlin(
            """
              package x.y
              class Target<A>
              """),
          kotlin(
            """
              package example
              
              import a.b.Original as MyAlias
              
              fun test(original: MyAlias<String>) { }
              """,
            """
              package example
              
              import x.y.Target as MyAlias
              
              fun test(original: MyAlias<String>) { }
              """
          )
        );
    }

    @Test
    void changeTypeWithGenericArgumentFullyQualified() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original<A>
              """),
          kotlin(
            """
              package x.y
              class Target<A>
              """),
          kotlin(
            """
              package example
              
              fun test(original: a.b.Original<String>) { }
              """,
            """
              package example

              fun test(original: x.y.Target<String>) { }
              """
          )
        );
    }

    @Test
    void changeType() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Original
              """),
          kotlin(
            """
              class A {
                  val type : a.b.Original = a.b.Original()
              }
              """,
            """
              class A {
                  val type : x.y.Target = x.y.Target()
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void changeDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("file", "newFile", false)),
          kotlin(
            """
              class file {
              }
              """,
            """
              class newFile {
              }
              """,
            spec -> spec.path("file.kt").afterRecipe(cu ->
                assertThat(TypeUtils.isOfClassType(cu.getClasses().get(0).getType(), "newFile")).isTrue())
          )
        );
    }

    @Test
    void addImportAlias() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              import java.util.ArrayList as MyList
              import java.util.ArrayList

              fun main() {
                  val list = ArrayList<String>()
                  val list2 = MyList<String>()
              }
              """,
            """
              import java.util.LinkedList as MyList
              import java.util.LinkedList

              fun main() {
                  val list = LinkedList<String>()
                  val list2 = MyList<String>()
              }
              """
          )
        );
    }

    @Test
    void updateImportAlias() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              import java.util.ArrayList as MyList
              import java.util.ArrayList

              fun main() {
                  val list = ArrayList<String>()
                  val list2 = MyList<String>()
              }
              """,
            """
              import java.util.LinkedList as MyList
              import java.util.LinkedList

              fun main() {
                  val list = LinkedList<String>()
                  val list2 = MyList<String>()
              }
              """
          )
        );
    }

    @Test
    void usingAliasOnly() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              import java.util.ArrayList as MyList

              fun main() {
                  val list2 = MyList<String>()
              }
              """,
            """
              import java.util.LinkedList as MyList

              fun main() {
                  val list2 = MyList<String>()
              }
              """
          )
        );
    }

    @Test
    void implicitImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              fun main() {
                  val list = ArrayList<String>()
              }
              """,
            """
              import java.util.LinkedList

              fun main() {
                  val list = LinkedList<String>()
              }
              """
          )
        );
    }

    @Test
    void qualifiedReference() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              import java.util.ArrayList as MyList

              fun main() {
                  val list2 = java.util.ArrayList<String>()
              }
              """,
            """


              fun main() {
                  val list2 = java.util.LinkedList<String>()
              }
              """
            , SourceSpec::noTrim
          )
        );
    }

    @Disabled
    @Test
    void fromLibrary() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType(
            "misk.metrics.backends.prometheus.v2.PrometheusMetrics",
              "misk.metrics.v2.Metrics", true))
            .parser(KotlinParser.builder()
              .classpath("misk-prometheus", "misk-metrics"))
          ,
          kotlin(
            """
              import misk.metrics.backends.prometheus.v2.PrometheusMetrics
              
              class A(val a: PrometheusMetrics)
              """,
            """
              import java.util.LinkedList as MyList
              
              class A(val a: Metrics)
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/433")
    @Test
    void changeImportSecondaryConstructor() {
        rewriteRun(
          kotlin(
            """
              package a.b
              
              open class Original
              """),
          kotlin(
            """
              import a.b.Original
              
              class A() : Original() {
                    constructor(s1: String): this()
              }
              """,
            """
              import x.y.Target
              
              class A() : Target() {
                    constructor(s1: String): this()
              }
              """
          )
        );
    }

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/433")
    @Test
    void changeAnnotationImport() {
        rewriteRun(
          kotlin(
            """
              package a.b

              annotation class Original
              """),
          kotlin(
            """
              import a.b.Original
              
              @Original
              class A
              """,
            """
              import x.y.Target
              
              @Target
              class A
              """
          )
        );
    }
}
