/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class ClassExpressionTest implements RewriteTest {

    @Test
    void classExpressions() {
        rewriteRun(
          groovy(
            """
              maven( List , List ) {
                  from(components.java)
              }
              """
          )
        );
    }

    @Test
    void innerClassExpression() {
        rewriteRun(
          groovy(
            """
              ProcessBuilder.Redirect.to(new File("wat"))
              """
          )
        );

    }

    @Test
    void innerClassViaImport() {
        rewriteRun(
          groovy(
            """
              import java.lang.ProcessBuilder.Redirect
              Redirect.to(new File("wat"))
              """
          )
        );
    }

    @Test
    void unqualified() {
        rewriteRun(
          groovy(
            """
              package foo
              
              interface MyEntity {
              }
              class Foo {
                  void setUp() {
                      e = MyEntity.class
                  }
              }
              """
          )
        );
    }

    @Test
    void qualified() {
        rewriteRun(
          groovy(
            """
              package foo
              
              interface MyEntity {
              }
              class Foo {
                  void setUp() {
                      e = foo.MyEntity.class
                  }
              }
              """
          )
        );
    }
}
