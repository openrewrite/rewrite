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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RenameTypeAliasTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RenameTypeAlias("OldAlias", "NewAlias", "Test"));
    }

    @DocumentExample
    @Test
    void declaration() {
        rewriteRun(
          kotlin(
            """
              class Test
              typealias OldAlias = Test
              """,
            """
              class Test
              typealias NewAlias = Test
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/119")
    @Test
    void doesNotMatchType() {
        rewriteRun(
          kotlin(
            """
              class Other
              typealias OldAlias = Other
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/119")
    @Test
    void differentAliasName() {
        rewriteRun(
          kotlin(
            """
              class Test
              typealias OtherAlias = Test
              """
          )
        );
    }

    @Test
    void aliasImport() {
        rewriteRun(
          spec -> spec.recipe(new RenameTypeAlias("OldAlias", "NewAlias", "foo.Test")),
          kotlin(
            """
              package foo
              class Test
              """
          ),
          kotlin(
            //language=none
            """
              import foo.Test as OldAlias

              val a : OldAlias = OldAlias()
              """,
            //language=none
            """
              import foo.Test as NewAlias

              val a : NewAlias = NewAlias()
              """
          )
        );
    }

    @Test
    void variableTypeExpression() {
        rewriteRun(
          kotlin(
            """
              class Test
              typealias OldAlias = Test
              val a : OldAlias = Test()
              """,
            """
              class Test
              typealias NewAlias = Test
              val a : NewAlias = Test()
              """
          )
        );
    }

    @Test
    void functionParameter() {
        rewriteRun(
          kotlin(
            """
              @file:Suppress("UNUSED_PARAMETER")
              class Test
              typealias OldAlias = Test
              fun method(a: OldAlias) {
              }
              """,
            """
              @file:Suppress("UNUSED_PARAMETER")
              class Test
              typealias NewAlias = Test
              fun method(a: NewAlias) {
              }
              """
          )
        );
    }

    @Test
    void parameterizedType() {
        rewriteRun(
          kotlin(
            """
              class Test<T>
              typealias OldAlias<T> = Test<T>
              val a: OldAlias<String> = Test()
              """,
            """
              class Test<T>
              typealias NewAlias<T> = Test<T>
              val a: NewAlias<String> = Test()
              """
          )
        );
    }
}
