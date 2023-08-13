package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class AutoFormatVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormat());
    }

    @Test
    void keepMaximumBetweenHeaderAndPackage() {
        rewriteRun(
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

    @Test
    void tabsAndIndents() {
        rewriteRun(
          kotlin(
            """
              open class Some {
                  private val f: (Int) -> Int = { a: Int -> a * 2 }
                  fun foo(): Int {
                  val test: Int = 12
                  for (i in 10..42) {
                  println(when {
                  i < test -> -1
                  i > test -> 1
                  else -> 0
                  })
                  }
                  if (true) {
                  }
                  while (true) {
                      break
                  }
                  try {
                      when (test) {
                          12 -> println("foo")
                          in 10..42 -> println("baz")
                          else -> println("bar")
                      }
                  } catch (e: Exception) {
                  } finally {
                  }
                  return test
                  }

                  fun multilineMethod(
                          foo: String,
                          bar: String
                          ) {
                      foo
                              .length
                  }

                  fun expressionBodyMethod() =
                      "abc"
              }

              class AnotherClass<T : Any> : Some()
              """,
            """
              open class Some {
                  private val f: (Int) -> Int = {   a: Int ->
                       a * 2
                  }

                  fun foo(): Int {
                      val test: Int = 12
                      for (i in 10..42) {
                          println(when {
                              i < test -> -1
                              i > test -> 1
                              else -> 0
                          })
                      }
                      if (true) {
                      }
                      while (true) {
                          break
                      }
                      try {
                          when (test) {
                              12 ->
                                  println("foo")
                             \s
                               in 10..42 ->
                                  println("baz")
                             \s
                              else ->
                                  println("bar")
                             \s
                          }
                      } catch (e: Exception) {
                      } finally {
                      }
                      return test
                  }

                  fun multilineMethod(
                            foo: String,
                            bar: String
                            ) {
                      foo
                              .length
                  }

                  fun expressionBodyMethod() =
                     \s
                          "abc"
                 \s
              }

              class AnotherClass <T : Any> : Some()
              """
          )
        );
    }
}

