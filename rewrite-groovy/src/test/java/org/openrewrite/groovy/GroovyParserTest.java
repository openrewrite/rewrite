/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class GroovyParserTest implements RewriteTest {

    @Test
    void groovyPackageDefinition() {
        rewriteRun(
          groovy(
              """
              package org.openrewrite.groovy
              
              class A {
                  static void main(String[] args) {
                     String name = "John"
                     println(name)
                  }
               }
              """
          ),
          groovy(
            """
              package org.openrewrite.groovy;
              
              class B {
                  static void main(String[] args) {
                     String name = "Doe"
                     println(name)
                  }
               }
              """
          ));
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4072")
    void groovySpecialCharacters() {
        rewriteRun(
          groovy(
              """
              package openrewrite.issues
              
              class MapIssue {
                  Map<String, Integer> mapTest() {
                      Map map0 = new HashMap()
                      Map map1 = [:]
                      Map<String, Object> map2 = [:]
                      Map<String, Object> map3 = [:] as Map<String, Object>
                      Map<String, Object> map4 = buildMap()
                      Map error0 = new HashMap<>()
                      Map error1 = [:] as Map<String, Object>
                      Map error2 = buildMap()
              
                      return map
                  }
              
                  Map<String, Object> buildMap() {
                      return [:] as Map<String, Object>
                  }
              }
              """
          ));
    }

}
