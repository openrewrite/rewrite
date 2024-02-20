/*
 * Copyright 2024 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GroovyResultOfObjectAllocationIgnored")
class ConstructorTest implements RewriteTest {

    @Test
    void inParens() {
        rewriteRun(
          groovy(
            """
              ( new String("foo") )
              """
          )
        );
    }

    @Test
    void anonymousClassDeclarationClosedOverVariable() {
        rewriteRun(
          groovy(
            """
              int i = 1
              new Object() {
                  int one() {
                      return i
                  }
              }
              """
          )
        );
    }

    @Test
    @Disabled("Fails to parse")
    void implicitPublic() {
        rewriteRun(
          groovy(
            """
              class T {
                  T(int a, int b, int c) {
                  }
              }
              """
          )
        );
    }

    @Test
    @Disabled("Fails to parse")
    void defaultConstructorArguments() {
        rewriteRun(
          groovy(
            """
              class T {
                  T(int a = 1) {
                  }
              }
              """
          )
        );
    }
}
