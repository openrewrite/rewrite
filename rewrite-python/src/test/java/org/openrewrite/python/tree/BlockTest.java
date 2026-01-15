/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.tree;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class BlockTest implements RewriteTest {

    @Test
    void singleStatement() {
        rewriteRun(python(
          """
            def f():
                pass
            """
        ));
    }

    @Test
    void blockPrefix() {
        rewriteRun(python(
          """
            def f() :
                pass
            """
        ));
    }

    @Test
    void singleStatementWithDocstring() {
        rewriteRun(python(
          """
            def f():
                \"""a docstring\"""
                pass
            """
        ));
    }

    @Test
    void singleStatementWithTrailingComment() {
        rewriteRun(python(
          """
            def f():
                pass  # a comment about the line
            """
        ));
    }

    @Test
    void multiStatementWithBlankLines() {
        rewriteRun(python(
          """
            def f():
                pass
                
                pass
                
                pass
            """
        ));
    }

    @Test
    void multiStatementWithTrailingComments() {
        rewriteRun(python(
          """
            def f():
                pass  # a comment about the line
                pass  # a comment about the line
                pass  # a comment about the line
            """
        ));
    }

    @Test
    void multiStatementWithBetweenComments() {
        rewriteRun(python(
          """
            def f():
                pass
                # a comment on its own line
                # another comment on its own line
                pass
                # a comment on its own line
                # another comment on its own line
                pass
            """
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\n", "\n\n", "\n\n\n"})
    void deeplyNested(String eof) {
        rewriteRun(python(
          """
            def f1():
                def f2():
                    pass
                    
                def f3():
                    
                    def f4():
                        pass
                    
                    pass%s""".formatted(eof)
        ));
    }


    @ParameterizedTest
    @ValueSource(strings = {"", "\n", "\n\n", "\n\n\n"})
    void lineEndingLocations(String eof) {
        rewriteRun(
          python(
            """
              print(1) # a comment
              print(2)
              print(3)
                            
              print(4)  # a comment
              print(5)%s""".formatted(eof)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "for x in mylist: print(x)",
      "def f(x): x = x + 1; return x",
      "def f(x): x = x + 1 ; return x",
      "def f(x): x = x + 1; return x;",
      "def f(x): x = x + 1; return x ;",
    })
    void oneLineBlocks(@Language("py") String code) {
        rewriteRun(
          python(code)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      """
        for x in xs:
            pass
        """,
      """
        for x in xs:
            pass
        else:
            pass
        """,
      """
        while True:
            pass
        """,
      """
        with stuff() as x:
            pass
        """,
      """
        if True:
            pass
        """,
      """
        if True:
            pass
        else:
            pass
        """,
      """
        if True:
            pass
        elif False:
            pass
        else:
            pass
        """,
      """
        try:
            pass
        except:
            pass
        """,
      """
        try:
            pass
        except:
            pass
        else:
            pass
        """,
      """
        try:
            pass
        except:
            pass
        else:
            pass
        finally:
            pass
        """,
    })
    void nested(String block) {
        rewriteRun(
          python(
            """
              def f():
                  %s
              """.formatted(block.replace("\n", "\n    "))
          )
        );
    }
}
