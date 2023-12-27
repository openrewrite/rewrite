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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NormalizeFormatTest implements RewriteTest {

    Recipe removeAnnotation = toRecipe(() -> new JavaIsoVisitor<>() {
        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            //noinspection ConstantConditions
            return null;
        }
    });

    @DocumentExample
    @Test
    void removeAnnotationFromMethod() {
        rewriteRun(
          spec -> spec.recipes(
            removeAnnotation,
            new NormalizeFormat(),
            new RemoveTrailingWhitespace(),
            new TabsAndIndents()
          ),
          java(
            """
              class Test {
                  @Deprecated
                  public void method(Test t) {
                  }
              }
              """,
            """
              class Test {
                            
                  public void method(Test t) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeAnnotationFromClass() {
        rewriteRun(
          spec -> spec.recipes(
            removeAnnotation,
            new NormalizeFormat(),
            new RemoveTrailingWhitespace(),
            new TabsAndIndents()
          ),
          java(
            """
              class Test {
                  @Deprecated
                  class A {
                  }
              }
              """,
            """
              class Test {
                            
                  class A {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeAnnotationFromVariable() {
        rewriteRun(
          spec -> spec.recipes(
            removeAnnotation,
            new NormalizeFormat(),
            new RemoveTrailingWhitespace(),
            new TabsAndIndents()
          ),
          java(
            """
              class Test {
                  @Deprecated
                  public String s;
              }
              """,
            """
              class Test {
                            
                  public String s;
              }
              """
          )
        );
    }
}
