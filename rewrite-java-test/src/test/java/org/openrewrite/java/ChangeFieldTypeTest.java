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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ChangeFieldTypeTest implements RewriteTest {

    @SuppressWarnings("rawtypes")
    @Test
    void changeFieldTypeDeclarative() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ChangeFieldType<>(
            JavaType.ShallowClass.build("java.util.List"),
            JavaType.ShallowClass.build("java.util.Collection")))),
          java(
            """
              import java.util.List;
              public class A {
                 List collection;
              }
              """,
            """
              import java.util.Collection;
                            
              public class A {
                 Collection collection;
              }
              """
          )
        );
    }
}
