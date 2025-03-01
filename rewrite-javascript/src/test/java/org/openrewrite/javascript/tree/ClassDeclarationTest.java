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
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

@SuppressWarnings({"JSUnusedLocalSymbols", "JSUnresolvedVariable"})
class ClassDeclarationTest implements RewriteTest {

    @Test
    void classDeclaration() {
        rewriteRun(
          javaScript(
            "class Foo { }"
          )
        );
    }

    @Test
    void abstractClass() {
        rewriteRun(
          javaScript(
            "abstract class Foo { }"
          )
        );
    }

    @Test
    void interfaceDeclaration() {
        rewriteRun(
          javaScript(
            "interface Foo { }"
          )
        );
    }

    @Test
    void withConstructor() {
        rewriteRun(
          javaScript(
            """
              class Foo {
                  private name : string ;
                  constructor ( theName : string ) {
                      this . name = theName ;
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterizedType() {
        rewriteRun(
          javaScript(
            """
              class Foo < T , S extends PT < S > & C > {
              }
              interface C {
              }
              interface PT < T > {
              }
              """
          )
        );
    }

    @Test
    void endOfFile() {
        rewriteRun(
          javaScript(
            """
              class Foo {
              }
                            
                            
                            
              """
          )
        );
    }
}
