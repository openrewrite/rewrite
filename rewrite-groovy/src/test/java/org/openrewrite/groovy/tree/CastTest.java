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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"UnnecessaryQualifiedReference", "GroovyUnusedAssignment", "GrUnnecessarySemicolon"})
class CastTest implements RewriteTest {

    @Test
    void javaStyleCast() {
        rewriteRun(
          groovy(
            """
              String foo = ( String ) null
              Integer i = (/**/java.lang.Integer/*
              */)1;
              """
          )
        );
    }

    @Test
    void groovyStyleCast() {
        rewriteRun(
          groovy(
            """
              String foo = null as String
              String bar = foo
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/2885")
    @Test
    void arrayCast() {
        rewriteRun(
          groovy(
            """
              def foo(String[][] a) {
              }
              def r = [["foo"], ["bar"]] as String  [/**/][
               // comment
              ]
              foo(r)
              foo((String [ ] [ ] )r)
              """
          )
        );
    }

    @Test
    void groovyCastAndInvokeMethod() {
        rewriteRun(
          groovy(
            """
              ( "" as String ).toString()
              """
          )
        );
    }

    @Test
    void javaCastAndInvokeMethod() {
        rewriteRun(
          groovy(
            """
              ( (String) "" ).toString()
              """
          )
        );
    }
}
