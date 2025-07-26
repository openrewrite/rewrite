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

    @Issue("https://github.com/openrewrite/rewrite/issues/4072")
    @Test
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

    @Issue("https://github.com/openrewrite/rewrite/issues/5296")
    @Test
    void anonymousClassWithNestedGenericType() {
        rewriteRun(groovy("new ArrayList<Map<String, String>>() {}"));
    }

    @Test
    void deeplyNestedAnonymousGeneric() {
        rewriteRun(groovy("new HashMap<String, List<Map<Integer, String>>>() {}"));
    }

    @Test
    void rawAnonymousClassShouldNotGetGenerics() {
        rewriteRun(groovy("new ArrayList() {}"));
    }

    @Test
    void inferredGenericsWithDiamondOperator() {
        rewriteRun(groovy("new ArrayList<>() {}"));
    }

    @Test
    void nestedAnonymousWithGenerics() {
        rewriteRun(
          groovy(
            """
              new HashMap<String, List<String>>() {
                  void inner() {
                      new ArrayList<Map<Integer, String>>() {}
                  }
              }
              """
          )
        );
    }

    @Test
    void variableDeclarationWithArrayInstantiationAsTheLastStatement() {
        rewriteRun(
          groovy(
            """
            def arr = new String[2]
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4614")
    @Test
    void trailingCommaInMethodCall() {
        rewriteRun(
          groovy(
            """
              System.out.println("Hello World with no extra space",)
              System.out.println("Hello World with space before comma" ,)
              System.out.println("Hello World with space after comma", )
              System.out.println("Hello World with space before & after comma" , )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5730")
    @Test
    void innerOuter() {
        rewriteRun(
          groovy(
            """
              class C {
                def outer() {
                  f(I<Void>) {
                    innner() { }
                  }
                }
                def outerWithSpaces() {
                  g( I < Void > ) {
                    innner() { }
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void multipleTypeParams() {
        rewriteRun(
          groovy(
            """
              class C {
                def method() {
                  f(Map<String, Integer>) {
                    println "test"
                  }
                }
                def methodWithSpaces() {
                  f( Map < String , Integer > ) {
                    println "test"
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void parameterizedArgOnly() {
        rewriteRun(
          groovy(
            """
              class C {
                def method() {
                  f(I<String, Integer>)
                }
              }
              """
          )
        );
    }

    @Test
    void nestedClosureAfterGeneric() {
        rewriteRun(
          groovy(
            """
              class C {
                def method() {
                  f(I<Void>) {
                    first {
                      second { }
                    }
                  }
                }
              }
              """
          )
        );
    }

}
