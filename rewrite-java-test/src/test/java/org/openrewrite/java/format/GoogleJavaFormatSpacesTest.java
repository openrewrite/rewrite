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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;

import static org.openrewrite.java.Assertions.java;

class GoogleJavaFormatSpacesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        String googleJavaFormatStyle = """
          type: specs.openrewrite.org/v1beta/style
          name: org.openrewrite.java.GoogleJavaFormat
          """;
        spec.recipe(Environment.builder()
                .load(new YamlResourceLoader(
                        new ByteArrayInputStream(googleJavaFormatStyle.getBytes(StandardCharsets.UTF_8)),
                        URI.create("rewrite.yml"),
                        new Properties()))
                .build()
                .activateRecipes("org.openrewrite.java.format.AutoFormat"));
    }

    @DocumentExample
    @Test
    void spacesAfterKeywords() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      if(true) {
                      }
                      for(int i = 0; i < 10; i++) {
                      }
                      while(true) {
                      }
                      try{
                      } catch(Exception e) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      if (true) {
                      }
                      for (int i = 0; i < 10; i++) {
                      }
                      while (true) {
                      }
                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void spacesAroundBinaryOperators() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 5+3;
                      int b = 10-2;
                      int c = 2*4;
                      int d = 8/2;
                      boolean e = true&&false;
                      boolean f = true||false;
                      int g = 5==5 ? 1 : 0;
                      int h = a<<2;
                      int i = b>>1;
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 5 + 3;
                      int b = 10 - 2;
                      int c = 2 * 4;
                      int d = 8 / 2;
                      boolean e = true && false;
                      boolean f = true || false;
                      int g = 5 == 5 ? 1 : 0;
                      int h = a << 2;
                      int i = b >> 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void spacesBeforeKeywords() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      if (true) {
                      }else {
                      }

                      try {
                      }catch (Exception e) {
                      }finally {
                      }
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      if (true) {
                      } else {
                      }

                      try {
                      } catch (Exception e) {
                      } finally {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void spacesAroundTernaryOperator() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = true?1:0;
                      int b = false ?2: 3;
                      int c = true? 4 :5;
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = true ? 1 : 0;
                      int b = false ? 2 : 3;
                      int c = true ? 4 : 5;
                  }
              }
              """
          )
        );
    }

    @Test
    void spacesAfterCommas() {
        rewriteRun(
          java(
            """
              class Test {
                  void method(int a,int b,int c) {
                      call(1,2,3);
                      int[] array = {1,2,3};
                  }

                  void call(int x,int y,int z) {
                  }
              }
              """,
            """
              class Test {
                  void method(int a, int b, int c) {
                      call(1, 2, 3);
                      int[] array = {1, 2, 3};
                  }

                  void call(int x, int y, int z) {
                  }
              }
              """
          )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known issue: SpacesVisitor doesn't handle space after colon in enhanced for loops")
    void spacesInEnhancedForAndLambda() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.function.Function;

              class Test {
                  void method(List<String> items) {
                      for (String item:items) {
                      }

                      Function<String, Integer> fn = s->s.length();
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.function.Function;

              class Test {
                  void method(List<String> items) {
                      for (String item : items) {
                      }

                      Function<String, Integer> fn = s -> s.length();
                  }
              }
              """
          )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known issue: SpacesVisitor doesn't normalize spaces around dot operator")
    void noSpacesForMethodReferenceAndDot() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;

              class Test {
                  void method() {
                      Function<String, Integer> ref = String :: length;
                      String s = "test";
                      int len = s . length();
                  }
              }
              """,
            """
              import java.util.function.Function;

              class Test {
                  void method() {
                      Function<String, Integer> ref = String::length;
                      String s = "test";
                      int len = s.length();
                  }
              }
              """
          )
        );
    }

    @Test
    void spacesBeforeOpeningBrace() {
        rewriteRun(
          java(
            """
              class Test{
                  void method(){
                      if (true){
                      }

                      for (int i = 0; i < 10; i++){
                      }

                      while (true){
                      }

                      try{
                      } catch (Exception e){
                      }
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      if (true) {
                      }

                      for (int i = 0; i < 10; i++) {
                      }

                      while (true) {
                      }

                      try {
                      } catch (Exception e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noSpaceBeforeBraceInAnnotationAndArrayInit() {
        rewriteRun(
          java(
            """
              @interface MyAnnotation {
                  String[] value();
              }

              class Test {
                  @MyAnnotation({"a", "b"})
                  void method() {
                      String[][] x = {{"foo"}};
                  }
              }
              """
          )
        );
    }

    @Test
    void spacesInTypeBounds() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;

              class Test<T extends Number&Serializable> {
                  void method() {
                      try {
                      } catch (RuntimeException|IllegalArgumentException e) {
                      }
                  }
              }
              """,
            """
              import java.io.Serializable;

              class Test<T extends Number & Serializable> {
                  void method() {
                      try {
                      } catch (RuntimeException | IllegalArgumentException e) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void spacesAfterTypeCast() {
        rewriteRun(
          java(
            """
              class Test {
                  void method(Object obj) {
                      String s = (String)obj;
                      Integer i = (Integer)obj;
                  }
              }
              """,
            """
              class Test {
                  void method(Object obj) {
                      String s = (String) obj;
                      Integer i = (Integer) obj;
                  }
              }
              """
          )
        );
    }

    @Test
    void alreadyProperlyFormatted() {
        // This test verifies that properly formatted code remains unchanged
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.function.Function;

              class Test {
                  void method(int a, int b, int c) {
                      if (true) {
                          System.out.println("true");
                      } else {
                          System.out.println("false");
                      }

                      for (int i = 0; i < 10; i++) {
                          System.out.println(i);
                      }

                      while (true) {
                          break;
                      }

                      try {
                          String s = "test";
                          int len = s.length();
                      } catch (Exception e) {
                          e.printStackTrace();
                      } finally {
                          System.out.println("done");
                      }

                      int x = 5 + 3;
                      int y = 10 - 2;
                      int z = 2 * 4;
                      boolean flag = true && false;
                      int result = flag ? 1 : 0;

                      List<String> items = List.of("a", "b", "c");
                      for (String item : items) {
                          System.out.println(item);
                      }

                      Function<String, Integer> fn = s -> s.length();
                      Function<String, Integer> ref = String::length;

                      String str = (String) new Object();
                  }
              }
              """
          )
        );
    }
}