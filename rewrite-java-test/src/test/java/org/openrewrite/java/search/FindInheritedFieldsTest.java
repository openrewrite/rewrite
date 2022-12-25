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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindInheritedFieldsTest implements RewriteTest {

    @Test
    void findInheritedField() {
        rewriteRun(
          java(
            """
              import java.util.*;
              public class A {
                 protected List<?> list;
                 private Set<?> set;
              }
              """
          ),
          java(
            "public class B extends A { }",
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindInheritedFields.find(cu.getClasses().get(0), "java.util.List").stream()
                  .map(JavaType.Variable::getName)).containsExactly("list");
                // the Set field is not considered to be inherited because it is private
                assertThat(FindInheritedFields.find(cu.getClasses().get(0), "java.util.Set")).isEmpty();
            })
          )
        );
    }

    @Test
    void findArrayOfType() {
        rewriteRun(
          java(
            """
              public class A {
                 String[] s;
              }
              """
          ),
          java(
            "public class B extends A {}",
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindInheritedFields.find(cu.getClasses().get(0), "java.lang.String").stream()
                  .map(JavaType.Variable::getName)).containsExactly("s");
            })
          )
        );
    }
}
