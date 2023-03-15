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
package org.openrewrite.java.dataflow;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.java.controlflow.Guard;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({
  "ConstantConditions",
  "EnhancedSwitchMigration",
  "FunctionName",
  "IdempotentLoopBody",
  "LoopConditionNotUpdatedInsideLoop",
  "ObviousNullCheck",
  "ReassignedVariable",
  "RedundantCast",
  "RedundantOperationOnEmptyContainer",
  "ResultOfMethodCallIgnored",
  "SillyAssignment",
  "StringOperationCanBeSimplified",
  "UnnecessaryLocalVariable",
  "UnusedAssignment",
  "MismatchedReadAndWriteOfArray"
})
class FindLocalFlowPathsStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new FindLocalFlowPaths<>(new LocalFlowSpec<>() {
            @Override
            public boolean isSource(Expression expr, Cursor cursor) {
                if (expr instanceof J.Literal) {
                    return Objects.equals(((J.Literal) expr).getValue(), "42");
                }
                if (expr instanceof J.MethodInvocation) {
                    return ((J.MethodInvocation) expr).getName().getSimpleName().equals("source");
                }
                return false;
            }

            @Override
            public boolean isSink(J j, Cursor cursor) {
                return true;
            }

            @Override
            public boolean isBarrierGuard(Guard guard, boolean branch) {
                if (guard.getExpression() instanceof J.MethodInvocation) {
                    return branch && ((J.MethodInvocation) guard.getExpression()).getName().getSimpleName().equals("guard");
                }
                return super.isBarrierGuard(guard, branch);
            }
        }))).expectedCyclesThatMakeChanges(1).cycles(1);
    }

    @Test
    void transitiveAssignmentFromLiteral() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      String n = "42";
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      String o = /*~~>*/n;
                      System.out.println(/*~~>*/o);
                      String p = /*~~>*/o;
                  }
              }
              """
          )
        );
    }

    @Test
    void transitiveAssignmentFromSourceMethod() {
        rewriteRun(
          java(
            """
              class Test {
                  String source() {
                      return null;
                  }

                  void test() {
                      String n = source();
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              class Test {
                  String source() {
                      return null;
                  }

                  void test() {
                      String n = /*~~>*/source();
                      String o = /*~~>*/n;
                      System.out.println(/*~~>*/o);
                      String p = /*~~>*/o;
                  }
              }
              """
          )
        );
    }

    @Test
    void taintFlowViaAppendIsNotDataFlow() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      String n = "42";
                      String o = n + '/';
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      String o = /*~~>*/n + '/';
                      System.out.println(o);
                      String p = o;
                  }
              }
              """
          )
        );
    }

    @Test
    void taintFlowIsNotDataFlowButItIsTrackedToCallSite() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      String n = "42";
                      String o = n.toString() + '/';
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      String o = /*~~>*//*~~>*/n.toString() + '/';
                      System.out.println(o);
                      String p = o;
                  }
              }
              """
          )
        );
    }

    @Test
    void taintFlowViaConstructorCallisNotDataFlow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  void test() {
                      String n = "42";
                      java.io.File o = new java.io.File(n);
                      System.out.println(o);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      java.io.File o = new java.io.File(/*~~>*/n);
                      System.out.println(o);
                  }
              }
              """
          )
        );
    }

    @Test
    void theSourceIsAlsoASinkSimple() {
        rewriteRun(
          java(
            """
              class Test {
                  String source() {
                      return null;
                  }
                  void sink(Object any) {
                      // do nothing
                  }
                  void test() {
                      sink(source());
                  }
              }
              """,
            """
              class Test {
                  String source() {
                      return null;
                  }
                  void sink(Object any) {
                      // do nothing
                  }
                  void test() {
                      sink(/*~~>*/source());
                  }
              }
              """
          )
        );
    }

    @Test
    void theSourceAsALiteralIsAlsoASink() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  void sink(Object any) {
                      // do nothing
                  }
                  void test() {
                      sink("42");
                  }
              }
              """,
            """
              class Test {
                  void sink(Object any) {
                      // do nothing
                  }
                  void test() {
                      sink(/*~~>*/"42");
                  }
              }
              """
          )
        );
    }

    @Test
    void theSourceIsAlsoASink() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              import java.util.Locale;
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      source();
                      source()
                          .toString();
                      source()
                          .toLowerCase(Locale.ROOT);
                      source()
                          .toString()
                          .toLowerCase(Locale.ROOT);
                  }
              }
              """,
            """
              import java.util.Locale;
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      /*~~>*/source();
                      /*~~>*//*~~>*/source()
                          .toString();
                      /*~~>*/source()
                          .toLowerCase(Locale.ROOT);
                      /*~~>*//*~~>*/source()
                          .toString()
                          .toLowerCase(Locale.ROOT);
                  }
              }
              """
          )
        );
    }

    @Test
    void theSourceIsAlsoASinkDoubleCallChain() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      source()
                          .toString()
                          .toString();
                  }
              }
              """,
            """
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      /*~~>*//*~~>*//*~~>*/source()
                          .toString()
                          .toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void theSourceCanBeTrackedThroughWrappedParentheses() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              import java.util.Locale;
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      (
                          source()
                      ).toLowerCase(Locale.ROOT);
                      (
                          (
                              source()
                          )
                      ).toLowerCase(Locale.ROOT);
                      (
                          (Object) source()
                      ).equals(null);
                  }
              }
              """,
            """
              import java.util.Locale;
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      /*~~>*/(
                          /*~~>*/source()
                      ).toLowerCase(Locale.ROOT);
                      /*~~>*/(
                          /*~~>*/(
                              /*~~>*/source()
                          )
                      ).toLowerCase(Locale.ROOT);
                      /*~~>*/(
                          /*~~>*/(Object) /*~~>*/source()
                      ).equals(null);
                  }
              }
              """
          )
        );
    }

    @Test
    void theSourceCanBeTrackedThroughWrappedParenthesesThroughCasting() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              import java.util.Locale;
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      (
                          (String)(
                              (Object) source()
                          )
                      ).toString();
                  }
              }
              """,
            """
              import java.util.Locale;
              class Test {
                  String source() {
                      return null;
                  }
                  void test() {
                      /*~~>*//*~~>*/(
                          /*~~>*/(String)/*~~>*/(
                              /*~~>*/(Object) /*~~>*/source()
                          )
                      ).toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void sourceIsTrackedWhenAssignedInWhileLoopControlParentheses() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  String source() {
                      return null;
                  }
                  
                  void test() {
                      String a;
                      a = a;
                      while ((a = source()) != null) {
                          System.out.println(a);
                      }
                  }
              }
              """,
            """
              class Test {
                  String source() {
                      return null;
                  }
                  
                  void test() {
                      String a;
                      a = a;
                      while ((a = /*~~>*/source()) != null) {
                          System.out.println(/*~~>*/a);
                      }
                  }
              }
                  """
          )
        );
    }

    @Test
    void sourceIsTrackedWhenAssignedInDoWhileLoopControlParentheses() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  String source() {
                      return null;
                  }
                  
                  void test() {
                      String a = null;
                      a = a;
                      do {
                          System.out.println(a);
                      } while ((a = source()) != null);
                  }
              }
              """,
            """
              class Test {
                  String source() {
                      return null;
                  }
                  
                  void test() {
                      String a = null;
                      a = a;
                      do {
                          System.out.println(/*~~>*/a);
                      } while ((a = /*~~>*/source()) != null);
                  }
              }
                  """
          )
        );
    }

    @Test
    void sourceIsTrackedWhenAssignedInForLoop() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  String source(int i) {
                      return null;
                  }
                  
                  void test() {
                      String a = null;
                      a = a;
                      for (int i = 0; i < 10 && (a = source(i)) != null; i++) {
                          System.out.println(a);
                      }
                  }
              }
              """,
            """
              class Test {
                  String source(int i) {
                      return null;
                  }
                  
                  void test() {
                      String a = null;
                      a = a;
                      for (int i = 0; i < 10 && (a = /*~~>*/source(i)) != null; i++) {
                          System.out.println(/*~~>*/a);
                      }
                  }
              }
                  """
          )
        );
    }

    @Test
    void assignmentOfValueInsideIfBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  String source() {
                      return null;
                  }
                  void test(boolean condition) {
                      String a = null;
                      if (condition) {
                          a = source();
                          System.out.println(a);
                      }
                  }
              }
              """,
            """
              class Test {
                  String source() {
                      return null;
                  }
                  void test(boolean condition) {
                      String a = null;
                      if (condition) {
                          a = /*~~>*/source();
                          System.out.println(/*~~>*/a);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void reassignmentOfAVariableBreaksFlow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  void test() {
                      String n = "42";
                      System.out.println(n);
                      n = "100";
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                      n = "100";
                      System.out.println(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void reassignmentOfAVariableWithExistingValuePreservesFlow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  void test() {
                      String n = "42";
                      System.out.println(n);
                      n = n;
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                      n = /*~~>*/n;
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void reassignmentOfAVariableWithExistingValueWrappedInParenthesesPreservesFlow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  void test() {
                      String n = "42";
                      System.out.println(n);
                      (n) = n;
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                      (n) = /*~~>*/n;
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void aClassNameInAConstructorCallIsNotConsideredAsAPartOfDataflow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  class n {}

                  void test() {
                      String n = "42";
                      System.out.println(n);
                      n = new n().toString();
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {
                  class n {}

                  void test() {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                      n = new n().toString();
                      System.out.println(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void aClassNameInAConstructorCallOnParentTypeIsNotConsideredAsAPartOfDataflow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  class n {}

                  void test() {
                      String n = "42";
                      System.out.println(n);
                      n = new Test.n().toString();
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {
                  class n {}

                  void test() {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                      n = new Test.n().toString();
                      System.out.println(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void aMethodNameIsNotConsideredAsAPartOfDataflow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  String n() {
                      return null;
                  }

                  void test() {
                      String n = "42";
                      System.out.println(n);
                      n = n();
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {
                  String n() {
                      return null;
                  }

                  void test() {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                      n = n();
                      System.out.println(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void aClassVariableAccessIsNotConsideredAsAPartOfDataflow() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {
                  String n = "100";

                  void test() {
                      String n = "42";
                      System.out.println(n);
                      System.out.println(this.n);
                  }
              }
              """,
            """
              class Test {
                  String n = "100";

                  void test() {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                      System.out.println(this.n);
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorIsConsideredDataFlowStep() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {

                  void test(boolean conditional) {
                      String n = conditional ? "42" : "100";
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {

                  void test(boolean conditional) {
                      String n = /*~~>*/conditional ? /*~~>*/"42" : "100";
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorIsConsideredDataFlowStep2() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1),
          java(
            """
              class Test {

                  void test(boolean conditional) {
                      String n = "42";
                      String m = conditional ? "100" : n;
                      System.out.println(m);
                  }
              }
              """,
            """
              class Test {

                  void test(boolean conditional) {
                      String n = /*~~>*/"42";
                      String m = /*~~>*/conditional ? "100" : /*~~>*/n;
                      System.out.println(/*~~>*/m);
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryConditionIsNotConsideredDataFlowStep() {
        rewriteRun(
          java(
            """
              class Test {

                  Boolean source() {
                      return null;
                  }

                  void test(String other) {
                      String n = source() ? "102" : "100";
                      System.out.println(n);
                  }
              }
              """,
            """
              class Test {

                  Boolean source() {
                      return null;
                  }

                  void test(String other) {
                      String n = /*~~>*/source() ? "102" : "100";
                      System.out.println(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void objectsRequireNotNullIsValidFlowStep() {
        rewriteRun(
          java(
            """
              import java.util.Objects;
              @SuppressWarnings({"ObviousNullCheck", "RedundantSuppression"})
              class Test {
                  void test() {
                      String n = Objects.requireNonNull("42");
                      String o = n;
                      System.out.println(Objects.requireNonNull(o));
                      String p = o;
                  }
              }
              """,
            """
              import java.util.Objects;
              @SuppressWarnings({"ObviousNullCheck", "RedundantSuppression"})
              class Test {
                  void test() {
                      String n = /*~~>*/Objects.requireNonNull(/*~~>*/"42");
                      String o = /*~~>*/n;
                      System.out.println(/*~~>*/Objects.requireNonNull(/*~~>*/o));
                      String p = /*~~>*/o;
                  }
              }
              """
          )
        );
    }

    @Test
    void transitiveAssignmentFromLiteralWithWithAGuard() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      if (guard()) {
                          String o = n;
                          System.out.println(o);
                          String p = o;
                      } else {
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      if (guard()) {
                          String o = n;
                          System.out.println(o);
                          String p = o;
                      } else {
                          System.out.println(/*~~>*/n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void transitiveAssignmentFromLiteralWithWithAOrOperatorguard() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      if (guard() || guard()) {
                          String o = n;
                          System.out.println(o);
                          String p = o;
                      } else {
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      if (guard() || guard()) {
                          String o = n;
                          System.out.println(o);
                          String p = o;
                      } else {
                          System.out.println(/*~~>*/n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void transitiveAssignmentFromLiteralWithWithANegatedGuard() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      if (!guard()) {
                          String o = n;
                          System.out.println(o);
                          String p = o;
                      } else {
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      if (!guard()) {
                          String o = /*~~>*/n;
                          System.out.println(/*~~>*/o);
                          String p = /*~~>*/o;
                      } else {
                          System.out.println(n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void thrownExceptionIsAGuard() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      if (!guard()) {
                          throw new RuntimeException();
                      }
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      if (!guard()) {
                          throw new RuntimeException();
                      }
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """
          )
        );
    }

    @Test
    void thrownExceptionIsAGuardWhenIncludedInAnBooleanExpression() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      if (!guard() && !guard()) {
                          throw new RuntimeException();
                      }
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      if (!guard() && !guard()) {
                          throw new RuntimeException();
                      }
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """
          )
        );
    }

    @Test
    void thrownExceptionIsAGuardWhenIncludedInAnBooleanExpressionDeMorgans() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      if (!(guard() || guard())) {
                          throw new RuntimeException();
                      }
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      if (!(guard() || guard())) {
                          throw new RuntimeException();
                      }
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """
          )
        );
    }

    @Test
    void tryCatchBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      try {
                          System.out.println(n);
                      } catch (Exception e) {
                          System.out.println(n);
                      }
                      String o = n;
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      try {
                          System.out.println(/*~~>*/n);
                      } catch (Exception e) {
                          System.out.println(n);
                      }
                      String o = /*~~>*/n;
                      System.out.println(/*~~>*/o);
                      String p = /*~~>*/o;
                  }
              }
              """
          )
        );
    }

    @Test
    void statementWithBooleanArrayIndexConditional() {
        rewriteRun(
          java(
            """
              abstract class Test {

                  void test() {
                      String n = "42";
                      Boolean[] b = new Boolean[1];
                      if (b[0] && (b.length == 1)) {
                          String o = n;
                          System.out.println(o);
                          String p = o;
                      } else {
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              abstract class Test {

                  void test() {
                      String n = /*~~>*/"42";
                      Boolean[] b = new Boolean[1];
                      if (b[0] && (b.length == 1)) {
                          String o = /*~~>*/n;
                          System.out.println(/*~~>*/o);
                          String p = /*~~>*/o;
                      } else {
                          System.out.println(/*~~>*/n);
                      }
                  }
              }
              """
          )
        );
    }

    // currently giving "java.lang.AssertionError: The recipe must make changes"
    @Test
    void switchWithMultipleCases() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      switch (n) {
                          case "1":
                              System.out.println(n);
                              break;
                          case "42":
                              System.out.println("Correct");
                              break;
                          default:
                              break;
                      }
                      String o = n + "";
                      System.out.println(o);
                      String p = o;
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      switch (/*~~>*/n) {
                          case "1":
                              System.out.println(/*~~>*/n);
                              break;
                          case "42":
                              System.out.println("Correct");
                              break;
                          default:
                              break;
                      }
                      String o = /*~~>*/n + "";
                      System.out.println(o);
                      String p = o;
                  }
              }
              """
          )
        );
    }

    @Test
    void forEachLoop() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      for (char c : n.toCharArray()) {
                          System.out.println(c);
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      for (char c : /*~~>*/n.toCharArray()) {
                          System.out.println(c);
                          System.out.println(/*~~>*/n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void genericObjectInstantiation() {
        rewriteRun(
          java(
            """
              import java.util.LinkedList;
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      LinkedList<Integer> ll = new LinkedList<>();
                      ll.add(1);
                      for (int i : ll) {
                          System.out.println(i);
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              import java.util.LinkedList;
              abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      LinkedList<Integer> ll = new LinkedList<>();
                      ll.add(1);
                      for (int i : ll) {
                          System.out.println(i);
                          System.out.println(/*~~>*/n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void assertExpression() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean guard();
                  void test() {
                      String n = "42";
                      assert n.contains("4");
                  }
              }
              """,
            """
              abstract class Test {
                  abstract boolean guard();
                  void test() {
                      String n = /*~~>*/"42";
                      assert /*~~>*/n.contains("4");
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("CodeBlock2Expr")
    @Test
    void lambdaExpression() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList; abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = "42";
                      ArrayList<Integer> numbers = new ArrayList<>();
                      numbers.forEach( (i) -> { System.out.println(n); } );
                  }
              }
              """,
            """
              import java.util.ArrayList; abstract class Test {
                  abstract boolean guard();

                  void test() {
                      String n = /*~~>*/"42";
                      ArrayList<Integer> numbers = new ArrayList<>();
                      numbers.forEach( (i) -> { System.out.println(/*~~>*/n); } );
                  }
              }
              """
          )
        );
    }

    @Test
    void trueLiteralGuard() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test() {
                      String n = "42";
                      if (true) {
                          System.out.println(n);
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      if (true) {
                          System.out.println(/*~~>*/n);
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void falseLiteralGuard() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test() {
                      String n = "42";
                      if (false) {
                          System.out.println(n);
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test() {
                      String n = /*~~>*/"42";
                      if (false) {
                          System.out.println(n);
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInAHigherBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test() {
                      String n;
                      {
                          n = "42";
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test() {
                      String n;
                      {
                          n = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInADoublyHigherBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test() {
                      String n;
                      {
                          {
                              n = "42";
                          }
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test() {
                      String n;
                      {
                          {
                              n = /*~~>*/"42";
                          }
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInATriplyHigherBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test() {
                      String n;
                      {
                          {
                              {
                                  n = "42";
                              }
                          }
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test() {
                      String n;
                      {
                          {
                              {
                                  n = /*~~>*/"42";
                              }
                          }
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInAnIfBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      if (condition) {
                          n = "42";
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      if (condition) {
                          n = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInAWhileBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      while (condition) {
                          n = "42";
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      while (condition) {
                          n = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInADoWhileBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      do {
                          n = "42";
                      } while (condition);
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      do {
                          n = /*~~>*/"42";
                      } while (condition);
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInAForIBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      for(int i = 0; i < 42; i++) {
                          n = "42";
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      for(int i = 0; i < 42; i++) {
                          n = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInAForEachBlock() {
        rewriteRun(
          java(
            """
              import java.util.List;
              abstract class Test {
                  void test(boolean condition, List<Integer> integerList) {
                      String n;
                      for(Integer i : integerList) {
                          n = "42";
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              import java.util.List;
              abstract class Test {
                  void test(boolean condition, List<Integer> integerList) {
                      String n;
                      for(Integer i : integerList) {
                          n = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInATryCatchBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      try {
                          n = "42";
                      } catch (Exception e) {
                          // No-op
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      try {
                          n = /*~~>*/"42";
                      } catch (Exception e) {
                          // No-op
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInATryCatchFinallyBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      String o;
                      try {
                          n = "42";
                      } catch (Exception e) {
                          // No-op
                      } finally {
                          o = "42";
                      }
                      System.out.println(n);
                      System.out.println(o);
                  }
              }
              """,
            """
              abstract class Test {
                  void test(boolean condition) {
                      String n;
                      String o;
                      try {
                          n = /*~~>*/"42";
                      } catch (Exception e) {
                          // No-op
                      } finally {
                          o = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                      System.out.println(/*~~>*/o);
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowShouldNotCrossScopeBoundaries() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void test() {
                      {
                          String n;
                          {
                              n = "42";
                          }
                          System.out.println(n);
                      }
                      {
                          String n = "hello";
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  void test() {
                      {
                          String n;
                          {
                              n = /*~~>*/"42";
                          }
                          System.out.println(/*~~>*/n);
                      }
                      {
                          String n = "hello";
                          System.out.println(n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowShouldNotCrossScopeBoundariesWithClassScopeVariableConflicts() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  String n;
                  void test() {
                      {
                          String n;
                          {
                              n = "42";
                          }
                          System.out.println(n);
                      }
                      {
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  String n;
                  void test() {
                      {
                          String n;
                          {
                              n = /*~~>*/"42";
                          }
                          System.out.println(/*~~>*/n);
                      }
                      {
                          System.out.println(n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void dataFlowForASourceInAHigherBlockInStaticBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  static {
                      String n;
                      {
                          n = "42";
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  static {
                      String n;
                      {
                          n = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ClassInitializerMayBeStatic")
    @Test
    void dataFlowForASourceInAnInit() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  {
                      String n = "42";
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  {
                      String n = /*~~>*/"42";
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ClassInitializerMayBeStatic")
    @Test
    void dataFlowForASourceInAHigherBlockInInitBlock() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  {
                      String n;
                      {
                          n = "42";
                      }
                      System.out.println(n);
                  }
              }
              """,
            """
              abstract class Test {
                  {
                      String n;
                      {
                          n = /*~~>*/"42";
                      }
                      System.out.println(/*~~>*/n);
                  }
              }
              """
          )
        );
    }
}
