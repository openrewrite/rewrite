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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions", "ClassInitializerMayBeStatic"})
class UnwrapParenthesesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
                doAfterVisit(new UnwrapParentheses<>(parens));
                return super.visitParentheses(parens, ctx);
            }
        }));
    }

    @DocumentExample
    @Test
    void unwrapAssignment() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean a;
                  {
                      a = (true);
                  }
              }
              """,
            """
              public class A {
                  boolean a;
                  {
                      a = true;
                  }
              }
              """
          )
        );
    }

    @Test
    void unwrapIfCondition() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      if((true)) {}
                  }
              }
              """,
            """
              public class A {
                  {
                      if(true) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void unwrapOnUnaryWithWrappedPreIncrement() {
        rewriteRun(
          java(
            """
              public class A {
                  static boolean count = 0;
                  static {
                    ++(count);
                  }
              }
              """,
            """
              public class A {
                  static boolean count = 0;
                  static {
                    ++count;
                  }
              }
              """
          )
        );
    }

    @Test
    void unwrapOnUnaryWithWrappedPostIncrement() {
        rewriteRun(
          java(
            """
              public class A {
                  static boolean count = 0;
                  static {
                    (count)++;
                  }
              }
              """,
            """
              public class A {
                  static boolean count = 0;
                  static {
                    count++;
                  }
              }
              """
          )
        );
    }

    @Test
    void unwrapOnUnaryWithWrappedPreDecrement() {
        rewriteRun(
          java(
            """
              public class A {
                  static boolean count = 0;
                  static {
                    --(count);
                  }
              }
              """,
            """
              public class A {
                  static boolean count = 0;
                  static {
                    --count;
                  }
              }
              """
          )
        );
    }

    @Test
    void unwrapOnUnaryWithWrappedPostDecrement() {
        rewriteRun(
          java(
            """
              public class A {
                  static boolean count = 0;
                  static {
                    (count)--;
                  }
              }
              """,
            """
              public class A {
                  static boolean count = 0;
                  static {
                    count--;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepParenthesesOnUnaryWithWrappedBinary() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              public class A {
                  static HashSet<String> set = new HashSet<>();
                  static boolean notEmpty = !(set == null || set.isEmpty());
              }
              """
          )
        );
    }

    @Test
    void unwrapOnUnaryWithWrappedMethodCall() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              public class A {
                  static boolean notEmpty = !(new HashSet<>().isEmpty());
              }
              """,
            """
              import java.util.HashSet;
              
              public class A {
                  static boolean notEmpty = !new HashSet<>().isEmpty();
              }
              """
          )
        );
    }

    @ExpectedToFail("Parentheses are removed as intended but overall formatting looks weird.")
    @Test
    void unwrapOnUnaryWithWrappedSwitch() {
        rewriteRun(
          java(
            """
              public class A {
                  static int count = 100;
                  static boolean uncategorized = !(
                      switch (count) {
                        case 10:
                        case 20:
                            return true;
                        default:
                            return false;
                      }
                  );
              }
              """,
            """
              public class A {
                  static int count = 100;
                  static boolean uncategorized = !switch (count) {
                    case 10:
                    case 20:
                        return true;
                    default:
                        return false;
                  };
              }
              """
          )
        );
    }
}
