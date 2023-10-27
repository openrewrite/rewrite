/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ClassInitializerMayBeStatic")
class FindMissingTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindMissingTypes())
          .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void missingAnnotationType() {
        rewriteRun(
          java(
            """
              import org.junit.Test;
                            
              class ATest {
                  @Test
                  void foo() {}
              }
              """,
            """
              import org.junit.Test;

              class ATest {
                  @/*~~(Identifier type is missing or malformed)~~>*/Test
                  void foo() {}
              }
              """
          )
        );
    }

    @Test
    void variableDeclaration() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      Foo f;
                  }
              }
              """,
            """
              class A {
                  {
                      /*~~(Identifier type is missing or malformed)~~>*/Foo f;
                  }
              }
              """
          )
        );
    }

    @Test
    void classReference() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      Class<?> c = Unknown.class;
                  }
              }
              """,
            """
              class A {
                  {
                      Class<?> c = /*~~(Identifier type is missing or malformed)~~>*/Unknown.class;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodReference() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              
              class A {
                  Consumer<String> r = System.out::printlns;
              }
              """,
            """
              import java.util.function.Consumer;
              
              class A {
                  Consumer<String> r = /*~~(MemberReference type is missing or malformed)~~>*/System.out::printlns;
              }
              """
          )
        );
    }

    @Test
    void newClass() {
        rewriteRun(
          java(
            """
              import some.org.Unknown;
                            
              class A {
                  {
                      Object o = new Unknown();
                  }
              }
              """,
            """
              import some.org.Unknown;
                            
              class A {
                  {
                      Object o = /*~~(NewClass type is missing or malformed)~~>*/new Unknown();
                  }
              }
              """
          )
        );
    }
}
