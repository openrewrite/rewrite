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

@SuppressWarnings("JSUnusedLocalSymbols")
class AnnotationTest implements RewriteTest {

    @Test
    void classDecorator() {
        rewriteRun(
          javaScript(
            """
              function enumerable ( value : boolean ) {
                  return function ( target : any ,
                          propertyKey : string,
                          descriptor : PropertyDescriptor ) {
                      descriptor . enumerable = value ;
                  } ;
              }
              @enumerable ( false )
              class Foo {
                  foo ( ) {
                      return "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void methodDecorator() {
        rewriteRun(
          javaScript(
            """
              function enumerable ( value : boolean ) {
                  return function ( target : any ,
                          propertyKey : string,
                          descriptor : PropertyDescriptor ) {
                      descriptor . enumerable = value ;
                  } ;
              }
              class Foo {
                  @enumerable ( false )
                  foo ( ) {
                      return "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void propertyDecorator() {
        rewriteRun(
          javaScript(
            """
              function enumerable ( value : boolean ) {
                  return function ( target : any ,
                          propertyKey : string ,
                          descriptor : PropertyDescriptor ) {
                      descriptor . enumerable = value ;
                  } ;
              }
              class Foo {
                  @enumerable ( false )
                  foo : String = "hello" ;
              }
              """
          )
        );
    }
}
