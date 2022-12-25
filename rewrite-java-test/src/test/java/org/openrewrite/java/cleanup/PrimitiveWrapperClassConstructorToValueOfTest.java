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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({
  "UnnecessaryBoxing", "BooleanConstructorCall", "ConstantConditions",
  "StringOperationCanBeSimplified", "CachedNumberConstructorCall"
})
class PrimitiveWrapperClassConstructorToValueOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimitiveWrapperClassConstructorToValueOf());
    }

    @Test
    void integerValueOf() {
        rewriteRun(
          java(
            """
              class A {
                  Integer i = Integer.valueOf(1);
                  String hello = new String("Hello" + " world " + i);
                  Long l = 11L;
              }
              """
          )
        );
    }

    @Test
    void newClassToValueOf() {
        rewriteRun(
          java(
            """
              class A {
                  Boolean bool = new Boolean(true);
                  Byte b = new Byte("1");
                  Character c = new Character('c');
                  Double d = new Double(1.0);
                  Float f = new Float(1.1f);
                  Long l = new Long(1);
                  Short sh = new Short("12");
                  short s3 = 3;
                  Short sh3 = new Short(s3);
                  Integer i = new Integer(1);
              }
              """,
            """
              class A {
                  Boolean bool = Boolean.valueOf(true);
                  Byte b = Byte.valueOf("1");
                  Character c = Character.valueOf('c');
                  Double d = Double.valueOf(1.0);
                  Float f = Float.valueOf(1.1f);
                  Long l = Long.valueOf(1);
                  Short sh = Short.valueOf("12");
                  short s3 = 3;
                  Short sh3 = Short.valueOf(s3);
                  Integer i = Integer.valueOf(1);
              }
              """
          )
        );
    }

    @Test
    void newIntegerToValueOfValueRef() {
        rewriteRun(
          java(
            """
              class A {
                  boolean fls = true;
                  Boolean b2 = new Boolean(fls);
                  char ch = 'c';
                  Character c = new Character(ch);
                  double d1 = 1.1;
                  Double d = new Double(d1);
                  int k = 1;
                  Integer k2 = new Integer(k);
              }
              """,
            """
              class A {
                  boolean fls = true;
                  Boolean b2 = Boolean.valueOf(fls);
                  char ch = 'c';
                  Character c = Character.valueOf(ch);
                  double d1 = 1.1;
                  Double d = Double.valueOf(d1);
                  int k = 1;
                  Integer k2 = Integer.valueOf(k);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/901")
    @Test
    void templateIsNewClassArgumentForNewClass() {
        rewriteRun(
          java(
            """
              import java.util.Date;
              public class A {
                  public static void main(String[] args) {
                      Date d = new Date(new Long(0));
                      Long l = new Long(new Integer(0));
                  }
              }
              """,
            """
              import java.util.Date;
              public class A {
                  public static void main(String[] args) {
                      Date d = new Date(Long.valueOf(0));
                      Long l = Long.valueOf(Integer.valueOf(0));
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleToFloat() {
        rewriteRun(
          java(
            """
              class T {
                  Double d1 = Double.valueOf(1.0);
                  void makeFloats() {
                      Float f = new Float(2.0d);
                      Float f2 = new Float(getD());
                      Float f3 = new Float(d1);
                  }
                  Double getD() {
                      return Double.valueOf(2.0d);
                  }
              }
              """,
            """
              class T {
                  Double d1 = Double.valueOf(1.0);
                  void makeFloats() {
                      Float f = Float.valueOf("2.0");
                      Float f2 = Float.valueOf(getD().floatValue());
                      Float f3 = Float.valueOf(d1.floatValue());
                  }
                  Double getD() {
                      return Double.valueOf(2.0d);
                  }
              }
              """
          )
        );
    }

    @Test
    void withinEnum() {
        rewriteRun(
          java(
            """
              public enum Options {
              
                  JAR("instance.jar.file"),
                  JVM_ARGUMENTS("instance.vm.args"),
                  QUICKSTART_OPTIONS("instance.options"),
                  INSTALLATIONS("instance.installations"),
                  START_TIMEOUT("instance.timeout");
              
                  private String name;
              
                  Options(String name) {
                      this.name = name;
                  }
              
                  public String asString() {
                      return System.getProperty(name);
                  }
                  
                  public Integer asInteger(int defaultValue) {
                      String string  = asString();
              
                      if (string == null) {
                          return defaultValue;
                      }
              
                      return new Integer(asString());
                  }
              
              }
              """,
            """
              public enum Options {
              
                  JAR("instance.jar.file"),
                  JVM_ARGUMENTS("instance.vm.args"),
                  QUICKSTART_OPTIONS("instance.options"),
                  INSTALLATIONS("instance.installations"),
                  START_TIMEOUT("instance.timeout");
              
                  private String name;
              
                  Options(String name) {
                      this.name = name;
                  }
              
                  public String asString() {
                      return System.getProperty(name);
                  }
                  
                  public Integer asInteger(int defaultValue) {
                      String string  = asString();
              
                      if (string == null) {
                          return defaultValue;
                      }
              
                      return Integer.valueOf(asString());
                  }
              
              }
              """
          )
        );
    }
}
