/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeType;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.python;

/**
 * Integration tests for ChangeType recipe running on Python source code.
 * <p>
 * ChangeType renames fully qualified type references, which in Python
 * applies to module and class references.
 */
@SuppressWarnings("PyUnresolvedReferences")
class ChangeTypeIntegTest implements RewriteTest {

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
    }

    @Test
    void changeBuiltinType() {
        // a str value has the String primitive for its type, so the class is left as the owner of its methods
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeType("str", "text.Text", false)),
          python(
            """
              result = "hello".upper()
              """,
            """
              result = "hello".upper()
              """,
            // No visible change - type attribution is internal
            spec -> spec.afterRecipe(cu -> assertThat(cu.getTypesInUse().getUsedMethods())
              .extracting(method -> method.getDeclaringType().getFullyQualifiedName())
              .containsExactly("text.Text"))
          )
        );
    }

    // Note: ChangeType on module references (like os.path -> pathlib) requires
    // additional work to handle Python's module import structure properly.
    // Currently ChangeType works on type attribution but may not work on
    // all Python module patterns.
}
