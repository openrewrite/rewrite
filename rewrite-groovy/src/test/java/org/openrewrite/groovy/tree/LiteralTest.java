/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GroovyUnusedAssignment")
class LiteralTest implements RewriteTest {

    @SuppressWarnings("GroovyConstantConditional")
    @Test
    void insideParentheses() {
        rewriteRun(
          groovy("(1)"),
          groovy("((1))")
        );
    }

    @Test
    void singleQuoteString() {
        rewriteRun(
          groovy(
            """
              'hello'
              """
          )
        );
    }

    @Test
    void regexPatternSingleQuoteString() {
        rewriteRun(
          groovy(
            """
              ~"hello"
              """
          )
        );
    }

    @Test
    void doubleQuoteString() {
        rewriteRun(
          groovy(
            """
              "hello"
              """
          )
        );
    }

    @Test
    void regexPatternDoubleQuoteString() {
        rewriteRun(
          groovy(
            """
              ~"hello"
              """
          )
        );
    }

    @Test
    void nullValue() {
        rewriteRun(
          groovy("null")
        );
    }

    @Test
    void boxedInt() {
        rewriteRun(
          groovy("Integer a = 1")
        );
    }

    @Test
    void tripleQuotedString() {
        rewriteRun(
          groovy(
            """
              \"""
                  " Hi "
              \"""
              """
          )
        );
    }

    @Test
    void regexPatternQuotedString() {
        rewriteRun(
          groovy(
            """
              ~\"""
                  " Hi "
              \"""
              """
          )
        );
    }

    @Test
    void slashString() {
        rewriteRun(
          groovy(
            """
              /.*"foo".*/
              """
          )
        );
    }

    @Test
    void regexPatternSlashString() {
        rewriteRun(
          groovy(
            """
              ~/foo/
              """
          )
        );
    }

    @Test
    void escapedString() {
        rewriteRun(
          groovy(
            """
              "f\\"o\\\\\\"o"
              """
          )
        );
    }

    @Test
    void gString() {
        rewriteRun(
          groovy(
            """
              " uid: ${ UUID.randomUUID() } "
              """
          )
        );
    }

    @Test
    void regexPatternGString() {
        rewriteRun(
          groovy(
            """
              ~"${ UUID.randomUUID() }"
              """
          )
        );
    }

    @Test
    void gStringNoCurlyBraces() {
        rewriteRun(
          groovy(
            """
              def foo = 1
              def s = "foo: $foo"
              """
          )
        );
    }

    @Test
    void gStringMultiPropertyAccess() {
        rewriteRun(
          groovy(
                """
            "$System.env.BAR_BAZ"
            """
          )
        );
    }

    @Test
    void emptyGString() {
        rewriteRun(
          groovy(
                """
            "${}"
            """
          )
        );
    }

    @Test
    void nestedGString() {
        rewriteRun(
          groovy(
                """
            " ${ " ${ " " } " } "
            """
          )
        );
    }

    @Test
    void gStringInterpolateString() {
        rewriteRun(
          groovy(
                """
            " ${""}\\n${" "} "
            """
          )
        );
    }

    @Test
    void gStringInterpolationFollowedByForwardSlash() {
        rewriteRun(
          groovy(
            """
              String s = "${ARTIFACTORY_URL}/plugins-release"
              """
          )
        );
    }

    @Test
    void gStringWithSpace() {
        rewriteRun(
          groovy(
            """
              String s = "${ ARTIFACTORY_URL }"
              """
          )
        );
    }

    @Test
    void gStringWithEscapedDelimiter() {
        rewriteRun(
          groovy(
            """
              String s = "<a href=\\"$url\\">${displayName}</a>"
              """
          )
        );
    }

    @Test
    void gStringWithStringLiteralsWithParentheses() {
        rewriteRun(
          groovy(
            """
              "Hello ${from(":-)").via(''':-|(''').via(":-)").to(':-(')}!"
              """
          )
        );
    }

    @Test
    void mapLiteral() {
        rewriteRun(
          groovy(
            """
              def person = [ name: 'sam' , age: 9000 ]
              """
          )
        );
    }

    @Test
    void numericLiterals() {
        rewriteRun(
          groovy(
            """
              float a = 0.1
              def b = 0.10f
              def c = -0.10f
              double d = 1.0d
              double e = -1.0d
              long f = +1L
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2752")
    @Test
    void numericLiteralsWithUnderscores() {
        rewriteRun(
          groovy(
            """
              def l1 = 10_000L
              def l2 = 10_000l
              def i = 10_000
              def d1 = 10_000d
              def d2 = 10_000D
              def f1 = 10_000f
              def f2 = -10_000.0F
              """
          )
        );
    }

    @Test
    void numericLiteralWithSpacing() {
        rewriteRun(
          groovy(
            """
              def a = -           0.10
              def b = +           0.10
              """
          )
        );
    }

    @Test
    void literalValueAndTypeAgree() {
        rewriteRun(groovy(
            """
              def a = 1.8
              """,
            spec -> spec.beforeRecipe(cu -> {
                // Groovy AST represents 1.8 as a BigDecimal
                // Java AST would represent it as Double
                // Our AST could reasonably make either choice
                var initializer = requireNonNull((J.Literal) ((J.VariableDeclarations) cu.getStatements().getFirst())
                  .getVariables().getFirst().getInitializer());
                if (initializer.getType() == JavaType.Primitive.Double) {
                    assertThat(initializer.getValue()).isEqualTo(1.8);
                } else if (TypeUtils.isOfClassType(initializer.getType(), "java.math.BigDecimal")) {
                    assertThat(initializer.getValue()).isInstanceOf(BigDecimal.class);
                }
            })
          )
        );
    }

    @Test
    void multilineStringWithApostrophes() {
        rewriteRun(
          groovy(
            """
              def s = '''
                multiline
                string
                with apostrophes
              '''
              """
          )
        );
    }

    @Test
    void mapLiteralTrailingComma() {
        rewriteRun(
          groovy(
            """
              def a = [ foo : "bar" , ]
              """
          )
        );
    }

    @Test
    void gStringThatHasEmptyValueExpressionForUnknownReason() {
        rewriteRun(
          groovy(
            """
              def a = "${foo.bar}"
              def b = "${foo.bar}baz"
              """
          )
        );
    }

    @Test
    void escapeCharacters() {
        rewriteRun(
          groovy(
            """
            "\\\\\\\\n\\\\t"
            '\\\\n\\t'
            ///\\\\n\\t///
            """
          )
        );
    }

    @Test
    void differentiateEscapeFromLiteral() {
        rewriteRun(
          groovy(
            """
            '\t'
            '	'
            """
          )
        );
    }

    @Test
    void stringLiteralInParentheses() {
        rewriteRun(
          groovy(
            """
              def a = (       "-"  )
              """
          ),
          groovy(
            """
              def a = (("-"))
              """
          ),
          groovy(
            """
              from(":-)").via(''':-|(''').via(":-)").to(':-(')
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/5232")
    @Test
    void stringWithMultipleBackslashes() {
        rewriteRun(
          groovy(
            """
              "".replaceAll('\\\\', '/')
              "a\\b".replaceAll('\\\\', '/')
              """
          )
        );
    }
}
