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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

@SuppressWarnings("JSUnusedLocalSymbols")
class TypeOperatorTest implements RewriteTest {

    @Test
    void delete() {
        rewriteRun(
          javaScript(
            """
              let foo = { bar : 'v1' , buz : 'v2' }
              delete foo . buz
              """
          )
        );
    }

    @Test
    void typeof() {
        rewriteRun(
          javaScript(
            """
              let s = "hello"
              let t = typeof s
              """
          )
        );
    }

    @Test
    void instanceofOp() {
        rewriteRun(
          javaScript(
            """
              let arr = [ 1, 2 ]
              let t = arr instanceof Array
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void extendsKeyword() {
        rewriteRun(
          javaScript(
            """
              type PartialPerson = { name?: string; age?: number };

              function merge<T extends object, U extends object>(obj1: T, obj2: U): T & U {
                  return { ...obj1, ...obj2 };
              }

              const merged = merge({ name: 'John' }, { age: 30 });
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void typeLiteralDelimiterSemicolon() {
        rewriteRun(
          javaScript("""
            type Person = {
                name: string ; // Semicolon as delimiter
                age: number };
            """
          )
        );
    }

    @Test
    void typeLiteralDelimiterComma() {
        rewriteRun(
          javaScript("""
            type Person = {
                name: string , // Comma as delimiter
                age: number };
            """
          )
        );
    }
}
