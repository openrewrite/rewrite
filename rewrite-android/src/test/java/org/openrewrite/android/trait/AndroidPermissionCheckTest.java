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
package org.openrewrite.android.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AndroidPermissionCheckTest implements RewriteTest {

    private static final String CONTEXT_COMPAT_STUB = """
            package androidx.core.content;
            public class ContextCompat {
                public static int checkSelfPermission(Object context, String permission) { return 0; }
            }
            """;

    private static final String CONTEXT_COMPAT_STUB_KOTLIN = """
            package androidx.core.content

            object ContextCompat {
                @JvmStatic
                fun checkSelfPermission(context: Any, permission: String): Int = 0
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new AndroidPermissionCheck.Matcher().asVisitor(
                (trait, ctx) -> SearchResult.found(trait.getTree(),
                        trait.getPermissionExpression().printTrimmed(trait.getCursor()))
        )));
    }

    @Test
    void javaInstanceCheckSelfPermission() {
        rewriteRun(
          java(
            """
              class Test {
                  int checkSelfPermission(String permission) { return 0; }
                  int run() { return checkSelfPermission("android.permission.CAMERA"); }
              }
              """,
            """
              class Test {
                  int checkSelfPermission(String permission) { return 0; }
                  int run() { return /*~~("android.permission.CAMERA")~~>*/checkSelfPermission("android.permission.CAMERA"); }
              }
              """
          )
        );
    }

    @Test
    void javaContextCompatCheckSelfPermission() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(CONTEXT_COMPAT_STUB)),
          java(
            """
              import androidx.core.content.ContextCompat;
              class Test {
                  int run() {
                      return ContextCompat.checkSelfPermission(this, "android.permission.CAMERA");
                  }
              }
              """,
            """
              import androidx.core.content.ContextCompat;
              class Test {
                  int run() {
                      return /*~~("android.permission.CAMERA")~~>*/ContextCompat.checkSelfPermission(this, "android.permission.CAMERA");
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinInstanceCheckSelfPermission() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun checkSelfPermission(permission: String): Int = 0
                  fun run(): Int = checkSelfPermission("android.permission.CAMERA")
              }
              """,
            """
              class Test {
                  fun checkSelfPermission(permission: String): Int = 0
                  fun run(): Int = /*~~("android.permission.CAMERA")~~>*/checkSelfPermission("android.permission.CAMERA")
              }
              """
          )
        );
    }

    @Test
    void kotlinContextCompatCheckSelfPermission() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn(CONTEXT_COMPAT_STUB_KOTLIN)),
          kotlin(
            """
              import androidx.core.content.ContextCompat

              fun run(ctx: Any): Int =
                  ContextCompat.checkSelfPermission(ctx, "android.permission.CAMERA")
              """,
            """
              import androidx.core.content.ContextCompat

              fun run(ctx: Any): Int =
                  /*~~("android.permission.CAMERA")~~>*/ContextCompat.checkSelfPermission(ctx, "android.permission.CAMERA")
              """
          )
        );
    }

    @Test
    void doesNotMatchUnrelatedMethod() {
        rewriteRun(
          java(
            """
              class Test {
                  int checkOtherPermission(String permission) { return 0; }
                  int run() { return checkOtherPermission("android.permission.CAMERA"); }
              }
              """
          )
        );
    }
}
