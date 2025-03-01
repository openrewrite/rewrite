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
class MethodInvocationTest implements RewriteTest {

    @Test
    void methodInvocation() {
        rewriteRun(
          javaScript(
            """
              console . log ( "hello world" )
              """
          )
        );
    }

    @Test
    void statementParameter() {
        rewriteRun(
          javaScript(
            """
              export default function ( req ) {
                req . on ( 'data' , function ( ) {
                } ) ;
              }
              """
          )
        );
    }

    @ExpectedToFail("Requires CallExpression#typeArguments")
    @Test
    void typeArguments() {
        rewriteRun(
          javaScript(
            """
              class User {
              }
              
              function foo < T > () : User {
                  return new User() ;
              }
              
              const bar = foo<User>();
              """
          )
        );
    }

    @Test
    void parenthesizedSelect() {
        rewriteRun(
          javaScript(
            """
              class Foo {
                  constructor( x : any ) { }
              }
              
              ( ( ) => {
                  const headers = new Foo( { x : 1 } ) ;
              } ) ( ) ;
              """
          )
        );
    }
}
