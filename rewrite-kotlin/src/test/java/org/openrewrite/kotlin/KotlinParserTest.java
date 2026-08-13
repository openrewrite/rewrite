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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("LombokKotlinCompilerPlugin")
class KotlinParserTest implements RewriteTest {

    @Test
    void classDefinitionFromDependsOn() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn("""
            package foo.bar

            class MyClass
            """)),
          kotlin(
            """
              import foo.bar.MyClass

              val myClass: MyClass? = null
              """
          )
        );
    }

    @Test
    void dependsOnWithAbsoluteRelativeTo(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .relativeTo(tempDir)
            .parser(KotlinParser.builder().dependsOn("""
              package foo.bar

              class MyClass
              """)),
          kotlin(
            """
              import foo.bar.MyClass

              val myClass: MyClass? = null
              """
          )
        );
    }

    @Test
    void dependsOnDoesNotLeakAsParseErrorWhenParseThrows() {
        KotlinParser parser = KotlinParser.builder()
          .dependsOn(
            """
              package foo.bar

              class MyClass
              """
          )
          .build();

        // An input whose source supplier throws on its first call forces KotlinParser#parse
        // to fail before any per-CU processing, exercising the catch block in parseInputs.
        // Subsequent calls return an empty stream so ParseError.build can still capture the input.
        AtomicInteger callCount = new AtomicInteger();
        Parser.Input throwingInput = new Parser.Input(
          Paths.get("Bad.kt"),
          null,
          () -> {
              if (callCount.getAndIncrement() == 0) {
                  throw new RuntimeException("intentional parse failure");
              }
              return new ByteArrayInputStream(new byte[0]);
          },
          true
        );

        List<SourceFile> results = parser
          .parseInputs(singletonList(throwingInput), null, new InMemoryExecutionContext(t -> {
          }))
          .collect(Collectors.toList());

        // dependsOn must not leak into the returned stream on the error path
        assertThat(results)
          .extracting(SourceFile::getSourcePath)
          .containsExactly(Paths.get("Bad.kt"));
    }

    @Test
    void multiDollarStringInterpolation() {
        rewriteRun(
          kotlin(
            """
              val x = $$"$something"
              """
          )
        );
    }

    @Test
    void multiDollarStringInterpolationWithBlockExpression() {
        rewriteRun(
          kotlin(
            """
              val name = "World"
              val x = $$"Hello $${name}!"
              """
          )
        );
    }

    @Test
    void multiDollarStringInterpolationWithSimpleName() {
        rewriteRun(
          kotlin(
            """
              val name = "World"
              val x = $$"Hello $$name!"
              """
          )
        );
    }

    @Test
    void multiDollarStringInterpolationLiteralDollarBeforeInterpolation() {
        rewriteRun(
          kotlin(
            """
              val name = "World"
              val x = $$"$$${name}"
              """
          )
        );
    }

    @Test
    void multiDollarStringInterpolationTwoBlocksWithLiteralDollarBetween() {
        // Mirrors the pattern in KotlinTypeSignatureBuilder.kt that originally
        // triggered the parse failure: a multi-dollar string with two block
        // interpolations separated by a literal '$' (the JVM-style Outer$Inner FQN).
        rewriteRun(
          kotlin(
            """
              val outer = "java.lang.Object"
              val inner = "Entry"
              val fqn = $$"$${outer}$$${inner}"
              """
          )
        );
    }

    @Test
    void tripleDollarStringInterpolation() {
        rewriteRun(
          kotlin(
            """
              val name = "World"
              val x = $$$"Hello $$$name!"
              """
          )
        );
    }

    @Test
    void parenthesizedThisAsInnerClassConstructorReceiver() {
        rewriteRun(
          kotlin(
            """
              open class A(val value: String) {
                  inner class B(val s: String)
              }

              class C : A("fromC") {
                  inner class X : A("fromX") {
                      fun f() = (this as A).B("OK")
                      fun g() = (this@X).B("OK")
                  }
              }
              """
          )
        );
    }

    // The cases below were found by parse-printing a corpus of ~7,000 real Kotlin sources; each was silently
    // dropped or mangled by the LST mapping.

    // Context parameters (Kotlin 2.2) are dropped from both function and property declarations.
    @Test
    void contextParameterOnDeclaration() {
        rewriteRun(
          kotlin(
            """
              class Ctx

              context(c: Ctx)
              fun f(): Int = 1

              context(c: Ctx)
              val v: Int get() = 1
              """
          )
        );
    }

    @Test
    void contextParameterInFunctionType() {
        rewriteRun(
          kotlin(
            """
              class Box

              fun produce(box: context(Box) () -> Unit): Unit = TODO()
              """
          )
        );
    }

    // Annotations may sit on either side of the context parameters, which print between the leading
    // annotations and the modifiers.
    @Test
    void contextParameterOrderedAgainstAnnotationsAndModifiers() {
        rewriteRun(
          kotlin(
            """
              class A
              class B

              @Deprecated("x")
              context(a: A)
              private fun f(): Int = 1

              context(a: A)
              @Deprecated("x")
              fun g(): Int = 1

              context (a: A, b: B)
              fun h(): Int = 1
              """
          )
        );
    }

    // `when` guards (Kotlin 2.1) are dropped from the branch condition, silently widening the branch.
    @Test
    void whenGuardOnBranchCondition() {
        rewriteRun(
          kotlin(
            """
              fun f(x: Any, flag: Boolean): Int = when (x) {
                  is String if flag -> 1
                  in setOf(1, 2) if flag -> 2
                  true if flag -> 3
                  else -> 4
              }
              """
          )
        );
    }

    @Test
    void whenGuardOnElseBranch() {
        rewriteRun(
          kotlin(
            """
              fun f(x: Any, flag: Boolean): Int = when (x) {
                  is String -> 1
                  else if flag -> 2
                  else -> 3
              }
              """
          )
        );
    }

    // A bracketed use-site annotation list loses both the brackets and the `file:` target, so the
    // annotations are reattached to the wrong element.
    @Test
    void bracketedFileAnnotations() {
        rewriteRun(
          kotlin(
            """
              @file:[JvmName("Foo") JvmMultifileClass]

              package foo
              """
          )
        );
    }

    // The space after `suspend` is moved to before it, producing the unparseable `suspendInt`.
    @Test
    void parenthesizedSuspendFunctionType() {
        rewriteRun(
          kotlin(
            """
              fun f(): (suspend Int.() -> Int)? = null
              """
          )
        );
    }

    // Explicit backing fields are dropped, taking their initializer with them.
    @Test
    void explicitBackingField() {
        rewriteRun(
          kotlin(
            """
              class A {
                  val numbers: List<Int>
                      field = mutableListOf<Int>()

                  val others: List<Int>
                      field: MutableList<Int> = mutableListOf()
              }
              """
          )
        );
    }

    // A semicolon terminating the `then` branch of an `if` is dropped.
    @Test
    void semicolonBeforeElse() {
        rewriteRun(
          kotlin(
            """
              fun f(k: Int): Int {
                  if (k == 0) return 1; else return 2
              }
              """
          )
        );
    }

}
