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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class DoesNotUseRewriteSkipTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeType("java.util.List", "java.util.Collection", false)
            .addSingleSourceApplicableTest(new DoesNotUseRewriteSkip().getVisitor()))
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void skipAll() {
        rewriteRun(
          java(
            """
              @RewriteSkip
              package org.openrewrite;
                            
              import org.openrewrite.java.RewriteSkip;
                            
              class Test {
                  java.util.List<String> o;
              }
              """
          )
        );
    }

    @Test
    void skipByClass() {
        rewriteRun(
          java(
            """
              @RewriteSkip(recipeClasses = ChangeType.class)
              package org.openrewrite;
                            
              import org.openrewrite.java.RewriteSkip;
              import org.openrewrite.java.ChangeType;
                            
              class Test {
                  java.util.List<String> o;
              }
              """
          )
        );
    }

    @Test
    void skipByName() {

        rewriteRun(
          java(
            """
              @RewriteSkip(recipes = "org.openrewrite.java.ChangeType")
              package org.openrewrite;
                            
              import org.openrewrite.java.RewriteSkip;
              import org.openrewrite.java.ChangeType;
                            
              class Test {
                  java.util.List<String> o;
              }
              """
          )
        );
    }

    @Test
    void skipByClassDoesNotMatch() {
        rewriteRun(
          java(
            """
              @RewriteSkip(recipeClasses = ChangeMethodName.class)
              package org.openrewrite;
                            
              import org.openrewrite.java.RewriteSkip;
              import org.openrewrite.java.ChangeMethodName;
                            
              import java.util.List;
                            
              class Test {
                  List<String> o;
              }
              """,
            """
              @RewriteSkip(recipeClasses = ChangeMethodName.class)
              package org.openrewrite;
                            
              import org.openrewrite.java.RewriteSkip;
              import org.openrewrite.java.ChangeMethodName;
                            
              import java.util.Collection;
                            
              class Test {
                  Collection<String> o;
              }
              """
          )
        );
    }
}
