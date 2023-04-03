/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceIsPresentWithIfPresentTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true))
          .recipe(new ReplaceIsPresentWithIfPresent());
    }

    @Test
    void doNothingIfIsPresentNotFound() {
        rewriteRun(
          java(
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNothingIfPresentPartOfElseIf() {
        rewriteRun(
          java(
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      boolean x=false;
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                      if(x){
                          System.out.println("hello");
                      }else if(o.isPresent()){
                          list.add(o.get());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNothingIfElsePartPresent() {
        rewriteRun(
          java(
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                      if(o.isPresent()){
                          list.add(o.get());
                      }else{
                          System.out.println("else part");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNothingIfNotLambdaCompatible() {
        rewriteRun(
          java(
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      int x;
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                      if(o.isPresent()){
                          x=o.get();
                          list.add(o.get());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void replace() {
        rewriteRun(
          java(
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                      if(o.isPresent()){
                          list.add(o.get());
                      }
                  }
              }
              """,
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                      o.ifPresent((obj) -> {
                          list.add(obj);
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceNestedIf() {
        rewriteRun(
          java(
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      boolean x = true;
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                      if(x){
                          if(o.isPresent()){
                              list.add(o.get());
                          }
                      }
                  }
              }
              """,
            """
              package com.foobar;
              import java.util.Optional;
              import java.util.ArrayList;
              import java.util.List;
              public class A {
                  void method() {
                      boolean x = true;
                      List<Integer> list = new ArrayList<>();
                      Optional<Integer> o = Optional.of(2);
                      if(x){
                          o.ifPresent((obj) -> {
                              list.add(obj);
                          });
                      }
                  }
              }
              """
          )
        );
    }
}
