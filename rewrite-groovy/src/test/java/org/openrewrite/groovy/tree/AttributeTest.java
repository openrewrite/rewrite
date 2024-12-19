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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.groovy.Assertions.srcTestGroovy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcTestJava;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessarySemicolon"})
class AttributeTest implements RewriteTest {

    @Language("groovy")
    private static final String SOME_USER = """
      class User {
        public final String name
        User(String name) { this.name = name}
      }
      """;


    @Test
    void attribute() {
        rewriteRun(
          srcTestGroovy(groovy(SOME_USER)),
          groovy(
            """
              new User("Bob").@name == 'Bob'
              """
          )
        );
    }

    @Test
    void attributeInClosure() {
        rewriteRun(
          srcTestGroovy(groovy(SOME_USER)),
          groovy("[new User('Bob')].find { it.@name == 'Bob' }")
        );
    }

    @Test
    void attributeWithParentheses() {
        rewriteRun(
          srcTestGroovy(groovy(SOME_USER)),
          groovy("(new User('Bob').@name) == 'Bob'")
        );
    }
}
