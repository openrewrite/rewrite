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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
public class UseForEachRemoveInsteadOfSetRemoveAllTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseForEachRemoveInsteadOfSetRemoveAll());
    }

    @Test
    void returnExpressionIgnored() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.Collections;
              import java.util.HashSet;
              import java.util.List;
              import java.util.Set;
                          
              class T {
                  static {
                      Set<String> s = new HashSet<>();
                      List<String> l = Collections.singletonList("a");
                      s.removeAll(l);
                  }
                  
                  Set<String> removeFromSet(Set<String> s, Collection<String> c) {
                      s.removeAll(c);
                      return s;
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.Collections;
              import java.util.HashSet;
              import java.util.List;
              import java.util.Set;
                          
              class T {
                  static {
                      Set<String> s = new HashSet<>();
                      List<String> l = Collections.singletonList("a");
                      l.forEach(s::remove);
                  }
                  
                  Set<String> removeFromSet(Set<String> s, Collection<String> c) {
                      c.forEach(s::remove);
                      return s;
                  }
              }
              """
          )
        );
    }

    @Test
    void usedInAnonymousClassOrLambda() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.Set;
                          
              class T {
                  MyThings howAboutAnonymousInitializer(Set<String> s, Collection<String> c) {
                      return new MyThings() {
                          @Override
                          public void removeOtherThings(Set<String> s, Collection<String> c) {
                              s.removeAll(c);
                          }
                      };
                  }
                  
                  MyThings howLambda(Set<String> s, Collection<String> c) {
                      return (s1, c1) -> s1.removeAll(c1);
                  }
                      
                  @FunctionalInterface
                  interface MyThings {
                      void removeOtherThings(Set<String> s, Collection<String> c);
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.Set;
                          
              class T {
                  MyThings howAboutAnonymousInitializer(Set<String> s, Collection<String> c) {
                      return new MyThings() {
                          @Override
                          public void removeOtherThings(Set<String> s, Collection<String> c) {
                              c.forEach(s::remove);
                          }
                      };
                  }
                  
                  MyThings howLambda(Set<String> s, Collection<String> c) {
                      return (s1, c1) -> c1.forEach(s1::remove);
                  }
                      
                  @FunctionalInterface
                  interface MyThings {
                      void removeOtherThings(Set<String> s, Collection<String> c);
                  }
              }
              """
          )
        );
    }

    @Test
    void returnExpressionIsReferenced() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.HashSet;
              import java.util.Set;
                          
              class T {
                          
                  Set<String> removeFromSet(Set<String> s, Collection<String> c) {
                      if (s.removeAll(c)) {
                          return s;
                      }
                      return new HashSet<>(c);
                  }
                  
                  Set<String> doSomething(Set<String> s, Collection<String> c) {
                      if (!c.isEmpty() && s.removeAll(c)) {
                          return s;
                      } else if (s.isEmpty()) {
                          return new HashSet<>(c);
                      }
                      return new HashSet<>();
                  }
                  
                  void workWithSet(Set<String> s, Collection<String> c) {
                      boolean removedALl = s.removeAll(c);
                  }
                  
                  String didItRemoveAnything(Set<String> s, Collection<String> c) {
                      String success = s.removeAll(c) ? "YES" : "NO";
                      return success;
                  }
                  
                  boolean returnsResult(Set<String> s, Collection<String> c) {
                      return s.removeAll(c);
                  }
              }
              """
          )
        );
    }
}
