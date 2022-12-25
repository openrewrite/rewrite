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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeMethodAccessLevelTest implements RewriteTest {

    @Test
    void publicToPrivate() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A aMethod(..)", "private", null)),
          java(
            """
              package com.abc;
                      
              class A {
                  @SuppressWarnings("ALL") // comment
                  public void aMethod(String s) {
                  }
                      
                  // comment
                  @SuppressWarnings("ALL")
                  public void aMethod() {
                  }
                      
                  // comment
                  public void aMethod(Integer i) {
                  }
                      
                  public void aMethod(Double i) {
                  }
              }
              """,
            """
              package com.abc;
                            
              class A {
                  @SuppressWarnings("ALL") // comment
                  private void aMethod(String s) {
                  }
                            
                  // comment
                  @SuppressWarnings("ALL")
                  private void aMethod() {
                  }
                            
                  // comment
                  private void aMethod(Integer i) {
                  }
                            
                  private void aMethod(Double i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void packagePrivateToProtected() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected", null)),
          java(
            """
              package com.abc;
                      
              class A {
                  @SuppressWarnings("ALL") // comment
                  static void aMethod(String s) {
                  }
                      
                  // comment
                  @SuppressWarnings("ALL")
                  void aMethod() {
                  }
                      
                  // comment
                  void aMethod(Integer i) {
                  }
                      
                  void aMethod(Double i) {
                  }
              }
              """,
            """
              package com.abc;
                    
              class A {
                  @SuppressWarnings("ALL") // comment
                  protected static void aMethod(String s) {
                  }
                    
                  // comment
                  @SuppressWarnings("ALL")
                  protected void aMethod() {
                  }
                    
                  // comment
                  protected void aMethod(Integer i) {
                  }
                    
                  protected void aMethod(Double i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void publicToPackagePrivate() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A aMethod(..)", "package", null)),
          java(
            """
              package com.abc;
                      
              class A {
                  @SuppressWarnings("ALL") // comment
                  public void aMethod(String s) {
                  }
                      
                  // comment
                  @SuppressWarnings("ALL")
                  public void aMethod() {
                  }
                      
                  // comment
                  public void aMethod(Integer i) {
                  }
                      
                  public void aMethod(Double i) {
                  }
              }
              """,
            """
              package com.abc;
                    
              class A {
                  // comment
                  @SuppressWarnings("ALL")
                  void aMethod(String s) {
                  }
                    
                  // comment
                  @SuppressWarnings("ALL")
                  void aMethod() {
                  }
                    
                  // comment
                  void aMethod(Integer i) {
                  }
                    
                  void aMethod(Double i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void publicToPackagePrivateWildcard() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A *(..)", "package", null)),
          java(
            """
              package com.abc;
                      
              class A {
                  // comment
                  public A(Integer i) {
                  }
                      
                  @Deprecated // comment
                  public A(Float f) {
                  }
                      
                  @Deprecated // comment
                  public void aMethod(String s) {
                  }
                      
                  // comment
                  public void aMethod(Integer i) {
                  }
              }
              """,
            """
              package com.abc;
                    
              class A {
                  // comment
                  A(Integer i) {
                  }
                    
                  // comment
                  @Deprecated
                  A(Float f) {
                  }
                    
                  // comment
                  @Deprecated
                  void aMethod(String s) {
                  }
                    
                  // comment
                  void aMethod(Integer i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeExistingAccessLevel() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A aMethod(String)", "public", null)),
          java(
            """
              package com.abc;

              class A {
                  // comment
                  public void aMethod(String s) {
                  }
              }
              """
          )
        );
    }

    @Test
    void packagePrivateToProtectedWithOtherModifier() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected", null)),
          java(
            """
              package com.abc;

              class A {
                  // comment
                  @Deprecated
                  static void aMethod(Double d) {
                  }
              }
              """,
            """
              package com.abc;

              class A {
                  // comment
                  @Deprecated
                  protected static void aMethod(Double d) {
                  }
              }
              """
          )
        );
    }

    @Test
    void packagePrivateToProtectedWithConstructor() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A <constructor>(..)", "protected", null)),
          java(
            """
              package com.abc;

              class A {
                  A(Integer i) {
                  }

                  // comment
                  A() {
                  }
              }
              """,
            """
              package com.abc;

              class A {
                  protected A(Integer i) {
                  }

                  // comment
                  protected A() {
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1215")
    @SuppressWarnings("MethodMayBeStatic")
    void methodPatternExactMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected", null)),
          java(
            """
              package com.abc;

              class A {
                  public String aMethod() {
                      return "example_A";
                  }
              }

              class B extends A {
                  @Override
                  public String aMethod() {
                      return "example_B";
                  }
              }
              """,
            """
              package com.abc;

              class A {
                  protected String aMethod() {
                      return "example_A";
                  }
              }

              class B extends A {
                  @Override
                  public String aMethod() {
                      return "example_B";
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1215")
    @SuppressWarnings("MethodMayBeStatic")
    void matchOverrides() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected", true)),
          java(
            """
              package com.abc;

              class A {
                  public String aMethod() {
                      return "example_A";
                  }
              }

              class B extends A {
                  @Override
                  public String aMethod() {
                      return "example_B";
                  }
              }
              """,
            """
              package com.abc;

              class A {
                  protected String aMethod() {
                      return "example_A";
                  }
              }

              class B extends A {
                  @Override
                  protected String aMethod() {
                      return "example_B";
                  }
              }
              """
          )
        );
    }
}
