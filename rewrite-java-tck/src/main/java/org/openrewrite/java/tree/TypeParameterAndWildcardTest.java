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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class TypeParameterAndWildcardTest implements RewriteTest {

    @Test
    void annotatedTypeParametersOnWildcardBounds() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              import java.util.List;
              interface B {}
              class A {
                  List<? extends @NotNull B> checks;
              }
              """
          )
        );
    }

    @Test
    void annotatedTypeParametersOnReturnTypeExpression() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              import java.util.List;
              interface B {}
              class A {
                  public List<
                      @NotNull(groups = Prioritized.P1.class)
                      @javax.validation.Valid
                      B> foo() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void extendsAndSuper() {
        rewriteRun(
          java(
            """
              import java.util.List;
              interface B {}
              interface C {}
              public class A {
                  public <P  extends B> void foo(List<P> out, List<? super C> in) {}
              }
              """
          )
        );
    }

    @Test
    void multipleExtends() {
        rewriteRun(
          java(
            """
              interface B {}
              interface C {}
              public class A< T extends  B & C > {}
              """
          )
        );
    }

    @Test
    void wildcardExtends() {
        rewriteRun(
          java(
            """
              import java.util.*;
              interface B {}
              public class A {
                  List< ?  extends  B > bs;
              }
              """
          )
        );
    }

    @Test
    void emptyWildcard() {
        rewriteRun(
          java(
            """
              import java.util.*;
              public class A {
                  List< ? > a;
              }
              """
          )
        );
    }
}
