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

class AndroidIntentActionTest implements RewriteTest {

    private static final String INTENT_STUB = """
            package android.content;
            public class Intent {
                public Intent() {}
                public Intent(String action) {}
                public Intent setAction(String action) { return this; }
            }
            """;

    private static final String INTENT_STUB_KOTLIN = """
            package android.content

            class Intent {
                constructor()
                constructor(action: String)
                fun setAction(action: String): Intent = this
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new AndroidIntentAction.Matcher().asVisitor(
                (trait, ctx) -> SearchResult.found(trait.getTree(), trait.getAction())
        )));
    }

    @Test
    void javaIntentConstructor() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(INTENT_STUB)),
          java(
            """
              import android.content.Intent;
              class Test {
                  void make() {
                      Intent i = new Intent("android.intent.action.VIEW");
                  }
              }
              """,
            """
              import android.content.Intent;
              class Test {
                  void make() {
                      Intent i = new Intent(/*~~(android.intent.action.VIEW)~~>*/"android.intent.action.VIEW");
                  }
              }
              """
          )
        );
    }

    @Test
    void javaSetAction() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(INTENT_STUB)),
          java(
            """
              import android.content.Intent;
              class Test {
                  void make() {
                      Intent i = new Intent();
                      i.setAction("android.intent.action.SEND");
                  }
              }
              """,
            """
              import android.content.Intent;
              class Test {
                  void make() {
                      Intent i = new Intent();
                      i.setAction(/*~~(android.intent.action.SEND)~~>*/"android.intent.action.SEND");
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinIntentConstructor() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn(INTENT_STUB_KOTLIN)),
          kotlin(
            """
              import android.content.Intent

              fun make() {
                  val i = Intent("android.intent.action.VIEW")
              }
              """,
            """
              import android.content.Intent

              fun make() {
                  val i = Intent(/*~~(android.intent.action.VIEW)~~>*/"android.intent.action.VIEW")
              }
              """
          )
        );
    }

    @Test
    void kotlinSetAction() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn(INTENT_STUB_KOTLIN)),
          kotlin(
            """
              import android.content.Intent

              fun make() {
                  val i = Intent()
                  i.setAction("android.intent.action.SEND")
              }
              """,
            """
              import android.content.Intent

              fun make() {
                  val i = Intent()
                  i.setAction(/*~~(android.intent.action.SEND)~~>*/"android.intent.action.SEND")
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchUnrelatedStringLiterals() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(INTENT_STUB)),
          java(
            """
              class Test {
                  String s = "android.intent.action.VIEW";
                  String t() { return "android.intent.action.SEND"; }
              }
              """
          )
        );
    }
}
