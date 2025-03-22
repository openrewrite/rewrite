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

@SuppressWarnings({"JSLastCommaInArrayLiteral", "JSUnresolvedVariable", "JSUnusedLocalSymbols", "TypeScriptUnresolvedVariable"})
class TrailingCommaTest implements RewriteTest {

    @Test
    void onMethodParameter() {
        rewriteRun(
          javaScript(
            """
              console . log ( "hello world" , )
              """
          )
        );
    }

    @Test
    void onTuple() {
        rewriteRun(
          javaScript(
            """
              let tuple : [ number , boolean , ] = [ 1, true , ]
              """
          )
        );
    }

    @Test
    void onEnum() {
        rewriteRun(
          javaScript(
            """
              enum Foo {
                Bar , Buz ,
              }
              """
          )
        );
    }

    @Test
    void multiExport() {
        rewriteRun(
          javaScript(
            """
              export {
                  first ,
                  second ,
                  third ,
              }
              """
          )
        );
    }

    @Test
    void objectBindingDeclaration() {
        rewriteRun(
          javaScript(
            """
              const { o1 , o2 , o3 , } = "" ;
              """
          )
        );
    }
}
