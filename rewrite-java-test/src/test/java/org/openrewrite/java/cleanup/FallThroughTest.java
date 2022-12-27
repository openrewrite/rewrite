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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.FallThroughStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"EnhancedSwitchMigration", "ConstantConditions", "StatementWithEmptyBody", "SwitchStatementWithTooFewBranches", "ReassignedVariable", "UnusedAssignment"})
class FallThroughTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FallThrough());
    }

    @Test
    void switchExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                  int test(int n) {
                      return switch(n) {
                         case 1 -> n+1;
                         case 2 -> n+2;
                         default -> n;
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void addBreakWhenPreviousCaseHasCodeButLacksBreak() {
        rewriteRun(
          java(
            """
              public class A {
                  int i;
                  {
                      switch (i) {
                      case 0:
                          i++;
                      case 99:
                          i++;
                      }
                  }
              }
              """,
            """
              public class A {
                  int i;
                  {
                      switch (i) {
                      case 0:
                          i++;
                          break;
                      case 99:
                          i++;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotAddBreakWhenPreviousCaseDoesNotContainCode() {
        rewriteRun(
          java(
            """
                  public class A {
                      int i;
                      {
                          switch (i) {
                          case 0:
                          case 99:
                              i++;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void checkLastCaseGroupAddsBreakToLastCase() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(),
                singletonList(new FallThroughStyle(true)))))
          ),
          java(
            """
              public class A {
                  int i;
                  {
                      switch (i) {
                      case 0:
                      case 99:
                          i++;
                      }
                  }
              }
              """,
            """
                  public class A {
                      int i;
                      {
                          switch (i) {
                          case 0:
                          case 99:
                              i++;
                              break;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void acceptableStatementsAreBreakOrReturnOrThrowOrContinue() {
        rewriteRun(
          java(
            """
                  public class A {
                      int i;
                      {
                          switch (i) {
                          case 0:
                              i++;
                              break;
                          case 1:
                              i++;
                              return;
                          case 2:
                              i++;
                              throw new Exception();
                          case 3:
                              i++;
                              continue;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void reliefPatternExpectedMatchesVariations() {
        rewriteRun(
          java(
            """
                  public class A {
                      int i;
                      {
                          switch (i) {
                          case 0:
                              i++; // fall through
                          case 1:
                              i++; // falls through
                          case 2:
                              i++; // fallthrough
                          case 3:
                              i++; // fallthru
                          case 4:
                              i++; // fall-through
                          case 5:
                              i++; // fallthrough
                          case 99:
                              i++;
                              break;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void handlesSwitchesWithOneOrNoneCases() {
        rewriteRun(
          java(
            """
                  public class A {
                      public void noCase(int i) {
                          switch (i) {
                          }
                      }
                      
                      public void oneCase(int i) {
                          switch (i) {
                              case 0:
                                  i++;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void addBreaksFallthroughCasesComprehensive() {
        rewriteRun(
          java(
            """
                  public class A {
                      int i;
                      {
                          switch (i) {
                          case 0:
                              i++; // fall through

                          case 1:
                              i++;
                              // falls through
                          case 2:
                          case 3: {{
                          }}
                          case 4: {
                              i++;
                          }
                          // fallthrough
                          case 5:
                              i++;
                          /* fallthru */case 6:
                              i++;
                              // fall-through
                          case 7:
                              i++;
                              break;
                          case 8: {
                              // fallthrough
                          }
                          case 9:
                              i++;
                          }
                      }
                  }
              """,
            """
                  public class A {
                      int i;
                      {
                          switch (i) {
                          case 0:
                              i++; // fall through

                          case 1:
                              i++;
                              // falls through
                          case 2:
                          case 3: {{
                              break;
                          }}
                          case 4: {
                              i++;
                          }
                          // fallthrough
                          case 5:
                              i++;
                          /* fallthru */case 6:
                              i++;
                              // fall-through
                          case 7:
                              i++;
                              break;
                          case 8: {
                              // fallthrough
                          }
                          case 9:
                              i++;
                          }
                      }
                  }
              """
          )
        );
    }
}
