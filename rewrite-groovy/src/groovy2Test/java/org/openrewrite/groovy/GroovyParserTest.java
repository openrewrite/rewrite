/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class GroovyParserTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/8958")
    @ParameterizedTest
    @ValueSource(strings = {
      "a[0,1]",
      "a[0,1][2]",
      "a[[0,1], [2,3]]",
      "a[(0),1]",
      "a[*xs]",
      "a[0]",
      "a[[0,1]]",
      "a[[*xs]]",
      "a[ /* first */ 0 /* before comma */, /* second */ 1 /* end */ ]",
      "commandLine ['cmd', '/c'] + azCmd",
      "commandLine(['cmd', '/c'] + azCmd)"
    })
    void multiIndexAccess(String source) {
        rewriteRun(
          groovy(source)
        );
    }

    @Test
    void groovyRuntimeIsVersion2() throws Exception {
        Class<?> groovySystem = Class.forName("groovy.lang.GroovySystem");
        var version = (String) groovySystem.getMethod("getVersion").invoke(null);
        assertThat(version).startsWith("2.");
    }

    @Test
    void shouldNotTreatDivisionAsDelimiter() {
        rewriteRun(
                groovy(
                        """
                        def x = (1 / 1) * 2
                        System.out.println("test")
                        """
                )
        );
    }

    @Test
    void shouldHandleUsingSlashyStringsWithDivision() {
        rewriteRun(
                groovy(
                        """
                        def x = (Integer.parseInt(/3/) / Integer.parseInt(/2/)) * Integer.parseInt(/5/)
                        System.out.println("test")
                        """
                )
        );
    }

    @Test
    void shouldBeAbleToParseParenthesisedConstructorCallExpressions() {
        rewriteRun(
                groovy(
                        """
                        (new BigDecimal(10))
                        """
                )
        );
    }

    @Test
    void shouldBeAbleToParseParenthesisedSlashyStringConstantExpressions() {
        rewriteRun(
                groovy(
                        """
                        ((/test/))
                        """
                )
        );
    }

    @Test
    void shouldBeAbleToParseParenthesisedQuotedConstantExpressions() {
        rewriteRun(
                groovy(
                        """
                        (("test"))
                        """
                )
        );
    }

    @Test
    void shouldBeAbleToParseParenthesisedIntegerConstantExpressions() {
        rewriteRun(
                groovy(
                        """
                        (100)
                        """
                )
        );
    }

    @Test
    void closuresBlocksAndMethodPointers() {
        rewriteRun(
          groovy(
            """
              [1, 2].each { println it }
              if (true) {
                  def f = this.&println
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8978")
    @Test
    void baseScriptDeclaration() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.BaseScript groovy.lang.Script base
              println 1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8979")
    @Test
    void fieldDeclarationInsideBlock() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field
              @Field def top = 0
              @Deprecated @Field def second = 1
              if (true) {
                  @Field def list = []
                  @Deprecated
                  @Field @SuppressWarnings("unused") String s = "s"
              }
              """
          )
        );
    }

    @Test
    void annotationsAfterModifiers() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field
              final @Field x = 1
              final @Deprecated String y = "y"
              public @SuppressWarnings("unused") final class A {
                  private @Deprecated t
                  public @Deprecated A() {}
                  public @Deprecated <T> T n(final @Deprecated q) { null }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8980")
    @Test
    void gStringInParentheses() {
        rewriteRun(
          groovy(
            """
              def t = 1
              def label = ("build.${t}")
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8981")
    @Test
    void statementsInsideGStringInterpolation() {
        rewriteRun(
          groovy(
            """
              def x = true
              def s = "${if (x) { return 'a' }; return ''}"
              """
          )
        );
    }

    @Test
    void shouldBeAbleToParseClassDeclaration() {
        rewriteRun(
                groovy(
                        """
                        class Foo {
                            String bar
                        }
                        """
                )
        );
    }

}
