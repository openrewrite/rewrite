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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class WhitespaceTest implements RewriteTest {

    @Test
    void hashbang() {
        rewriteRun(python(
          """
            #!/usr/bin/env python3.6
            
            print(42)
            """
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "",
      " ",
      ";",
      " ;",
      "; ",
      "\n",
      " \n",
      "\n ",
      "# comment",
      " # comment",
      "# comment ",
    })
    void singleStatement(String ending) {
        rewriteRun(python("print(42)%s".formatted(ending)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      ";",
      " ;",
      "; ",
      "\n",
      " \n",
      "# comment\n",
      " # comment\n",
      "# comment \n",
    })
    void multiStatement(String ending) {
        rewriteRun(python("print(42)%sprint(2)".formatted(ending)));
    }

    @ParameterizedTest
    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @ValueSource(strings = {
      "",
      " ",
      ";",
      " ;",
      "; ",
      "\n",
      " \n",
      "\n ",
      "# comment",
      " # comment",
      "# comment ",
      "\n#comment\n",
      "\n  #comment\n",
    })
    void singleLineMultiStatement(String firstLineEnding) {
        rewriteRun(python(
          """
            print(42); print(43) ;print(44)%s
            print(42); print(43) ;print(44) ; 
            """.formatted(firstLineEnding)
        ));
    }

    @ParameterizedTest
    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @ValueSource(strings = {
      "",
      " ",
      ";",
      " ;",
      "; ",
      "\n",
      " \n",
      "\n ",
      "# comment",
      " # comment",
      "# comment ",
      "\n#comment\n",
      "\n #comment\n",
      "\n  #comment\n",
      "\n   #comment\n",
      "\n    #comment\n",
    })
    void singleLineMultiStatementInsideBlock(String firstLineEnding) {
        rewriteRun(python(
          """
            def foo():
                print(42); print(43) ;print(44)%s
                print(42); print(43) ;print(44) ; 
            """.formatted(firstLineEnding)
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "", "\n", "\n\n", "\n\n\n"
    })
    void eOF(String eofSpace) {
        rewriteRun(python(
          """
            print(1)
            print(2)
            print(3)%s""".formatted(eofSpace)
        ));
    }
}
