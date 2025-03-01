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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.junitpioneer.jupiter.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

@SuppressWarnings({"JSUnusedLocalSymbols", "JSUnresolvedVariable", "TypeScriptCheckImport", "TypeScriptUnresolvedVariable"})
class VariableDeclarationTest implements RewriteTest {

    @Test
    void let() {
        rewriteRun(
          javaScript(
            """
              let hello = "World" ;
              """
          )
        );
    }

    @Test
    void multiTypeLet() {
        rewriteRun(
          javaScript(
            """
              let stringWord : string | null ;
              """
          )
        );
    }

    @Test
    void constant() {
        rewriteRun(
          javaScript(
            """
              const hello = "World" ;
              """
          )
        );
    }

    @Test
    void var() {
        rewriteRun(
          javaScript(
            """
              var hello = "World" ;
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void multiTypeVariableDeclaration() {
        rewriteRun(
          javaScript(
            """
              let x : number , y : string ;
              """
          )
        );
    }

    @Test
    void declareModifier() {
        rewriteRun(
          javaScript(
            """
              declare const name ;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-javascript/issues/80")
    @Test
    void readOnlyModifier() {
        rewriteRun(
          javaScript(
            """
              interface SomeType {
                  readonly prop: string;
              }
              """
          )
        );
    }

    @Test
    void generic() {
        rewriteRun(
          javaScript(
            """
              var v : Array < string > = [ 'foo' , 'bar', 'buz' ] ;
              """
          )
        );
    }

    @Test
    void methodInvocationInitializer() {
        rewriteRun(
          javaScript(
            """
              import parseProtocol from './parseProtocol.js';
              const protocol = parseProtocol("");
              """
          )
        );
    }

    @Test
    void typeDeclaration() {
        rewriteRun(
          javaScript(
            """
              type Value = string | string[] | number | boolean | null;
              """
          )
        );
    }

    @Test
    void typeDeclarationWithParameters() {
        rewriteRun(
          javaScript(
            """
              import Foo from 'foo' ;
              type Other < T = unknown , D = any > = Foo < T , D > ;
              """
          )
        );
    }

    @Test
    void optionalProperty() {
        rewriteRun(
          javaScript(
            """
              import foo from 'foo' ;
              const config : foo . Bar = {
                  params : {
                      param : ( value : Record < string , any > , options ? : foo.Options ) => String ( value )
                  }
              }
              """
          )
        );
    }

    @Test
    void methodDeclarationInitializer() {
        rewriteRun(
          javaScript(
            """
              let a = function all(promises) {
                  return Promise.all(promises);
              };
              """
          )
        );
    }
}
