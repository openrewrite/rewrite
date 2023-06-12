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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ArraysAsListToImmutableRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ArraysAsListToImmutableRecipe());
    }

    @Test
    void wrapArrayListToBeUnmodifiableWhenStrings() {
        rewriteRun(
          java("""
              import java.util.Arrays;
              
              class A {
                 public static final List<String> entries = Arrays.asList("A", "B");
              }
              """,
            """
              import java.util.Arrays;
              import java.util.Collections;
              
              class A {
                 public static final List<String> entries = Collections.unmodifiableList(Arrays.asList("A", "B"));
              }
              """
          )
        );
    }

    @Test
    void wrapArrayListToBeUnmodifiableWhenNulls() {
        rewriteRun(
          java("""
              import java.util.Arrays;
              
              class A {
                 public static final List<String> entries = Arrays.asList("A", null);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.Collections;
              
              class A {
                 public static final List<String> entries = Collections.unmodifiableList(Arrays.asList("A", null));
              }
              """
          )
        );
    }


    @Test
    void wrapArrayListToBeUnmodifiableWhenIntegers() {
        rewriteRun(
          java("""
              import java.util.Arrays;
              
              class A {
                 public static final List<Integer> entries = Arrays.asList(1, 2);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.Collections;
              
              class A {
                 public static final List<Integer> entries = Collections.unmodifiableList(Arrays.asList(1, 2));
              }
              """
          )
        );
    }

    @Test
    void doNotWrapIfItIsAlreadyUnmodifiable() {
        rewriteRun(
          java("""
            import java.util.Arrays;
            import java.util.Collections;
            
            class A {
               public static final List<String> entries = Collections.unmodifiableList(Arrays.asList("A", "B"));
            }
            """
          )
        );
    }
}