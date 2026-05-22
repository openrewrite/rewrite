/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ImportTest implements RewriteTest {

    @Test
    void singleImport() {
        rewriteRun(
          scala(
            """
            import scala.collection.mutable
            """
          )
        );
    }

    @Test
    void importExtraSpaceAfterKeyword() {
        rewriteRun(
          scala(
            """
            import  scala.collection.mutable
            """
          )
        );
    }
    
    @Test
    void singleImportNoNewline() {
        // Test import without trailing newline (from SingleImportDebugTest)
        rewriteRun(
          scala("import scala.collection.mutable")
        );
    }

    @Test
    void javaImport() {
        rewriteRun(
          scala(
            """
            import java.util.List
            """
          )
        );
    }

    @Test
    void wildcardImport() {
        rewriteRun(
          scala(
            """
            import java.util._
            """
          )
        );
    }

    @Test
    void multipleSelectImport() {
        rewriteRun(
          scala(
            """
            import java.util.{List, Map}
            """
          )
        );
    }

    @Test
    void aliasedImport() {
        rewriteRun(
          scala(
            """
            import java.io.{File => JFile}
            """
          )
        );
    }

    @Test
    void multipleImports() {
        rewriteRun(
          scala(
            """
            import scala.collection.mutable
            import java.util.List
            import java.io._
            """
          )
        );
    }

    @Test
    void complexMultiSelectImport() {
        rewriteRun(
          scala(
            """
            import a.b.{c, d => D, _}
            """
          )
        );
    }

    @Test
    void importWithPackage() {
        rewriteRun(
          scala(
            """
            package com.example

            import scala.collection.mutable
            import java.util.List

            val x = 42
            """
          )
        );
    }

    @Test
    void scala3AliasKeyword() {
        rewriteRun(
          scala(
            """
            import java.io.{File as JFile}
            """
          )
        );
    }

    @Test
    void givenSelector() {
        rewriteRun(
          scala(
            """
            import a.b.{given}
            """
          )
        );
    }

    @Test
    void givenWithTypeSelector() {
        rewriteRun(
          scala(
            """
            import a.b.{given Ordering[Int]}
            """
          )
        );
    }

    @Test
    void mixedSelectorsIncludingGiven() {
        rewriteRun(
          scala(
            """
            import a.b.{c, d => D, given Foo, *}
            """
          )
        );
    }

    @Test
    void importInsideClass() {
        rewriteRun(
          scala(
            """
            class C {
              import scala.util._
              val x = 1
            }
            """
          )
        );
    }

    @Test
    void braceImportFollowedByClass() {
        rewriteRun(
          scala(
            """
            package com.example

            import a.b.{c, d}

            class X {
              val y = 1
            }
            """
          )
        );
    }

    @Test
    void braceImportInterleavedWithPlainImports() {
        rewriteRun(
          scala(
            """
            package com.example

            import a.b
            import c.d.{e, f}
            import g.h
            """
          )
        );
    }

    @Test
    void multipleNamedSelectorsRealWorld() {
        rewriteRun(
          scala(
            """
            import com.a.b.{Inspection, InspectionContext, Inspector, Levels}
            """
          )
        );
    }

    @Test
    void hidingAliasUnderscore() {
        // Classic Scala idiom: import everything from Predef except `println`.
        rewriteRun(
          scala(
            """
            import scala.Predef.{println => _, _}
            """
          )
        );
    }

    @Test
    void hidingAliasAsUnderscore() {
        // Scala 3 spelling of the same idiom.
        rewriteRun(
          scala(
            """
            import scala.Predef.{println as _, *}
            """
          )
        );
    }

    @Test
    void givenWithParameterizedType() {
        rewriteRun(
          scala(
            """
            import a.b.{given Conversion[Int, String]}
            """
          )
        );
    }

    @Test
    void backtickedSelector() {
        rewriteRun(
          scala(
            """
            import a.b.{`weird name`, c}
            """
          )
        );
    }

    @Test
    void backtickedQualifier() {
        rewriteRun(
          scala(
            """
            import a.`weird name`.c
            """
          )
        );
    }

    @Test
    void scala3AliasKeywordWithoutBraces() {
        rewriteRun(
          scala(
            """
            import a.b.c.X as Y
            """
          )
        );
    }

    @Test
    void rootPrefix() {
        rewriteRun(
          scala(
            """
            import _root_.scala.collection.mutable
            """
          )
        );
    }

    @Test
    void semicolonAfterImportInsideClass() {
        rewriteRun(
          scala(
            """
            class C { import scala.math._; val x = 1 }
            """
          )
        );
    }

    @Test
    void commentBetweenSelectorNameAndAs() {
        rewriteRun(
          scala(
            """
            import a.b.{X /* hi */ as Y}
            """
          )
        );
    }

    @Test
    void wildcardGivenWithoutBraces() {
        // Imports all givens from a.b — Scala 3 shorthand.
        rewriteRun(
          scala(
            """
            import a.b.given
            """
          )
        );
    }

    @Test
    void commentInsideBraceSelectors() {
        rewriteRun(
          scala(
            """
            import a.b.{c /* the c */, d}
            """
          )
        );
    }

    @Test
    void importInsideMethodBody() {
        rewriteRun(
          scala(
            """
            def foo(): Int =
              import scala.math._
              max(1, 2)
            """
          )
        );
    }

    @Test
    void commaContinuationBrace() {
        rewriteRun(
          scala(
            """
            import a.{x}, b.{y}
            """
          )
        );
    }

    @Test
    void commaContinuationSimpleAndBrace() {
        rewriteRun(
          scala(
            """
            import a._, b.{x, y}
            """
          )
        );
    }

    @Test
    void commaContinuationBraceWithAlias() {
        rewriteRun(
          scala(
            """
            import a.{x => X}, b.{y as Y}
            """
          )
        );
    }

    @Test
    void importAfterStatement() {
        // Scala allows imports to appear after other top-level statements.
        rewriteRun(
          scala(
            """
            class A { val x = 1 }
            import scala.collection.mutable
            class B { val y = 2 }
            """
          )
        );
    }
}
