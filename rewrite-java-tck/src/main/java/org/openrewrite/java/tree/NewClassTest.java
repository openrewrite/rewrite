/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class NewClassTest implements RewriteTest {

    @Test
    void anonymousInnerClass() {
        rewriteRun(
          java(
            """
              class A { static class B {} }
              class C {
                  A.B anonB = new A.B() {};
              }
              """
          )
        );
    }

    @Test
    void concreteInnerClass() {
        rewriteRun(
          java(
            """
              class A { static class B {} }
              class C {
                  A.B anonB = new A.B();
              }
              """
          )
        );
    }

    @Test
    void concreteClassWithParams() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  Object l = new ArrayList < String > ( 0 ) { };
              }
              """
          )
        );
    }

    @Test
    void rawType() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  List<String> l = new ArrayList < > ();
              }
              """
          )
        );
    }

    @Test
    void delegate() {
        rewriteRun(
          java(
            """
              class A {
                  private String name;
                  A() {
                    this("ABC");
                  }
                  A(String name) {
                    this.name = name;
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterizedTypeAttribution() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  List<String> l = new ArrayList<>();
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable l =
                  ((J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0)).getVariables().get(0);
                J.NewClass arrayList = (J.NewClass) l.getInitializer();
                JavaType.Parameterized javaType = (JavaType.Parameterized) arrayList.getType();
                assertThat(javaType.getType().getFullyQualifiedName()).isEqualTo("java.util.ArrayList");
                assertThat(javaType.getTypeParameters()).satisfiesExactly(
                  p -> assertThat(((JavaType.Class) p).getFullyQualifiedName()).isEqualTo("java.lang.String")
                );
            })
          )
        );
    }

    @Test
    @MinimumJava11
    void anonymousTypeAttribution() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  List<String> l = new ArrayList<>() {};
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations.NamedVariable l =
                  ((J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0)).getVariables().get(0);
                J.NewClass arrayList = (J.NewClass) l.getInitializer();
                JavaType.Class javaType = (JavaType.Class) arrayList.getType();
                JavaType.Parameterized arrayListType = (JavaType.Parameterized) javaType.getSupertype();
                assertThat(arrayListType.getType().getFullyQualifiedName()).isEqualTo("java.util.ArrayList");
                assertThat(arrayListType.getTypeParameters()).satisfiesExactly(
                  p -> assertThat(((JavaType.Class) p).getFullyQualifiedName()).isEqualTo("java.lang.String")
                );
            })
          )
        );
    }

    @Test
    void anonymousClass() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;
              
              class Test {
                  List<Integer> l = new ArrayList<Integer>() {
                      /** Javadoc */
                      @Override
                      public boolean isEmpty() {
                          return false;
                      }
                  };
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2367")
    @Test
    void anonymousClassWithMultipleVariableDeclarations() {
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.util.ArrayList;
              import java.util.Arrays;
              import java.util.Comparator;
              
              class Test {
                  void method() {
                      Arrays.sort(new ArrayList[]{new ArrayList<File>()}, new Comparator<Object>() {
                          long time1, time2;
                  
                          @Override
                          public int compare(Object o1, Object o2) {
                              time1 = ((File) o1).lastModified();
                              time2 = ((File) o2).lastModified();
                              return time1 > time2 ? 1 : 0;
                          }
                      });
                  }
              }
              """
          )
        );
    }
}
