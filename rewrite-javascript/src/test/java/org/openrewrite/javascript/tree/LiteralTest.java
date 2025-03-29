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

@SuppressWarnings({"JSUnusedLocalSymbols", "TypeScriptCheckImport", "TypeScriptUnresolvedFunction"})
class LiteralTest implements RewriteTest {

    @Test
    void stringLiteral() {
        rewriteRun(
          javaScript(
            """
              let hello = 'World' ;
              """
          )
        );
    }

    @Test
    void numericLiteral() {
        rewriteRun(
          javaScript(
            """
              let n = 0 ;
              """
          )
        );
    }

    @Test
    void intentionallyBadUnicodeCharacter() {
        rewriteRun(
          javaScript(
            """
              let s1 = "\\\\u{U1}"
              let s2 = "\\\\u1234"
              let s3 = "\\\\u{00AUF}"
              """
          )
        );
    }

    @Test
    void unmatchedSurrogatePair() {
        rewriteRun(
          javaScript(
            """
              let c1 = '\uD800'
              let c2 = '\uDfFf'
              """
          )
        );
    }

    @Test
    void unmatchedSurrogatePairInString() {
        rewriteRun(
          javaScript(
            """
              let s1 : String = "\uD800"
              let s2 : String = "\uDfFf"
              """
          )
        );
    }

    @Test
    void templateSingleSpan() {
        rewriteRun(
          javaScript(
            """
              function foo ( group : string ) {
                  console . log ( `${group}` )
              }
              """
          )
        );
    }

    @Test
    void whitespaceBetween() {
        rewriteRun(
          javaScript(
            """
              function foo ( group : string ) {
                  console . log ( `${ group }` )
              }
              """
          )
        );
    }

    @Test
    void templateSingleSpanWithHead() {
        rewriteRun(
          javaScript(
            """
              function foo ( group : string ) {
                  console . log ( `group: ${ group }` )
              }
              """
          )
        );
    }

    @Test
    void templateSingleSpanWithTail() {
        rewriteRun(
          javaScript(
            """
              function foo ( group : string ) {
                  console . log ( `group: ${ group } after` )
              }
              """
          )
        );
    }

    @Test
    void templateWithMiddleSpan() {
        rewriteRun(
          javaScript(
            """
              function foo ( group : string , version : string ) {
                  console . log ( `group: ${ group } version: ${ version } after` )
              }
              """
          )
        );
    }

    @Test
    void templateWithTag() {
        rewriteRun(
          javaScript(
            """
              function foo ( ) {
                  const c = ""
                  console . log ( colorize ( ) `[${c}]: ✓ OK` ) ;
              }
              """
          )
        );
    }

    @Test
    void templateWithNoSpans() {
        rewriteRun(
          javaScript(
            """
              function foo ( ) {
                  console . log ( `template` )
              }
              """
          )
        );
    }

    @Test
    void template() {
        rewriteRun(
          javaScript(
            """
              import utils from "../utils.js" ;
              const CRLF = '\\r\\n' ;
              class Foo {
                  constructor ( name , value ) {
                      const { escapeName } = this . constructor ;
                      const isStringValue = utils . isString ( value ) ;
                      let headers = `Content-Disposition: form-data; name=" ${ escapeName (name ) }" ${
                          ! isStringValue && value . name ? `; filename=" ${ escapeName ( value . name ) } "` : ''
                      } ${ CRLF }` ;
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleSpansWithRawTextBetween() {
        rewriteRun(
          javaScript(
            """
              const fn1 = ( a : number , b : number , c : number ) => `${a}-${b}-${c}` ;
              """
          )
        );
    }
}
