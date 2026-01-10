/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class ChangeMethodNameTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeMethodName("print", "println", true));
    }

    @Disabled("Flaky")
    @DocumentExample
    @SuppressWarnings("PyUnresolvedReferences")
    @Test
    void renameMethod() {
        rewriteRun(
          spec -> spec.parser(PythonParser.builder().logCompilationWarningsAndErrors(true)),
          python(
            """
            class Foo:
                def foo(self) :
                    print("hello")
            """,
            """
            class Foo:
                def foo(self) :
                    println("hello")
            """
          )
        );
    }
}
