/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class FindEmptyClassesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindEmptyClasses());
    }

    @Test
    void classNotEmpty() {
        rewriteRun(
          java(
            """
              class IsNotEmpty {
                  int x = 0;
              }
              """
          )
        );
    }

    @Test
    void emptyInterface() {
        rewriteRun(
          java(
            """
              interface IsEmpty {
              }
              """
          )
        );
    }

    @Test
    void emptyEnum() {
        rewriteRun(
          java(
            """
              enum IsEmpty {
              }
              """
          )
        );
    }

    @Test
    void emptyClassWithAnnotation() {
        rewriteRun(
          java(
            """
              @Deprecated
              class IsEmpty {
              }
              """
          )
        );
    }

    @Test
    void emptyClassWithExtends() {
        rewriteRun(
          java("class A {}", SourceSpec::skip),
          java(
            """
              class IsEmpty extends A {
              }
              """
          )
        );
    }

    @Test
    void emptyClassWithImplements() {
        rewriteRun(
          java("interface A {}"),
          java(
            """
              class IsEmpty implements A {
              }
              """
          )
        );
    }

    @Test
    void findEmptyClass() {
        rewriteRun(
          java(
            """
              class IsEmpty {
              }
              """,
            """
              /*~~>*/class IsEmpty {
              }
              """
          )
        );
    }
}
