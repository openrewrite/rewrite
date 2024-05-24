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
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.EmptyForInitializerPadStyle;
import org.openrewrite.java.style.EmptyForIteratorPadStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({
  "StatementWithEmptyBody", "EmptyTryBlock", "CatchMayIgnoreException", "ConstantConditions",
  "InfiniteLoopStatement", "EmptyFinallyBlock", "UnusedAssignment",
  "EmptySynchronizedStatement", "Convert2Diamond", "ClassInitializerMayBeStatic",
  "InfiniteRecursion", "PointlessBooleanExpression", "PointlessArithmeticExpression", "UnaryPlus",
  "LoopConditionNotUpdatedInsideLoop"
})
class SpacesTest implements RewriteTest {

    private static Consumer<RecipeSpec> spaces() {
        return spaces(style -> style);
    }

    private static Consumer<RecipeSpec> spaces(UnaryOperator<SpacesStyle> with) {
        return spec -> spec.recipe(new Spaces())
          .parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.spaces()))
            )
          )));
    }

    @DocumentExample
    @Test
    void beforeParensMethodDeclarationTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withMethodDeclaration(true))),
          java(
            """
              class Test {
                  void method1() {
                  }
                  void method2()    {
                  }
                  void method3()	{
                  }
              }
              """,
            """
              class Test {
                  void method1 () {
                  }
                  void method2 () {
                  }
                  void method3 () {
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensMethodDeclarationTrueWithComment() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withMethodDeclaration(true))),
          java(
            """
              class Test {
                  void method1    /*comment*/() {
                  }
              }
              """,
            """
              class Test {
                  void method1    /*comment*/ () {
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeClassBody() {
        rewriteRun(
          spaces(),
          java(
            """
              class Test{
              }
              """,
            """
              class Test {
              }
              """
          )
        );
    }

    @Test
    void beforeParensMethodDeclarationFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withMethodDeclaration(false))),
          java(
            """
              class Test {
                  void method1 () {
                  }
                  void method2    () {
                  }
                  void method3  	() {
                  }
              }
              """,
            """
              class Test {
                  void method1() {
                  }
                  void method2() {
                  }
                  void method3() {
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensMethodDeclarationFalseWithComment() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withMethodDeclaration(false))),
          java(
            """
              class Test {
                  void method1    /*comment*/    () {
                  }
              }
              """,
            """
              class Test {
                  void method1    /*comment*/() {
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensMethodDeclarationFalseWithLineBreakIgnored() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withMethodDeclaration(false))),
          java(
            """
              class Test {
                  void method1 
                  () {
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensMethodCallTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withMethodCall(true))),
          java(
            """
              class Test {
                  void foo() {
                      foo()  ;
                      Test test = new Test();
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      foo ();
                      Test test = new Test ();
                  }
              }
              """
          )
        );
    }

    @Test
    void noSpaceBeforeSemicolon() {
        rewriteRun(
          spaces(style -> style),
          java(
            """
              class Test {
                  boolean b = true    ;
                  void foo() {
                      foo()  ;
                      Test test = new Test()
                      ;
                      if (b)
                          System.out.println("OK")  ;
                  }
              }
              """,
            """
              class Test {
                  boolean b = true;
                  void foo() {
                      foo();
                      Test test = new Test()
                      ;
                      if (b)
                          System.out.println("OK");
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensMethodCallFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withMethodCall(false))),
          java(
            """
              class Test {
                  void foo() {
                      foo ();
                      Test test = new Test ();
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      foo();
                      Test test = new Test();
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensIfParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withIfParentheses(false))),
          java(
            """
              class Test {
                  void foo() {
                      if (true) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      if(true) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensIfParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withIfParentheses(true))),
          java(
            """
              class Test {
                  void foo() {
                      if(true) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      if (true) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensForParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withForParentheses(false))),
          java(
            """
              class Test {
                  void foo() {
                      for (; ; ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for(; ; ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensForParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withForParentheses(true))),
          java(
            """
              class Test {
                  void foo() {
                      for(; ; ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (; ; ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensWhileParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withWhileParentheses(false))),
          java(
            """
              class Test {
                  void foo() {
                      while (true) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      while(true) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensWhileParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withWhileParentheses(true))),
          java(
            """
              class Test {
                  void foo() {
                      while(true) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      while (true) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensSwitchParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withSwitchParentheses(false))),
          java(
            """
              class Test {
                  void foo() {
                      switch (0) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      switch(0) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensSwitchParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withSwitchParentheses(true))),
          java(
            """
              class Test {
                  void foo() {
                      switch(0) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      switch (0) {
                      }
                  }
              }
              """
          )
        );
    }

    //language=java
    SourceSpecs tryResource = java(
      """
        class MyResource implements AutoCloseable {
            public void close() {
            }
        }
        """
    );

    @Test
    void beforeParensTryParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withTryParentheses(false))),
          tryResource,
          java(
            """
              class Test {
                  void foo() {
                      try (MyResource res = new MyResource()) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      try(MyResource res = new MyResource()) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensTryParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withTryParentheses(true))),
          tryResource,
          java(
            """
              class Test {
                  void foo() {
                      try(MyResource res = new MyResource()) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      try (MyResource res = new MyResource()) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensCatchParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withCatchParentheses(false))),
          java(
            """
              class Test {
                  void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      try {
                      } catch(Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensCatchParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withCatchParentheses(true))),
          java(
            """
              class Test {
                  void foo() {
                      try {
                      } catch(Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensSynchronizedParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withSynchronizedParentheses(false))),
          java(
            """
              class Test {
                  void foo() {
                      synchronized (new Object()) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      synchronized(new Object()) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensSynchronizedParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withSynchronizedParentheses(true))),
          java(
            """
              class Test {
                  void foo() {
                      synchronized(new Object()) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      synchronized (new Object()) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeParensAnnotationParametersTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withAnnotationParameters(true))),
          java(
            """
              @SuppressWarnings({"ALL"})
              class Test {
              }
              """,
            """
              @SuppressWarnings ({"ALL"})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void beforeParensAnnotationParametersFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withAnnotationParameters(false))),
          java(
            """
              @SuppressWarnings ({"ALL"})
              class Test {
              }
              """,
            """
              @SuppressWarnings({"ALL"})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsAssignmentFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withAssignment(false))),
          java(
            """
              class Test {
                  void foo() {
                      int x = 0;
                      x += 1;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int x=0;
                      x+=1;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsAssignmentTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withAssignment(true))),
          java(
            """
              class Test {
                  void foo() {
                      int x=0;
                      x+=1;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int x = 0;
                      x += 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsLogicalFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withLogical(false))),
          java(
            """
              class Test {
                  void foo() {
                      boolean x = true && false;
                      boolean y = true || false;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      boolean x = true&&false;
                      boolean y = true||false;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsLogicalTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withLogical(true))),
          java(
            """
              class Test {
                  void foo() {
                      boolean x = true&&false;
                      boolean y = true||false;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      boolean x = true && false;
                      boolean y = true || false;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsEqualityFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withEquality(false))),
          java(
            """
              class Test {
                  void foo() {
                      boolean x = 0 == 1;
                      boolean y = 0 != 1;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      boolean x = 0==1;
                      boolean y = 0!=1;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsEqualityTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withEquality(true))),
          java(
            """
              class Test {
                  void foo() {
                      boolean x = 0==1;
                      boolean y = 0!=1;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      boolean x = 0 == 1;
                      boolean y = 0 != 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsRelationalFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withRelational(false))),
          java(
            """
              class Test {
                  void foo() {
                      boolean a = 0 < 1;
                      boolean b = 0 <= 1;
                      boolean c = 0 >= 1;
                      boolean d = 0 >= 1;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      boolean a = 0<1;
                      boolean b = 0<=1;
                      boolean c = 0>=1;
                      boolean d = 0>=1;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsRelationalTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withRelational(true))),
          java(
            """
              class Test {
                  void foo() {
                      boolean a = 0<1;
                      boolean b = 0<=1;
                      boolean c = 0>=1;
                      boolean d = 0>=1;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      boolean a = 0 < 1;
                      boolean b = 0 <= 1;
                      boolean c = 0 >= 1;
                      boolean d = 0 >= 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsBitwiseFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withBitwise(false))),
          java(
            """
              class Test {
                  void foo() {
                      int a = 1 & 2;
                      int b = 1 | 2;
                      int c = 1 ^ 2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int a = 1&2;
                      int b = 1|2;
                      int c = 1^2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsBitwiseTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withBitwise(true))),
          java(
            """
              class Test {
                  void foo() {
                      int a = 1&2;
                      int b = 1|2;
                      int c = 1^2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int a = 1 & 2;
                      int b = 1 | 2;
                      int c = 1 ^ 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsAdditiveFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withAdditive(false))),
          java(
            """
              class Test {
                  void foo() {
                      int x = 1 + 2;
                      int y = 1 - 2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int x = 1+2;
                      int y = 1-2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsAdditiveTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withAdditive(true))),
          java(
            """
              class Test {
                  void foo() {
                      int x = 1+2;
                      int y = 1-2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int x = 1 + 2;
                      int y = 1 - 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsMultiplicativeFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withMultiplicative(false))),
          java(
            """
              class Test {
                  void foo() {
                      int a = 1 * 2;
                      int b = 1 / 2;
                      int c = 1 % 2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int a = 1*2;
                      int b = 1/2;
                      int c = 1%2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsMultiplicativeTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withMultiplicative(true))),
          java(
            """
              class Test {
                  void foo() {
                      int a = 1*2;
                      int b = 1/2;
                      int c = 1%2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int a = 1 * 2;
                      int b = 1 / 2;
                      int c = 1 % 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsShiftFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withShift(false))),
          java(
            """
              class Test {
                  void foo() {
                      int a = 1 >> 2;
                      int b = 1 << 2;
                      int c = 1 >>> 2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int a = 1>>2;
                      int b = 1<<2;
                      int c = 1>>>2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsShiftTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withShift(true))),
          java(
            """
              class Test {
                  void foo() {
                      int a = 1>>2;
                      int b = 1<<2;
                      int c = 1>>>2;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int a = 1 >> 2;
                      int b = 1 << 2;
                      int c = 1 >>> 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsUnaryTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withUnary(true))),
          java(
            """
              class Test {
                  void foo() {
                      int x = 0;
                      x++;
                      x--;
                      --x;
                      ++x;
                      x = -x;
                      x = +x;
                      boolean y = false;
                      y = !y;
                      x = ~x;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int x = 0;
                      x ++;
                      x --;
                      -- x;
                      ++ x;
                      x = - x;
                      x = + x;
                      boolean y = false;
                      y = ! y;
                      x = ~ x;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsUnaryFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withUnary(false))),
          java(
            """
              class Test {
                  void foo() {
                      int x = 0;
                      x ++;
                      x --;
                      -- x;
                      ++ x;
                      x = - x;
                      x = + x;
                      boolean y = false;
                      y = ! y;
                      x = ~ x;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int x = 0;
                      x++;
                      x--;
                      --x;
                      ++x;
                      x = -x;
                      x = +x;
                      boolean y = false;
                      y = !y;
                      x = ~x;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsLambdaArrowFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withLambdaArrow(false))),
          java(
            """
              class Test {
                  void foo() {
                      Runnable r = () -> {};
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      Runnable r = ()->{};
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsLambdaArrowTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withLambdaArrow(true))),
          java(
            """
              class Test {
                  void foo() {
                      Runnable r = ()->{};
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      Runnable r = () -> {};
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsMethodReferenceDoubleColonTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withMethodReferenceDoubleColon(true))),
          java(
            """
              class Test {
                  void foo() {
                      Runnable r1 = this::foo;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      Runnable r1 = this :: foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void aroundOperatorsMethodReferenceDoubleColonFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withMethodReferenceDoubleColon(false))),
          java(
            """
              class Test {
                  void foo() {
                      Runnable r1 = this :: foo;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      Runnable r1 = this::foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceClassLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withClassLeftBrace(false))),
          java(
            """
              class Test {
              }
              """,
            """
              class Test{
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceClassLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withClassLeftBrace(true))),
          java(
            """
              class Test{
              }
              """,
            """
              class Test {
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceMethodLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withMethodLeftBrace(false))),
          java(
            """
              class Test{
                  public void foo() {
                  }
              }
              """,
            """
              class Test {
                  public void foo(){
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceMethodLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withMethodLeftBrace(true))),
          java(
            """
              class Test{
                  public void foo(){
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceIfLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withIfLeftBrace(false))),
          java(
            """
              class Test{
                  public void foo() {
                      if (true) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if (true){
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceIfLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withIfLeftBrace(true))),
          java(
            """
              class Test{
                  public void foo() {
                      if (true){
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if (true) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceElseLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withElseLeftBrace(false))),
          java(
            """
              class Test{
                  public void foo() {
                      if (true) {
                      } else {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if (true) {
                      } else{
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceElseLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withElseLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      if (true) {
                      } else{
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if (true) {
                      } else {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceForLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withForLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                      for (int i : new int[]{1, 2, 3}) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      for (int i = 0; i < 10; i++){
                      }
                      for (int i : new int[]{1, 2, 3}){
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceForLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withForLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      for (int i = 0; i < 10; i++){
                      }
                      for (int i : new int[]{1, 2, 3}){
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                      for (int i : new int[]{1, 2, 3}) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceWhileLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withWhileLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      while (true != false) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      while (true != false){
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceWhileLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withWhileLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      while (true != false){
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      while (true != false) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceDoLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withDoLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      do {
                      } while (true != false);
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      do{
                      } while (true != false);
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceDoLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withDoLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      do{
                      } while (true != false);
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      do {
                      } while (true != false);
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceSwitchLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withSwitchLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      switch (1) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      switch (1){
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceSwitchLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withSwitchLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      switch (1){
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      switch (1) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceTryLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withTryLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try{
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceTryLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withTryLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      try{
                      } catch (Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceCatchLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withCatchLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e){
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceCatchLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withCatchLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e){
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1896")
    @Test
    void aroundExceptionDelimiterFalse() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withBitwise(false))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (IllegalAccessException | IllegalStateException | IllegalArgumentException e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (IllegalAccessException|IllegalStateException|IllegalArgumentException e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1896")
    @Test
    void aroundExceptionDelimiterTrue() {
        rewriteRun(
          spaces(style -> style.withAroundOperators(style.getAroundOperators().withBitwise(true))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (IllegalAccessException|IllegalStateException|IllegalArgumentException e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (IllegalAccessException | IllegalStateException | IllegalArgumentException e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceFinallyLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withFinallyLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      } finally {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      } finally{
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceFinallyLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withFinallyLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      } finally{
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      } finally {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceSynchronizedLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withSynchronizedLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      synchronized (this) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      synchronized (this){
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceSynchronizedLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withSynchronizedLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      synchronized (this){
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      synchronized (this) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceArrayInitializerLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withArrayInitializerLeftBrace(true))),
          java(
            """
              class Test {
                  public void foo() {
                      int[] arr = new int[]{1, 2, 3};
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int[] arr = new int[] {1, 2, 3};
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceArrayInitializerLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withArrayInitializerLeftBrace(false))),
          java(
            """
              class Test {
                  public void foo() {
                      int[] arr = new int[] {1, 2, 3};
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int[] arr = new int[]{1, 2, 3};
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1162")
    @Test
    void beforeLeftBraceAnnotationArrayInitializerLeftBraceTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withAnnotationArrayInitializerLeftBrace(true))),
          java(
            """
              package abc;
              @interface MyAnno {
                  String[] names;
                  Integer[] counts;
              }
              """
          ),
          java(
            """
              package abc;
              @MyAnno(names={"a","b"},counts={1,2})
              class Test {
              }
              """,
            """
              package abc;
              @MyAnno(names = {"a", "b"}, counts = {1, 2})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void beforeLeftBraceAnnotationArrayInitializerLeftBraceFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeLeftBrace(style.getBeforeLeftBrace().withAnnotationArrayInitializerLeftBrace(false))),
          java(
            """
              @SuppressWarnings( {"ALL"})
              class Test {
              }
              """,
            """
              @SuppressWarnings({"ALL"})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsElseKeywordFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withElseKeyword(false))),
          java(
            """
              class Test {
                  public void foo() {
                      if (true) {
                      } else {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if (true) {
                      }else {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsElseKeywordTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withElseKeyword(true))),
          java(
            """
              class Test {
                  public void foo() {
                      if (true) {
                      }else {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if (true) {
                      } else {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsWhileKeywordFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withWhileKeyword(false))),
          java(
            """
              class Test {
                  public void foo() {
                      do {
                      } while (true);
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      do {
                      }while (true);
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsWhileKeywordTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withWhileKeyword(true))),
          java(
            """
              class Test {
                  public void foo() {
                      do {
                      }while (true);
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      do {
                      } while (true);
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsCatchKeywordFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withCatchKeyword(false))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      }catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsCatchKeywordTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withCatchKeyword(true))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      }catch (Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsFinallyKeywordFalse() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withFinallyKeyword(false))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      } finally {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }finally {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeKeywordsFinallyKeywordTrue() {
        rewriteRun(
          spaces(style -> style.withBeforeKeywords(style.getBeforeKeywords().withFinallyKeyword(true))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }finally {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      } finally {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinCodeBracesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withCodeBraces(true))),
          java(
            """
              class Test {}
              interface ITest {}
              """,
            """
              class Test { }
              interface ITest { }
              """
          )
        );
    }

    @Test
    void withinCodeBracesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withCodeBraces(false))),
          java(
            """
              class Test { }
              interface ITest { }
              """,
            """
              class Test {}
              interface ITest {}
              """
          )
        );
    }

    @Test
    void withinBracketsTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withBrackets(true))),
          java(
            """
              class Test {
                  public void foo(int[] a) {
                      int x = a[0];
                  }
              }
              """,
            """
              class Test {
                  public void foo(int[] a) {
                      int x = a[ 0 ];
                  }
              }
              """
          )
        );
    }

    @Test
    void withinBracketsFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withBrackets(false))),
          java(
            """
              class Test {
                  public void foo(int[] a) {
                      int x = a[ 0 ];
                  }
              }
              """,
            """
              class Test {
                  public void foo(int[] a) {
                      int x = a[0];
                  }
              }
              """
          )
        );
    }

    @Test
    void withinArrayInitializerBracesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withArrayInitializerBraces(true))),
          java(
            """
              class Test {
                  public void foo() {
                      int[] x = {1, 2, 3};
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int[] x = { 1, 2, 3 };
                  }
              }
              """
          )
        );
    }

    @Test
    void withinArrayInitializerBracesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withArrayInitializerBraces(false))),
          java(
            """
              class Test {
                  public void foo() {
                      int[] x = { 1, 2, 3 };
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int[] x = {1, 2, 3};
                  }
              }
              """
          )
        );
    }

    @Test
    void withinEmptyArrayInitializerBracesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withEmptyArrayInitializerBraces(true))),
          java(
            """
              class Test {
                  public void foo() {
                      int[] x = {};
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int[] x = { };
                  }
              }
              """
          )
        );
    }

    @Test
    void withinEmptyArrayInitializerBracesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withEmptyArrayInitializerBraces(false))),
          java(
            """
              class Test {
                  public void foo() {
                      int[] x = { };
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int[] x = {};
                  }
              }
              """
          )
        );
    }

    @Test
    void withinGroupingParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withGroupingParentheses(true))),
          java(
            """
              class Test {
                  public void foo(int x) {
                      x += (x + 1);
                  }
              }
              """,
            """
              class Test {
                  public void foo(int x) {
                      x += ( x + 1 );
                  }
              }
              """
          )
        );
    }

    @Test
    void withinGroupingParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withGroupingParentheses(false))),
          java(
            """
              class Test {
                  public void foo(int x) {
                      x += ( x + 1 );
                  }
              }
              """,
            """
              class Test {
                  public void foo(int x) {
                      x += (x + 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void withinMethodDeclarationParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withMethodDeclarationParentheses(true))),
          java(
            """
              class Test {
                  public void foo(int x) {
                  }
                  public void bar(    int y    ) {
                  }
              }
              """,
            """
              class Test {
                  public void foo( int x ) {
                  }
                  public void bar( int y ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void compositeMethodDeclarationParentheses() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withMethodDeclarationParentheses(true))
              .withBeforeParentheses(style.getBeforeParentheses().withMethodDeclaration(true))
          ),
          java(
            """
              class Test {
                  void  /*c1*/   foo  /*c2*/   (  /*c3*/   int x, int y  /*c4*/   ) {
                  }
              }
              """,
            """
              class Test {
                  void  /*c1*/   foo  /*c2*/ (  /*c3*/   int x, int y  /*c4*/ ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void withinMethodDeclarationParenthesesTrueWithComment() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withMethodDeclarationParentheses(true))),
          java(
            """
              class Test {
                  void foo(    /*c1*/    int x    ) {
                  }
                  void bar(    int y    /*c2*/    ) {
                  }
                  void baz(    /*c3*/    int z    /*c4*/    ) {
                  }
              }
              """,
            """
              class Test {
                  void foo(    /*c1*/    int x ) {
                  }
                  void bar( int y    /*c2*/ ) {
                  }
                  void baz(    /*c3*/    int z    /*c4*/ ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void withinMethodDeclarationParenthesesTrueWithLineBreakIgnored() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withMethodDeclarationParentheses(true))),
          java(
            """
              class Test {
                  void foo(
                      int x
                  ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void withinMethodDeclarationParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withMethodDeclarationParentheses(false))),
          java(
            """
              class Test {
                  public void foo( int x ) {
                  }
              }
              """,
            """
              class Test {
                  public void foo(int x) {
                  }
              }
              """
          )
        );
    }

    @Test
    void withinEmptyMethodDeclarationParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withEmptyMethodDeclarationParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                  }
              }
              """,
            """
              class Test {
                  public void foo( ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void withinEmptyMethodDeclarationParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withEmptyMethodDeclarationParentheses(false))),
          java(
            """
              class Test {
                  public void foo( ) {
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void withinMethodCallParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withMethodCallParentheses(true))),
          java(
            """
              class Test {
                  public void bar(int x) {
                  }
                  public void foo() {
                      bar(1);
                  }
              }
              """,
            """
              class Test {
                  public void bar(int x) {
                  }
                  public void foo() {
                      bar( 1 );
                  }
              }
              """
          )
        );
    }

    @Test
    void withinMethodCallParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withMethodCallParentheses(false))),
          java(
            """
              class Test {
                  public void bar(int x) {
                  }
                  public void foo() {
                      bar( 1 );
                  }
              }
              """,
            """
              class Test {
                  public void bar(int x) {
                  }
                  public void foo() {
                      bar(1);
                  }
              }
              """
          )
        );
    }

    @Test
    void withinEmptyMethodCallParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withEmptyMethodCallParentheses(true))),
          java(
            """
              class Test {
                  public void bar() {
                  }
                  public void foo() {
                      bar();
                  }
              }
              """,
            """
              class Test {
                  public void bar() {
                  }
                  public void foo() {
                      bar( );
                  }
              }
              """
          )
        );
    }

    @Test
    void withinEmptyMethodCallParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withEmptyMethodCallParentheses(false))),
          java(
            """
              class Test {
                  public void bar() {
                  }
                  public void foo() {
                      bar( );
                  }
              }
              """,
            """
              class Test {
                  public void bar() {
                  }
                  public void foo() {
                      bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void withinIfParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withIfParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                      if (true) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if ( true ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinIfParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withIfParentheses(false))),
          java(
            """
              class Test {
                  public void foo() {
                      if ( true ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      if (true) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinForParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withForParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                      for (int j : new int[]{1, 2, 3}) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      for ( int i = 0; i < 10; i++ ) {
                      }
                      for ( int j : new int[]{1, 2, 3} ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinForParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withForParentheses(false))),
          java(
            """
              class Test {
                  public void foo() {
                      for ( int i = 0; i < 10; i++ ) {
                      }
                      for ( int j : new int[]{1, 2, 3} ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                      for (int j : new int[]{1, 2, 3}) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinWhileParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withWhileParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                      while (true) {
                      }
                      do {
                      } while (true);
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      while ( true ) {
                      }
                      do {
                      } while ( true );
                  }
              }
              """
          )
        );
    }

    @Test
    void withinWhileParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withWhileParentheses(false))),
          java(
            """
              class Test {
                  public void foo() {
                      while ( true ) {
                      }
                      do {
                      } while ( true );
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      while (true) {
                      }
                      do {
                      } while (true);
                  }
              }
              """
          )
        );
    }

    @Test
    void withinSwitchParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withSwitchParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                      switch (1) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      switch ( 1 ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinSwitchParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withSwitchParentheses(false))),
          java(
            """
              class Test {
                  public void foo() {
                      switch ( 1 ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      switch (1) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinTryParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withTryParentheses(true))),
          tryResource,
          java(
            """
              class Test {
                  public void foo() {
                      try (MyResource res = new MyResource()) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try ( MyResource res = new MyResource() ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinTryParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withTryParentheses(false))),
          tryResource,
          java(
            """
              class Test {
                  public void foo() {
                      try ( MyResource res = new MyResource() ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try (MyResource res = new MyResource()) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinCatchParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withCatchParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch ( Exception e ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinCatchParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withCatchParentheses(false))),
          java(
            """
              class Test {
                  public void foo() {
                      try {
                      } catch ( Exception e ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinSynchronizedParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withSynchronizedParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                      synchronized (this) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      synchronized ( this ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinSynchronizedParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withSynchronizedParentheses(false))),
          java(
            """
              class Test {
                  public void foo() {
                      synchronized ( this ) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      synchronized (this) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withinTypeCastParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withTypeCastParentheses(true))),
          java(
            """
              class Test {
                  public void foo() {
                      int i = (int) 0.0d;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int i = ( int ) 0.0d;
                  }
              }
              """
          )
        );
    }

    @Test
    void withinTypeCastParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withTypeCastParentheses(false))),
          java(
            """
              class Test {
                  public void foo() {
                      int i = ( int ) 0.0d;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      int i = (int) 0.0d;
                  }
              }
              """
          )
        );
    }

    @Test
    void withinAnnotationParenthesesTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withAnnotationParentheses(true))),
          java(
            """
              @SuppressWarnings({"ALL"})
              class Test {
              }
              """,
            """
              @SuppressWarnings( {"ALL"} )
              class Test {
              }
              """
          )
        );
    }

    @Test
    void withinAnnotationParenthesesFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withAnnotationParentheses(false))),
          java(
            """
              @SuppressWarnings( {"ALL"} )
              class Test {
              }
              """,
            """
              @SuppressWarnings({"ALL"})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void withinAngleBracketsTrue() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withAngleBrackets(true))),
          java(
            """
              import java.util.ArrayList;
              import java.util.List;
                            
              class Test<T, U> {
                            
                  <T2 extends T> T2 foo() {
                      List<T2> myList = new ArrayList<>();
                      return null;
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;
                            
              class Test< T, U > {
                            
                  < T2 extends T > T2 foo() {
                      List< T2 > myList = new ArrayList<>();
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void withinAngleBracketsFalse() {
        rewriteRun(
          spaces(style -> style.withWithin(style.getWithin().withAngleBrackets(false))),
          java(
            """
              import java.util.ArrayList;
              import java.util.List;
                            
              class Test< T, U > {
                            
                  < T2 extends T > T2 foo() {
                      List< T2 > myList = new ArrayList<>();
                      return null;
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;
                            
              class Test<T, U> {
                            
                  <T2 extends T> T2 foo() {
                      List<T2> myList = new ArrayList<>();
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorBeforeQuestionMarkFalse() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withBeforeQuestionMark(false))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b? 1 : 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorBeforeQuestionMarkTrue() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withBeforeQuestionMark(true))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b? 1 : 0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorAfterQuestionMarkFalse() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withAfterQuestionMark(false))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ?1 : 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorAfterQuestionMarkTrue() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withAfterQuestionMark(true))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ?1 : 0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorBeforeColonFalse() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withBeforeColon(false))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1: 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorBeforeColonTrue() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withBeforeColon(true))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1: 0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorAfterColonFalse() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withAfterColon(false))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 :0;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryOperatorAfterColonTrue() {
        rewriteRun(
          spaces(style -> style.withTernaryOperator(style.getTernaryOperator().withAfterColon(true))),
          java(
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 :0;
                  }
              }
              """,
            """
              class Test {
                  public void foo() {
                      boolean b = true;
                      int x = b ? 1 : 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void typeArgumentsAfterCommaFalse() {
        rewriteRun(
          spaces(style -> style.withTypeArguments(style.getTypeArguments().withAfterComma(false))),
          java(
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """,
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String,String> m = new HashMap<String,String>();
                      Test.<String,Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """
          )
        );
    }

    @Test
    void typeArgumentsAfterCommaTrue() {
        rewriteRun(
          spaces(style -> style.withTypeArguments(style.getTypeArguments().withAfterComma(true))),
          java(
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String,String> m = new HashMap<String,String>();
                      Test.<String,Integer>bar(1,2);
                  }
                  static <A,B> void bar(int x,int y) {
                  }
              }
              """,
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer>bar(1, 2);
                  }
                  static <A, B> void bar(int x, int y) {
                  }
              }
              """
          )
        );
    }

    @Test
    void typeArgumentsBeforeOpeningAngleBracketTrue() {
        rewriteRun(
          spaces(style -> style.withTypeArguments(style.getTypeArguments().withBeforeOpeningAngleBracket(true))),
          java(
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """,
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map <String, String> m = new HashMap <String, String>();
                      Test. <String, Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """
          )
        );
    }

    @Test
    void typeArgumentsBeforeOpeningAngleBracketFalse() {
        rewriteRun(
          spaces(style -> style.withTypeArguments(style.getTypeArguments().withBeforeOpeningAngleBracket(false))),
          java(
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map <String, String> m = new HashMap <String, String>();
                      Test. <String, Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """,
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """
          )
        );
    }

    @Test
    void typeArgumentsAfterClosingAngleBracketTrue() {
        rewriteRun(
          spaces(style -> style.withTypeArguments(style.getTypeArguments().withAfterClosingAngleBracket(true))),
          java(
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """,
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer> bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """
          )
        );
    }

    @Test
    void typeArgumentsAfterClosingAngleBracketFalse() {
        rewriteRun(
          spaces(style -> style.withTypeArguments(style.getTypeArguments().withAfterClosingAngleBracket(false))),
          java(
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer> bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """,
            """
              import java.util.Map;
              import java.util.HashMap;

              class Test {
                  void foo() {
                      Map<String, String> m = new HashMap<String, String>();
                      Test.<String, Integer>bar();
                  }
                  static <A, B> void bar() {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaTrueNewArrayInitializer() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
          java(
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1, 2, 3};
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1 , 2 , 3};
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaFalseNewArrayInitializer() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
          java(
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1 , 2 , 3};
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1, 2, 3};
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaFalseNewArrayInitializer() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
          java(
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1, 2, 3};
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1,2,3};
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaTrueNewArrayInitializer() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
          java(
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1,2,3};
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int[] arr = new int[]{1, 2, 3};
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaTrueMethodDeclArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
          java(
            """
              class Test {
                  void bar(int x, int y) {
                  }
              }
              """,
            """
              class Test {
                  void bar(int x , int y) {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaFalseMethodDeclArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
          java(
            """
              class Test {
                  void bar(int x , int y) {
                  }
              }
              """,
            """
              class Test {
                  void bar(int x, int y) {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaFalseMethodDeclArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
          java(
            """
              class Test {
                  void bar(int x, int y) {
                  }
              }
              """,
            """
              class Test {
                  void bar(int x,int y) {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaTrueMethodDeclArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
          java(
            """
              class Test {
                  void bar(int x,int y) {
                  }
              }
              """,
            """
              class Test {
                  void bar(int x, int y) {
                  }
              }
              """
          )
        );
    }

    //language=java
    SourceSpecs methodInvocationDependsOn = java(
      """
        class A {
            void bar(int x, int y) {
            }
        }
        """,
      SourceSpec::skip
    );

    @Test
    void otherBeforeCommaTrueMethodInvocationParams() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
          methodInvocationDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A().bar(1, 2);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A().bar(1 , 2);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaFalseMethodInvocationParams() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
          methodInvocationDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A().bar(1 , 2);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A().bar(1, 2);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaFalseMethodInvocationParams() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
          methodInvocationDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A().bar(1, 2);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A().bar(1,2);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaTrueMethodInvocationParams() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
          methodInvocationDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A().bar(1,2);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A().bar(1, 2);
                  }
              }
              """
          )
        );
    }

    //language=java
    SourceSpecs newClassArgsDependsOn = java(
      """
        class A {
            A(String str, int num) {
            }
        }
        """,
      SourceSpec::skip
    );

    @Test
    void otherBeforeCommaTrueNewClassArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
          newClassArgsDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A("hello", 1);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A("hello" , 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaFalseNewClassArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
          newClassArgsDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A("hello" , 1);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A("hello", 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaFalseNewClassArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
          newClassArgsDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A("hello", 1);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A("hello",1);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaTrueNewClassArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
          newClassArgsDependsOn,
          java(
            """
              class Test {
                  void foo() {
                      new A("hello",1);
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      new A("hello", 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaTrueLambdaParameters() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
          java(
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                  }
              }
              """,
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1 , str2) -> "Hello";
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaFalseLambdaParameters() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
          java(
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1 , str2) -> "Hello";
                  }
              }
              """,
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaFalseLambdaParameters() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
          java(
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                  }
              }
              """,
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1,str2) -> "Hello";
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaTrueLambdaParameters() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
          java(
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1,str2) -> "Hello";
                  }
              }
              """,
            """
              import java.util.function.BiFunction;
                            
              class Test {
                  void foo() {
                      BiFunction<String, String, String> f = (str1, str2) -> "Hello";
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaTrueForLoopUpdate() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
          java(
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++, x++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++ , x++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaFalseForLoopUpdate() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
          java(
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++ , x++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++, x++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaFalseForLoopUpdate() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
          java(
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++, x++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++,x++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaTrueForLoopUpdate() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
          java(
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++,x++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int n = 0, x = 0; n < 100; n++, x++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaTrueEnumValueInitArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
          java(
            """
              enum Test {
                  TEST1("str1", 1),
                  TEST2("str2", 2);
                  
                  Test(String str, int num) {
                  }
              }
              """,
            """
              enum Test {
                  TEST1("str1" , 1),
                  TEST2("str2" , 2);
                  
                  Test(String str , int num) {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeCommaFalseEnumValueInitArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
          java(
            """
              enum Test {
                  TEST1("str1" , 1),
                  TEST2("str2" , 2);
                  
                  Test(String str , int num) {
                  }
              }
              """,
            """
              enum Test {
                  TEST1("str1", 1),
                  TEST2("str2", 2);
                  
                  Test(String str, int num) {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaFalseEnumValueInitArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
          java(
            """
              enum Test {
                  TEST1("str1", 1),
                  TEST2("str2", 2);
                  
                  Test(String str, int num) {
                  }
              }
              """,
            """
              enum Test {
                  TEST1("str1",1),
                  TEST2("str2",2);
                  
                  Test(String str,int num) {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterCommaTrueEnumValueInitArgs() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
          java(
            """
              enum Test {
                  TEST1("str1",1),
                  TEST2("str2",2);
                  
                  Test(String str,int num) {
                  }
              }
              """,
            """
              enum Test {
                  TEST1("str1", 1),
                  TEST2("str2", 2);
                  
                  Test(String str, int num) {
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeForSemicolonTrue() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeForSemicolon(true))),
          java(
            """
              class Test {
                  void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int i = 0 ; i < 10 ; i++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeForSemicolonFalse() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeForSemicolon(false))),
          java(
            """
              class Test {
                  void foo() {
                      for (int i = 0 ; i < 10 ; i++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterForSemicolonFalse() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterForSemicolon(false))),
          java(
            """
              class Test {
                  void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int i = 0;i < 10;i++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterForSemicolonTrue() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterForSemicolon(true))),
          java(
            """
              class Test {
                  void foo() {
                      for (int i = 0;i < 10;i++) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int i = 0; i < 10; i++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterTypeCastFalse() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterTypeCast(false))),
          java(
            """
              class Test {
                  void foo() {
                      int i = (int) 0.0d;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int i = (int)0.0d;
                  }
              }
              """
          )
        );
    }

    @Test
    void otherAfterTypeCastTrue() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withAfterTypeCast(true))),
          java(
            """
              class Test {
                  void foo() {
                      int i = (int)0.0d;
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      int i = (int) 0.0d;
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeColonInForEachFalse() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeColonInForEach(false))),
          java(
            """
              class Test {
                  void foo() {
                      for (int i : new int[]{1, 2, 3}) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int i: new int[]{1, 2, 3}) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherBeforeColonInForEachTrue() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withBeforeColonInForEach(true))),
          java(
            """
              class Test {
                  void foo() {
                      for (int i: new int[]{1, 2, 3}) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                      for (int i : new int[]{1, 2, 3}) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void otherInsideOneLineEnumBracesTrue() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withInsideOneLineEnumBraces(true))),
          java(
            """
              enum Test {}
              """,
            """
              enum Test { }
              """
          )
        );
    }

    @Test
    void otherInsideOneLineEnumBracesFalse() {
        rewriteRun(
          spaces(style -> style.withOther(style.getOther().withInsideOneLineEnumBraces(false))),
          java(
            """
              enum Test { }
              """,
            """
              enum Test {}
              """
          )
        );
    }

    @Test
    void typeParametersBeforeOpeningAngleBracketTrue() {
        rewriteRun(
          spaces(style -> style.withTypeParameters(style.getTypeParameters().withBeforeOpeningAngleBracket(true))),
          java(
            """
              class Test<T> {
              }
              """,
            """
              class Test <T> {
              }
              """
          )
        );
    }

    @Test
    void typeParametersBeforeOpeningAngleBracketFalse() {
        rewriteRun(
          spaces(style -> style.withTypeParameters(style.getTypeParameters().withBeforeOpeningAngleBracket(false))),
          java(
            """
              class Test <T> {
              }
              """,
            """
              class Test<T> {
              }
              """
          )
        );
    }

    @Test
    void typeParametersAroundTypeBoundsFalse() {
        rewriteRun(
          spaces(style -> style.withTypeParameters(style.getTypeParameters().withAroundTypeBounds(false))),
          java(
            """
              class Test<T extends Integer & Appendable> {
              }
              """,
            """
              class Test<T extends Integer&Appendable> {
              }
              """
          )
        );
    }

    @Test
    void typeParametersAroundTypeBoundsTrue() {
        rewriteRun(
          spaces(style -> style.withTypeParameters(style.getTypeParameters().withAroundTypeBounds(true))),
          java(
            """
              class Test<T extends Integer&Appendable> {
              }
              """,
            """
              class Test<T extends Integer & Appendable> {
              }
              """
          )
        );
    }

    @Test
    void noSpaceInitializerPadding() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      int i = 0, j = 0;
                      for (; i < j; i++, j--) { }
                  }
              }
              """
          )
        );
    }

    @Test
    void addSpaceToEmptyInitializer() {
        rewriteRun(
          spec -> spec.recipe(new Spaces())
            .parser(JavaParser.fromJavaVersion().styles(singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(),
                singletonList(new EmptyForInitializerPadStyle(true))
              )
            ))),
          java(
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for (; i < j; i++, j--) { }
                  }
              }
              """,
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for ( ; i < j; i++, j--) { }
                  }
              }
              """
          )
        );
    }

    @Test
    void removeSpaceFromEmptyInitializer() {
        rewriteRun(
          spec -> spec.recipe(new Spaces())
            .parser(JavaParser.fromJavaVersion().styles(singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(),
                singletonList(new EmptyForInitializerPadStyle(false))
              )
            ))),
          java(
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for ( ; i < j; i++, j--) { }
                  }
              }
              """,
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for (; i < j; i++, j--) { }
                  }
              }
              """
          )
        );
    }

    @Test
    void addSpaceToEmptyIterator() {
        rewriteRun(
          spec -> spec.recipe(new Spaces())
            .parser(JavaParser.fromJavaVersion().styles(singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(),
                singletonList(new EmptyForIteratorPadStyle(true))
              )
            ))),
          java(
            """
              public class A {
                  {
                      for (int i = 0; i < 10;) { i++; }
                  }
              }
              """,
            """
              public class A {
                  {
                      for (int i = 0; i < 10; ) { i++; }
                  }
              }
              """
          )
        );
    }

    @Test
    void removeSpaceFromEmptyIterator() {
        rewriteRun(
          spec -> spec.recipe(new Spaces())
            .parser(JavaParser.fromJavaVersion().styles(singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(),
                singletonList(new EmptyForIteratorPadStyle(false))
              )
            ))),
          java(
            """
              public class A {
                  {
                      for (int i = 0; i < 10; ) { i++; }
                  }
              }
              """,
            """
              public class A {
                  {
                      for (int i = 0; i < 10;) { i++; }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1051")
    @Test
    void preserveSpacePrecedingComment() {
        rewriteRun(
          java(
            """
              @Deprecated("version" /* some comment */)
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1064")
    @Test
    void preserveSpacePrecedingCommentInSpaceBefore() {
        rewriteRun(
          java(
            """
              import java.util.List;
              @Deprecated("version" /* some comment */)
              class Test {
                  void foo() {
                      List.of( // another comment
                          1,
                          2
                      );
                  }
              }
              """
          )
        );
    }

    @Test
    void spaceBetweenAnnotations() {
        rewriteRun(
          spaces(),
          java(
            """
              class A {
                  void m(@Deprecated@SuppressWarnings("ALL") int a) {
                  }
              }
              """,
            """
              class A {
                  void m(@Deprecated @SuppressWarnings("ALL") int a) {
                  }
              }
              """
          )
        );
    }
}
