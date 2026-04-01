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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class DeclaresTypeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new DeclaresType<>("com.sample.Foo")));
    }

    @DocumentExample
    @Test
    void declares() {
        rewriteRun(
          java(
            """
              package com.sample;
              public class Foo{}
              """,
            """
              package com.sample;
              /*~~>*/public class Foo{}
              """
          )
        );
    }

    @Test
    void detectSubtypeWhenEnabled() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new DeclaresType<>("com.sample.Foo", true))),
          java(
            """
              package com.sample;
              public interface Foo{}
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.sample.Foo;
              class A implements Foo{}
              """,
            """
              import com.sample.Foo;
              /*~~>*/class A implements Foo{}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/pull/961")
    @Test
    void detectSubtypeWhenEnabledOnAnonymousClass() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new DeclaresType<>("com.sample.Foo", true))),
          java(
            """
              package com.sample;
              public class Foo{}
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.sample.Foo;
              class A {
                  Foo foo = new Foo(){};
              }
              """,
            """
              import com.sample.Foo;
              class A {
                  Foo foo = /*~~>*/new Foo(){};
              }
              """
          )
        );
    }

    @Test
    void subtypeNotEnabledByDefault() {
        rewriteRun(
          java(
            """
              package com.sample;
              public interface Foo{}
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.sample.Foo;
              class A implements Foo{}
              """
          )
        );
    }

    @Test
    void notDeclares() {
        rewriteRun(
          java(
            """
              package com.sample;
              public class Fooz{}
              """
          )
        );
    }
}
