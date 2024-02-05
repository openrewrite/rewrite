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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class QualifyThisVisitorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(QualifyThisVisitor::new));
    }

    @Test
    void qualifyThis() {
        rewriteRun(
          java(
            """
              public class Foo {
                  private String str = "str";
                  public String getStr() {
                      return this.str;
                  }
              }
              """,
            """
              public class Foo {
                  private String str = "str";
                  public String getStr() {
                      return Foo.this.str;
                  }
              }
              """
          )
        );
    }

    @Test
    void alreadyQualifiedNoop() {
        rewriteRun(
          java(
            """
              public class Foo {
                  private String str = "str";
                  public String getStr() {
                      return Foo.this.str;
                  }
              }
              """
          )
        );
    }

    @Test
    void qualifyThisMethodInvocation() {
        rewriteRun(
          java(
            """
              public class Foo {
                  private String str = "str";
                  public int getLength() {
                      return this.str.length();
                  }
              }
              """,
            """
              public class Foo {
                  private String str = "str";
                  public int getLength() {
                      return Foo.this.str.length();
                  }
              }
              """
          )
        );
    }
}
