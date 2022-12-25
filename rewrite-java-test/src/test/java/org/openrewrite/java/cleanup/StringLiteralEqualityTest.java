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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({
  "ConstantConditions",
  "NewObjectEquality",
  "StatementWithEmptyBody",
  "LoopConditionNotUpdatedInsideLoop",
  "EqualsWithItself",
  "ResultOfMethodCallIgnored"
  , "StringEquality", "StringOperationCanBeSimplified"})
class StringLiteralEqualityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StringLiteralEquality());
    }

    @Test
    void stringLiteralEqualityReplacedWithEquals() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                  public String getString() {
                      return "stringy";
                  }

                  public void method(String str) {
                      if (str == "test") ;
                      if ("test" == str) ;
                      if ("test" == "test") ;
                      if ("test" == new String("test")) ;
                      if ("test" == getString());
                      boolean flag = (str == "test");
                      while ("test" == str) {
                      }
                  }
                  
                  public void findPeter(List<Friend> friends) {
                      friends.stream().filter(e -> e.name == "peter");
                  }
                  
                  class Friend {
                      String name;
                  }
              }
              """,
            """
              import java.util.List;
              class Test {
                  public String getString() {
                      return "stringy";
                  }

                  public void method(String str) {
                      if ("test".equals(str)) ;
                      if ("test".equals(str)) ;
                      if ("test".equals("test")) ;
                      if ("test".equals(new String("test"))) ;
                      if ("test".equals(getString()));
                      boolean flag = ("test".equals(str));
                      while ("test".equals(str)) {
                      }
                  }
                  
                  public void findPeter(List<Friend> friends) {
                      friends.stream().filter(e -> "peter".equals(e.name));
                  }
                  
                  class Friend {
                      String name;
                  }
              }
              """
          )
        );
    }

    @Test
    void stringLiteralEqualityReplacedWithNotEquals() {
        rewriteRun(
          java(
            """
              class Test {
                  public String getString() {
                      return "stringy";
                  }

                  public void method(String str) {
                      if (str != "test") ;
                      if ("test" != str) ;
                      if ("test" != "test") ;
                      if ("test" != new String("test")) ;
                      if ("test" != getString());
                      boolean flag = (str != "test");
                      while ("test" != str) {
                      }
                  }
              }
              """,
            """
              class Test {
                  public String getString() {
                      return "stringy";
                  }

                  public void method(String str) {
                      if (!"test".equals(str)) ;
                      if (!"test".equals(str)) ;
                      if (!"test".equals("test")) ;
                      if (!"test".equals(new String("test"))) ;
                      if (!"test".equals(getString()));
                      boolean flag = (!"test".equals(str));
                      while (!"test".equals(str)) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeNotNeeded() {
        rewriteRun(
          java(
            """
              class Test {
                  public String getString() {
                      return "stringy";
                  }

                  public void method(String str0, String str1) {
                      if (str0 == new String("str0")) ;
                      if (str1 != new String("str1")) ;
                      if (str0 == str1) ;
                      if (getString() == str0) ;
                      if (str1 != str0) ;
                      if (getString() != str1) ;
                  }
              }
              """
          )
        );
    }
}
