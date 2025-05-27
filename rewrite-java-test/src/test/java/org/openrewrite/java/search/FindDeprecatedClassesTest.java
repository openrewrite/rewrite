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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindDeprecatedClassesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
          """
            package org.old.types;
            @Deprecated public class D {}
            """,
          """
            package org.old.types;
            public class E extends D {}
            """
          )
        );
    }

    @DocumentExample
    @Test
    void findDeprecations() {
        rewriteRun(
          spec -> spec.recipe(new FindDeprecatedClasses("org.old..*", false, true)),
          java(
            """
              import org.old.types.D;
              class Test {
                  D d;
              }
              """,
            """
              import org.old.types.D;
              class Test {
                  /*~~>*/D d;
              }
              """
          )
        );
    }

    @Test
    void ignoreDeprecationsInDeprecatedMethod() {
        rewriteRun(
          spec -> spec.recipe(new FindDeprecatedClasses("org.old..*", false, true)),
          java(
            """
              import org.old.types.D;
              class Test {
                  @Deprecated
                  void test(int n) {
                      new D();
                  }
              }
              """
          )
        );
    }

    @Test
    void ignoreDeprecationsInDeprecatedClass() {
        rewriteRun(
          spec -> spec.recipe(new FindDeprecatedClasses("org.old..*", false, true)),
          java(
            """
              import org.old.types.D;
              @Deprecated
              class Test {
                  D d;
              }
              """
          )
        );
    }

    @Test
    void findTypesInheritingFromDeprecations() {
        rewriteRun(
          spec -> spec.recipe(new FindDeprecatedClasses("org.old..*", true, null)),
          java(
            """
              import org.old.types.D;
              class Test {
                  D d;
              }
              """,
            """
              import org.old.types.D;
              class Test {
                  /*~~>*/D d;
              }
              """
          )
        );
    }
}
