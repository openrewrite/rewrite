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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.ReorderMethodArguments;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;

import static org.openrewrite.python.Assertions.python;

/**
 * Integration tests for ReorderMethodArguments recipe running on Python source code.
 * <p>
 * ReorderMethodArguments changes the order of method arguments based on parameter names.
 */
@SuppressWarnings("PyUnresolvedReferences")
class ReorderMethodArgumentsIntegTest implements RewriteTest {

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(Path.of("/tmp/python-rpc.log"))
                .traceRpcMessages()
        );
    }

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
    }

    @Test
    void reorderStringSplitArguments() {
        // str.split() has signature: def split(sep, maxsplit) -> list[str]
        // This test reorders arguments from [sep, maxsplit] to [maxsplit, sep]
        // Note: We don't provide oldParameterNames so the recipe uses the
        // actual parameter names from the type system, avoiding infinite loops.
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ReorderMethodArguments("str split(..)",
              new String[]{"maxsplit", "sep"},  // new order
              null,  // let recipe use type's actual parameter names
              false, false)),
          python(
            """
              parts = "a-b-c".split("-", 1)
              """,
            """
              parts = "a-b-c".split(1, "-")
              """
          )
        );
    }

    @Test
    void noChangeWhenNoMatch() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ReorderMethodArguments("str nonexistent(..)",
              new String[]{"b", "a"},
              new String[]{"a", "b"},
              false, false)),
          python(
            """
              parts = "hello world".split(" ")
              """
            // No change expected
          )
        );
    }

    @Test
    void noChangeWhenTypeDiffers() {
        // list.append should not match str pattern
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ReorderMethodArguments("str append(..)",
              new String[]{"b", "a"},
              new String[]{"a", "b"},
              false, false)),
          python(
            """
              items = [1, 2, 3]
              items.append(4)
              """
            // No change expected
          )
        );
    }
}
