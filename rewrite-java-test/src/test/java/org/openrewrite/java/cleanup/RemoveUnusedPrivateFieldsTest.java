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

class RemoveUnusedPrivateFieldsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveUnusedPrivateFields());
    }

    @Test
    void doNotRemoveSerialVersionUid() {
        rewriteRun(
          java(
            """
              public class Test implements java.io.Serializable {
                  private static final long serialVersionUID = 42L;
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveAnnotatedField() {
        rewriteRun(
          java(
            """
              public class Test {
                  @Deprecated
                  public String annotated;
              }
              """
          )
        );
    }

    @Test
    void doNotChangeFieldsOnClassWithNativeMethod() {
        rewriteRun(
          java(
            """
              public class Test {
                  public String notUsed;
                  public native void method();
              }
              """
          )
        );
    }

    @Test
    void notPrivateField() {
        rewriteRun(
          java(
            """
              public class Test {
                  public String notUsed;
              }
              """
          )
        );
    }

    @Test
    void fieldIsUsed() {
        rewriteRun(
          java(
            """
              public class Test {
                  private String value;
                  void method() {
                      String useValue = value;
                  }
              }
              """
          )
        );
    }

    @Test
    void usedInClassScope() {
        rewriteRun(
          java(
            """
              public class Test {
                  private String value = "";
                  private String useValue = method(value);
                  String method(String arg0) {
                      return arg0 + useValue;
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnusedPrivateField() {
        rewriteRun(
          java(
            """
              public class Test {
                  private String notUsed;
              }
              """,
            """
              public class Test {
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Test
    void nameIsShadowed() {
        rewriteRun(
          java(
            """
              public class Test {
                  private String value;
                  void method() {
                      String value = "name shadow";
                      String shadowedUse = value;
                  }
              }
              """,
            """
              public class Test {
                  void method() {
                      String value = "name shadow";
                      String shadowedUse = value;
                  }
              }
              """
          )
        );
    }

    @Test
    void onlyRemoveUnusedNamedVariable() {
        rewriteRun(
          java(
            """
              public class Test {
                  private String aOne, aTwo, aThree;
                  private String bOne, bTwo, bThree;
                  private String cOne, cTwo, cThree;
                  void method() {
                      String removeAOne = aTwo + aThree;
                      String removeBTwo = bOne + bThree;
                      String removeCThree = cOne + cTwo;
                  }
              }
              """,
            """
              public class Test {
                  private String aTwo, aThree;
                  private String bOne, bThree;
                  private String cOne, cTwo;
                  void method() {
                      String removeAOne = aTwo + aThree;
                      String removeBTwo = bOne + bThree;
                      String removeCThree = cOne + cTwo;
                  }
              }
              """
          )
        );
    }
}

