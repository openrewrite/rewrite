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
package org.openrewrite.android.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AndroidResourceReferenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new AndroidResourceReference.Matcher().asVisitor(
                (ref, ctx) -> SearchResult.found(ref.getTree(),
                        ref.getResourceType() + ":" + ref.getResourceName())
        )));
    }

    @DocumentExample
    @Test
    void javaResourceReference() {
        rewriteRun(
          java(
            """
              class R {
                static class string { static int app_name = 0; }
                static class id { static int submit = 0; }
              }
              class Test {
                int s() { return R.string.app_name; }
                int b() { return R.id.submit; }
              }
              """,
            """
              class R {
                static class string { static int app_name = 0; }
                static class id { static int submit = 0; }
              }
              class Test {
                int s() { return /*~~(string:app_name)~~>*/R.string.app_name; }
                int b() { return /*~~(id:submit)~~>*/R.id.submit; }
              }
              """
          )
        );
    }

    @Test
    void kotlinResourceReference() {
        rewriteRun(
          kotlin(
            """
              class R {
                  class string { companion object { var app_name: Int = 0 } }
                  class id { companion object { var submit: Int = 0 } }
              }
              class Test {
                  fun s() = R.string.app_name
                  fun b() = R.id.submit
              }
              """,
            """
              class R {
                  class string { companion object { var app_name: Int = 0 } }
                  class id { companion object { var submit: Int = 0 } }
              }
              class Test {
                  fun s() = /*~~(string:app_name)~~>*/R.string.app_name
                  fun b() = /*~~(id:submit)~~>*/R.id.submit
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchUnrelatedFieldAccess() {
        rewriteRun(
          java(
            """
              class Foo {
                  static class Bar { static int baz = 0; }
              }
              class Test {
                  int v() { return Foo.Bar.baz; }
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchTwoSegmentRAccess() {
        // R.string is not a usable resource reference; only the three-segment chain qualifies.
        rewriteRun(
          java(
            """
              class R { static class string {} }
              class Test {
                  Class<?> v() { return R.string.class; }
              }
              """
          )
        );
    }
}
