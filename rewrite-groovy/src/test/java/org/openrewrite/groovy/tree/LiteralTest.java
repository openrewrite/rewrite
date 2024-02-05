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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

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
    void string() {
        rewriteRun(
          groovy("'hello'")
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
    void gString() {
        rewriteRun(
          groovy(
            """
              "uid: ${ UUID.randomUUID() } "
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
          groovy("""
            "$System.env.BAR_BAZ"
            """)
        );
    }

    @Test
    void emptyGString() {
        rewriteRun(
          groovy("""
            "${}"
            """)
        );
    }

    @Test
    void nestedGString() {
        rewriteRun(
          groovy("""
            " ${ " ${ " " } " } "
            """)
        );
    }

    @Test
    void gStringInterpolateString() {
        rewriteRun(
          groovy("""
            " ${""}\\n${" "} "
            """)
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
              def b = 0.1f
              double c = 1.0d
              long d = 1L
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
                var initializer = requireNonNull((J.Literal) ((J.VariableDeclarations) cu.getStatements().get(0))
                  .getVariables().get(0).getInitializer());
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
    void emptyListLiteral() {
        rewriteRun(
          groovy(
            """
              def a = []
              def b = [   ]
              """
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
    void listLiteralTrailingComma() {
        rewriteRun(
          groovy(
            """
              def a = [ "foo" /* "foo" suffix */ , /* "]" prefix */ ]
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
              "\\\\n\\t"
              '\\\\n\\t'
              ///\\\\n\\t///
              
            """
          )
        );

    }
}
