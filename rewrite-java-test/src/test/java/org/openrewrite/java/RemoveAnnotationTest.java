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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveAnnotationTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/861")
    @Test
    void removeLastAnnotationFromClassDeclaration() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@java.lang.Deprecated")),
          java(
            """
              @Deprecated
              interface Test {}
              """,
            """
              interface Test {}
              """
          )
        );
    }

    @Test
    void removeAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@java.lang.Deprecated")),
          java(
            """
              import java.util.List;

              @Deprecated
              public class Test {
                  @Deprecated
                  void test() {
                      @Deprecated int n;
                  }
              }
              """,
            """
              import java.util.List;

              public class Test {
                  void test() {
                      int n;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/697")
    @Test
    void preserveWhitespaceOnModifiers() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@java.lang.Deprecated")),
          java(
            """
              import java.util.List;

              @Deprecated
              public class Test {
                  @Deprecated
                  private final Integer value = 0;
              }
              """,
            """
              import java.util.List;

              public class Test {
                  private final Integer value = 0;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/728")
    @Test
    void multipleAnnotationsOnClass() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@java.lang.Deprecated")),
          java(
            """
              package org.b;
                          
              import java.lang.annotation.Target;
              import static java.lang.annotation.ElementType.*;
                          
              @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
              public @interface ThirdAnnotation {
              }
              """
          ),
          java(
            """
              import org.b.ThirdAnnotation;

              @Deprecated @SuppressWarnings("") @ThirdAnnotation
              public class PosOneWithModifier {
              }
                            
              @SuppressWarnings("") @Deprecated @ThirdAnnotation
              public class PosTwoWithModifier {
              }

              @SuppressWarnings("") @ThirdAnnotation @Deprecated
              public class PosThreeWithModifier {
              }
                            
              @Deprecated @SuppressWarnings("") @ThirdAnnotation
              class PosOneNoModifier {
              }
                            
              @SuppressWarnings("") @Deprecated @ThirdAnnotation
              class PosTwoNoModifier {
              }
                            
              @SuppressWarnings("") @ThirdAnnotation @Deprecated
              class PosThreeNoModifier {
              }
              """,
            """
              import org.b.ThirdAnnotation;

              @SuppressWarnings("") @ThirdAnnotation
              public class PosOneWithModifier {
              }
                            
              @SuppressWarnings("") @ThirdAnnotation
              public class PosTwoWithModifier {
              }
                            
              @SuppressWarnings("") @ThirdAnnotation
              public class PosThreeWithModifier {
              }
                            
              @SuppressWarnings("") @ThirdAnnotation
              class PosOneNoModifier {
              }
                            
              @SuppressWarnings("") @ThirdAnnotation
              class PosTwoNoModifier {
              }
                            
              @SuppressWarnings("") @ThirdAnnotation
              class PosThreeNoModifier {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/728")
    @Test
    void multipleAnnotationsOnMethod() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@java.lang.Deprecated")),
          java(
            """
              package org.b;
                          
              import java.lang.annotation.Target;
              import static java.lang.annotation.ElementType.*;
                          
              @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
              public @interface ThirdAnnotation {
              }
              """
          ),
          java(
            """
              import org.b.ThirdAnnotation;
                            
              public class RemoveAnnotation {
                            
                  private Integer intValue;
                  private Double doubleValue;
                  private Long longValue;
                  
                  // Pos 1 with modifier.
                  @Deprecated
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public RemoveAnnotation(Integer intValue) {
                      this.intValue = intValue;
                  }
                  
                  // Pos 2 with modifier.
                  @SuppressWarnings("")
                  @Deprecated
                  @ThirdAnnotation
                  public RemoveAnnotation(Double doubleValue) {
                      this.doubleValue = doubleValue;
                  }
                  
                  // Pos 3 with modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  @Deprecated
                  public RemoveAnnotation(Long longValue) {
                      this.longValue = longValue;
                  }
                  
                  // Pos 1 no modifier.
                  @Deprecated
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  RemoveAnnotation(Integer intValue, Double doubleValue) {
                      this.intValue = intValue;
                      this.doubleValue = doubleValue;
                  }
                  
                  // Pos 2 no modifier.
                  @SuppressWarnings("")
                  @Deprecated
                  @ThirdAnnotation
                  RemoveAnnotation(Double doubleValue, Long longValue) {
                      this.doubleValue = doubleValue;
                      this.longValue = longValue;
                  }
                  
                  // Pos 3 no modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  @Deprecated
                  RemoveAnnotation(Integer intValue, Double doubleValue, Long longValue) {
                      this.intValue = intValue;
                      this.doubleValue = doubleValue;
                      this.longValue = longValue;
                  }
                  
                  @Deprecated
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public void pos1WithModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @Deprecated
                  @ThirdAnnotation
                  public void pos2WithModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  @Deprecated
                  public void pos3WithModifier() {
                  }
                  
                  @Deprecated
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  void pos1NoModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @Deprecated
                  @ThirdAnnotation
                  void pos2NoModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  @Deprecated
                  void pos3NoModifier() {
                  }
              }
              """,
            """
              import org.b.ThirdAnnotation;
                            
              public class RemoveAnnotation {
                            
                  private Integer intValue;
                  private Double doubleValue;
                  private Long longValue;
                  
                  // Pos 1 with modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public RemoveAnnotation(Integer intValue) {
                      this.intValue = intValue;
                  }
                  
                  // Pos 2 with modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public RemoveAnnotation(Double doubleValue) {
                      this.doubleValue = doubleValue;
                  }
                  
                  // Pos 3 with modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public RemoveAnnotation(Long longValue) {
                      this.longValue = longValue;
                  }
                  
                  // Pos 1 no modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  RemoveAnnotation(Integer intValue, Double doubleValue) {
                      this.intValue = intValue;
                      this.doubleValue = doubleValue;
                  }
                  
                  // Pos 2 no modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  RemoveAnnotation(Double doubleValue, Long longValue) {
                      this.doubleValue = doubleValue;
                      this.longValue = longValue;
                  }
                  
                  // Pos 3 no modifier.
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  RemoveAnnotation(Integer intValue, Double doubleValue, Long longValue) {
                      this.intValue = intValue;
                      this.doubleValue = doubleValue;
                      this.longValue = longValue;
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public void pos1WithModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public void pos2WithModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public void pos3WithModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  void pos1NoModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  void pos2NoModifier() {
                  }
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  void pos3NoModifier() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/728")
    @Test
    void multipleAnnotationsOnVariable() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@java.lang.Deprecated")),
          java(
            """
              package org.b;
                            
              import java.lang.annotation.Target;
              import static java.lang.annotation.ElementType.*;
                            
              @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
              public @interface ThirdAnnotation {
              }
              """
          ),
          java(
            """
              import org.b.ThirdAnnotation;
                            
              public class RemoveAnnotation {
                            
                  @Deprecated
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public final Integer pos1WithModifiers;
                  
                  @SuppressWarnings("")
                  @Deprecated
                  @ThirdAnnotation
                  public final Integer pos2WithModifiers;
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  @Deprecated
                  public final Integer pos3WithModifiers;
                  
                  @Deprecated
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  Integer pos1NoModifiers;
                  
                  @SuppressWarnings("")
                  @Deprecated
                  @ThirdAnnotation
                  Integer pos2NoModifiers;
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  @Deprecated
                  Integer pos3NoModifiers;
              }
              """,
            """
              import org.b.ThirdAnnotation;
                            
              public class RemoveAnnotation {
                            
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public final Integer pos1WithModifiers;
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public final Integer pos2WithModifiers;
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  public final Integer pos3WithModifiers;
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  Integer pos1NoModifiers;
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  Integer pos2NoModifiers;
                  
                  @SuppressWarnings("")
                  @ThirdAnnotation
                  Integer pos3NoModifiers;
              }
              """
          )
        );
    }
}
