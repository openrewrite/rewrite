/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class RemoveRedundantTypeCastTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveRedundantTypeCast());
    }

    @Test
    void doNotChangeUpCast() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                  Object o = "";
                  String s = (String) o;
                  List<?> l;
                  String[] sArray = (String[]) l.toArray(new String.get(0));
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1784")
    @Test
    void objectToObjectArray() {
        rewriteRun(
          java(
            """
              class Test {
                  void method(Object array) {
                      Object[] o = (Object[]) array;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1783")
    @Test
    void parametersDoNotMatch() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
                            
              class Test {
                  Class<? extends Collection<String>> test = (Class<? extends Collection<String>>) get();

                  Class<?> get() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1739")
    @Test
    void doNotChangeGenericTypeCast() {
        rewriteRun(
          java(
            """
              import java.util.*;
              
              class Test {
                  public <T extends Collection<String>> T test() {
                      T t = (T) get();
                      return t;
                  }
                  public List<String> get() {
                      return List.of("a", "b", "c");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1647")
    @Test
    void redundantTypeCast() {
        rewriteRun(
          java(
            """
              class Test {
                  String s = (String) "";
                  String s2 = (String) method();

                  String method() {
                      return null;
                  }
              }
              """,
            """
              class Test {
                  String s = "";
                  String s2 = method();

                  String method() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1647")
    @Test
    void downCast() {
        rewriteRun(
          java(
            """
              class Test {
                  Object o = (String) "";
                  Object o2 = (String) method();

                  String method() {
                      return null;
                  }
              }
              """,
            """
              class Test {
                  Object o = "";
                  Object o2 = method();

                  String method() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void downCastParameterizedTypes() {
        rewriteRun(
          java(
            """
              import java.util.List;
                            
              class Test {
                  Object o = (List<String>) method();
                  Object o2 = (List<? extends String>) method();
                  Object o3 = (List<? super String>) method();

                  List<String> method() {
                      return null;
                  }
              }
              """,
            """
              import java.util.List;
                            
              class Test {
                  Object o = method();
                  Object o2 = method();
                  Object o3 = method();

                  List<String> method() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1647")
    @Test
    void downCastExtendedObject() {
        rewriteRun(
          java(
            """
              class Test {
              }
              """
          ),
          java(
            """
              class ExtendTest extends Test {
                  Test extendTest = (ExtendTest) new ExtendTest();
              }
              """,
            """
              class ExtendTest extends Test {
                  Test extendTest = new ExtendTest();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1647")
    @Test
    void downCastExtendedObjectArray() {
        rewriteRun(
          java(
            """
              class Test {
              }
              """
          ),
          java(
            """
              class ExtendTest extends Test {
                  Test[][] extendTestArray = (ExtendTest[][]) new ExtendTest[0][0];
              }
              """,
            """
              class ExtendTest extends Test {
                  Test[][] extendTestArray = new ExtendTest[0][0];
              }
              """
          )
        );
    }
}
