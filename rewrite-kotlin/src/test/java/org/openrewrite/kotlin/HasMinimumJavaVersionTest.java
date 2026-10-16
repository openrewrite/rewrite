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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.search.HasMinimumJavaVersion;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.SourceSpecs.text;

@Issue("https://github.com/moderneinc/customer-requests/issues/2389")
class HasMinimumJavaVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void kotlinOnly() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("17", false)),
          kotlin(
            "class A",
            "/*~~(Java version 17)~~>*/class A",
            spec -> spec.markers(javaVersion(17))
          ),
          kotlin(
            "class B",
            "/*~~(Java version 17)~~>*/class B",
            spec -> spec.markers(javaVersion(17))
          )
        );
    }

    @Test
    void mixedJavaAndKotlin() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("17", false)),
          java(
            "class A {}",
            "/*~~(Java version 17)~~>*/class A {}",
            spec -> spec.markers(javaVersion(17))
          ),
          kotlin(
            "class B",
            "/*~~(Java version 17)~~>*/class B",
            spec -> spec.markers(javaVersion(17))
          )
        );
    }

    @Test
    void kotlinBelowMinimumFailsCheck() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("17", false)),
          java(
            "class A {}",
            spec -> spec.markers(javaVersion(17))
          ),
          kotlin(
            "class B",
            spec -> spec.markers(javaVersion(11))
          )
        );
    }

    @Test
    void kotlinAtLowestVersionIsMarked() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("11", false)),
          java(
            "class A {}",
            spec -> spec.markers(javaVersion(17))
          ),
          kotlin(
            "class B",
            "/*~~(Java version 11)~~>*/class B",
            spec -> spec.markers(javaVersion(11))
          )
        );
    }

    @Test
    void gatesDeclarativeRecipeInKotlinOnlyRepository() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.kotlin.GatedOnJava17
              description: Test.
              preconditions:
                - org.openrewrite.Singleton
                - org.openrewrite.java.search.HasMinimumJavaVersion:
                    version: 17
              recipeList:
                - org.openrewrite.java.search.FindFields:
                    fullyQualifiedTypeName: java.lang.Integer
                    fieldName: MAX_VALUE
                - org.openrewrite.text.FindAndReplace:
                    find: "1"
                    replace: "2"
                    plaintextOnly: true
              """,
            "org.openrewrite.kotlin.GatedOnJava17"
          ),
          kotlin(
            "val i = Integer.MAX_VALUE",
            "val i = /*~~>*/Integer.MAX_VALUE",
            spec -> spec.markers(javaVersion(17))
          ),
          text("1", "2")
        );
    }
}
