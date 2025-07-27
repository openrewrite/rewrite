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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.style.BlankLinesStyle;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("all")
class BlankLinesTest implements RewriteTest {
    private static Consumer<RecipeSpec> blankLines() {
        return blankLines(style -> style);
    }

    private static Consumer<RecipeSpec> blankLines(UnaryOperator<BlankLinesStyle> with) {
        BlankLinesStyle blankLinesStyle = with.apply(IntelliJ.blankLines());

        //noinspection rawtypes
        return spec -> spec.recipe(toRecipe(() -> new BlankLinesVisitor<>(blankLinesStyle)))
          .parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              randomId(), "test", "test", "test", emptySet(),
              singletonList(blankLinesStyle)
            )
          )));
    }

    @Nested
    class KeepMaximumBlankLinesTest {

        @DocumentExample
        @Test
        void keepMaximumInDeclarations() {
            rewriteRun(
              blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withInDeclarations(0)))
                .andThen(spec -> spec.expectedCyclesThatMakeChanges(2)),
              kotlin(
                """
                  import java.util.List



                  class Test {


                      private var field1: Int = 0
                      private var field2: Int = 0

                      init {
                          field1 = 2
                      }

                      fun test0() = 1
                      fun test1() {
                          object : Runnable {
                              override fun run() {
                              }
                          }
                      }

                      inner class InnerClass {
                      }

                      enum class InnerEnum {
                          FIRST,
                          SECOND
                      }
                  }
                  """,
                """
                  import java.util.List

                  class Test {
                      private var field1: Int = 0
                      private var field2: Int = 0

                      init {
                          field1 = 2
                      }

                      fun test0() = 1
                      fun test1() {
                          object : Runnable {
                              override fun run() {
                              }
                          }
                      }

                      inner class InnerClass {
                      }

                      enum class InnerEnum {
                          FIRST,
                          SECOND
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepMaximumBlankLinesInClassDeclarations() {
            rewriteRun(
              blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withInDeclarations(1))),
              kotlin(
                """
                  class A {}



                  class B {}
                  """,
                """
                  class A {}

                  class B {}
                  """
              )
            );
        }

        @Test
        void keepMaximumInCode() {
            rewriteRun(
              blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withInCode(0))),
              kotlin(
                """
                  class Test {
                      private var field1: Int = 0

                      init {
                  
                  
                          field1 = 2
                      }
                  }
                  """,
                """
                  class Test {
                      private var field1: Int = 0

                      init {
                          field1 = 2
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepMaximumBeforeEndOfBlock() {
            rewriteRun(
              blankLines(style -> style.withKeepMaximum(style.getKeepMaximum().withBeforeEndOfBlock(0))),
              kotlin(
                """
                  class Test {
                      private var field1: Int = 0

                      init {
                          field1 = 2
                  
                  
                      }
                  
                      enum class Test {
                          FIRST,
                          SECOND
                  
                      }
                  }
                  """,
                """
                  class Test {
                      private var field1: Int = 0

                      init {
                          field1 = 2
                      }
                  
                      enum class Test {
                          FIRST,
                          SECOND
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepMaximumBetweenHeaderAndPackage() {
            rewriteRun(
              blankLines(),
              kotlin(
                """
                  /*
                   * This is a sample file.
                   */
                  
                  
                  
                  
                  package com.intellij.samples
                  
                  class Test {
                  }
                  """,
                """
                  /*
                   * This is a sample file.
                   */
                  
                  
                  package com.intellij.samples
                  
                  class Test {
                  }
                  """
              )
            );
        }
    }

    @Nested
    class MinimumBlankLinesTest {

        @Nested
        class AfterClassHeader {

            @Test
            void minimumAfterClassHeader() {
                rewriteRun(
                  blankLines(style -> style.withMinimum(style.getMinimum().withAfterClassHeader(1))),
                  kotlin(
                    """
                      class Test {
                          private val field1: Int = 0
                      }
                      """,
                    """
                      class Test {
                      
                          private val field1: Int = 0
                      }
                      """
                  )
                );
            }

            @Issue("https://github.com/openrewrite/rewrite/issues/1171")
            @Test
            void minimumAfterClassHeaderNestedClasses() {
                rewriteRun(
                  blankLines(style -> style.withMinimum(style.getMinimum().withAfterClassHeader(1))),
                  kotlin(
                    """
                      class OuterClass {
                          class InnerClass0 {
                              private val unused: Int = 0
                          }

                          class InnerClass1 {
                              private val unused: Int = 0
                          }
                      }
                      """,
                    """
                      class OuterClass {
                      
                          class InnerClass0 {
                      
                              private val unused: Int = 0
                          }
                      
                          class InnerClass1 {
                      
                              private val unused: Int = 0
                          }
                      }
                      """
                  )
                );
            }

            @Issue("https://github.com/openrewrite/rewrite/issues/1171")
            @Test
            void minimumAfterClassHeaderNestedEnum() {
                rewriteRun(
                  blankLines(style -> style.withMinimum(style.getMinimum().withAfterClassHeader(1))),
                  kotlin(
                    """
                      class OuterClass {
                          enum class InnerEnum0 {
                              FIRST,
                              SECOND
                          }
                      }
                      """,
                    """
                      class OuterClass {
                      
                          enum class InnerEnum0 {
                              FIRST,
                              SECOND
                          }
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class AroundWhenBranchesWithBlockTest {
            @Test
            void AroundWhenBranchesWithBlock() {
                rewriteRun(
                  blankLines(style -> style.withMinimum(style.getMinimum().withAroundWhenBranchWithBraces(2))),
                  kotlin(
                    """
                      fun foo1(condition: Int) {
                          when (condition) {
                              1 -> {
                                  println("1")
                              }
                              2 -> {
                                  println("2")
                              }
                              3 -> println("3")
                              4 -> println("4")
                          }
                      }
                      """,
                    """
                      fun foo1(condition: Int) {
                          when (condition) {
                              1 -> {
                                  println("1")
                              }


                              2 -> {
                                  println("2")
                              }


                              3 -> println("3")
                              4 -> println("4")
                          }
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class BeforeDeclarationWithCommentOrAnnotationTest {

            @Test
            void beforeDeclarationWithComment() {
                rewriteRun(
                  blankLines(style -> style.withMinimum(style.getMinimum().withBeforeDeclarationWithCommentOrAnnotation(3)))
                    .andThen(spec -> spec.expectedCyclesThatMakeChanges(2)),
                  kotlin(
                    """
                      annotation class Annotation

                      class Bar {
                          fun d() = Unit

                          // smth
                          fun e() {
                              d()
                          }
                      }
                      """,
                    """
                      annotation class Annotation

                      class Bar {
                          fun d() = Unit



                          // smth
                          fun e() {
                              d()
                          }
                      }
                      """
                  )
                );
            }

            @Test
            void feforeAnnotation() {
                rewriteRun(
                  blankLines(style -> style.withMinimum(style.getMinimum().withBeforeDeclarationWithCommentOrAnnotation(3)))
                    .andThen(spec -> spec.expectedCyclesThatMakeChanges(2)),
                  kotlin(
                    """
                      annotation class Annotation

                      class Bar {
                          @Annotation
                          val a = 42

                          @Annotation
                          val b = 43

                          @Annotation
                          var c = 44

                          @Annotation
                          fun method() {
                          }
                      }
                      """,
                    """
                      annotation class Annotation

                      class Bar {
                      
                      
                      
                          @Annotation
                          val a = 42



                          @Annotation
                          val b = 43



                          @Annotation
                          var c = 44



                          @Annotation
                          fun method() {
                          }
                      }
                      """
                  )
                );
            }
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/621")
    @Test
    void leaveTrailingComments() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              class A {
              
                  private val id: Long = 0 // this comment will move to wrong place
              
                  fun id(): Long {
                      return id;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/620")
    @Test
    void noBlankLineForFirstEnum() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              public enum class TheEnum {
                  FIRST,
                  SECOND
              }
              """
          )
        );
    }

    @Test
    void eachMethodOnItsOwnLine() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              class Test {
                  fun a(): Unit {
                  }    fun b(): Unit {
                  }
              }
              """,
            """
              class Test {
                  fun a(): Unit {
                  }

                  fun b(): Unit {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/314")
    @Test
    void blankLinesBetweenTopLevelStatements() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              class T
              fun a(): Unit {
              }
              fun b(): Unit {
              }
              """,
            """
              class T

              fun a(): Unit {
              }

              fun b(): Unit {
              }
              """
          )
        );
    }

    @Test
    void minimumBeforePackage() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              
              
              
              package com.intellij.samples
              
              class Test {
              }
              """,
            """
              package com.intellij.samples
              
              class Test {
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void minimumBeforeImportsWithPackage() {
        rewriteRun(
          // no blank lines if nothing preceding package
          blankLines(),
          kotlin(
            """
              package com.intellij.samples
              import java.util.Vector
              
              class Test {
              }
              """,
            """
              package com.intellij.samples
              
              import java.util.Vector
              
              class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumBeforeImports() {
        rewriteRun(
          // no blank lines if nothing preceding package
          blankLines(),
          kotlin(
            """
              
              import java.util.Vector
              
              class Test {
              }
              """,
            """
              import java.util.Vector
              
              class Test {
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void minimumAfterPackageWithImport() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              package com.intellij.samples
              import java.util.Vector
              
              class Test {
              }
              """,
            """
              package com.intellij.samples
              
              import java.util.Vector
              
              class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumAfterPackage() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              package com.intellij.samples
              class Test {
              }
              """,
            """
              package com.intellij.samples
              
              class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumAfterImports() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              import java.util.Vector;
              class Test {
              }
              """,
            """
              import java.util.Vector;
              
              class Test {
              }
              """
          )
        );
    }

    @Test
    void minimumAroundClass() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              import java.util.Vector
              
              class Test {
              }
              
              class Test2 {
                  class InnerClass0 {
                  }
                  class InnerClass1 {
                  }
              }
              """,
            """
              import java.util.Vector
              
              class Test {
              }
             
              class Test2 {
                  class InnerClass0 {
                  }
              
                  class InnerClass1 {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1171")
    @Test
    void minimumAroundClassNestedEnum() {
        rewriteRun(
          blankLines(),
          // blankLines(style -> style.withMinimum(style.getMinimum().withAroundClass(2))),
          kotlin(
            """
              enum class OuterEnum {
                  FIRST,
                  SECOND
              }
              class OuterClass {
                  enum class InnerEnum0 {
                      FIRST,
                      SECOND
                  }
                  enum class InnerEnum1 {
                      FIRST,
                      SECOND
                  }
              }
              """,
            """
              enum class OuterEnum {
                  FIRST,
                  SECOND
              }
              
              class OuterClass {
                  enum class InnerEnum0 {
                      FIRST,
                      SECOND
                  }
              
                  enum class InnerEnum1 {
                      FIRST,
                      SECOND
                  }
              }
              """
          )
        );
    }

    @Test
    void unchanged() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              package com.intellij.samples
              
              class Test {
              }
              """
          )
        );
    }

    @Test
    void maximumBlankLinesBetweenHeaderAndPackage() {
        // keepMaximumBlankLines_BetweenHeaderAndPackage defaults to 2
        rewriteRun(
          blankLines(),
          kotlin(
            """
              /*
               *  Copyright 2023 XXX, Inc.
               */




              package org.a

              class A {
              }
              """,
            """
              /*
               *  Copyright 2023 XXX, Inc.
               */


              package org.a

              class A {
              }
              """
          )
        );
    }

    @Test
    void minimumBlankLinesBeforePackageStatement() {
        // minimumBlankLines_BeforePackageStatement defaults to 0
        rewriteRun(
          blankLines(),
          kotlin(
            """
              /*
               *  Copyright 2023 XXX, Inc.
               */

              package org.a

              class A {
              }
              """
          )
        );
    }

    @Test
    void topLevelPropertyBeforeStatement() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              val one = 1
              
              class Test
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/324")
    @Test
    void fileAnnotation() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              @file:JvmName("Foo")
              package foo
              """
          )
        );
    }

    @Test
    void annotatedPrimaryConstructor() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              class A @Suppress constructor(val a: Boolean,): Any()
              """
          )
        );
    }

    @Test
    void annotatedLocalVariable() {
        rewriteRun(
          blankLines(),
          kotlin(
            """
              fun f() {
                  @Suppress val i = 1
                  @Suppress val j = 1
              }
              
              val o = object {
                  @Suppress val i = 1
                  @Suppress val j = 1
              }
              
              class T {
                  @Suppress val i = 1
                  @Suppress val j = 1
              }
              """,
            """
              fun f() {
                  @Suppress val i = 1
                  @Suppress val j = 1
              }
              
              val o = object {

                  @Suppress val i = 1
              
                  @Suppress val j = 1
              }
              
              class T {
              
                  @Suppress val i = 1
              
                  @Suppress val j = 1
              }
              """
          )
        );
    }
}
