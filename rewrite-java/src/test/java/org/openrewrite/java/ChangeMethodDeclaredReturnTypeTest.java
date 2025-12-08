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
package org.openrewrite.java;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ChangeMethodDeclaredReturnTypeTest implements RewriteTest {
    @Test
    @DisplayName("Change the return type of the method and import the FQN.")
    void shouldChangeReturnTypeAndAddMissingImportTest() {
        String methodPattern = "Foo bar(..)";
        String newFQNReturnType = "java.util.List<String>";

        rewriteRun(spec -> spec
            .recipe(new ChangeMethodDeclaredReturnType(methodPattern,newFQNReturnType))
            .expectedCyclesThatMakeChanges(1).cycles(1),
            // Remark: Even if we use autoformat, a space is still included before the return type modified !
            // To get the import added, it is needed to execute in a 2nd recipe the AddImport() visitor !
            //toRecipe(r -> new AddImport<>(newImport,null,false))))),
            java("""
                public class Foo {
                  Object bar() {
                      return null;
                  }
                }
                """, """
                  import java.util.List;
                
                  public class Foo {
                        java.util.List<String> bar() {
                          return null;
                      }
                  }
                """));
    }
}