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
import org.openrewrite.java.DeleteMethodArgument;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.python.Assertions.python;

/**
 * Integration tests for DeleteMethodArgument recipe running on Python source code.
 * <p>
 * DeleteMethodArgument removes an argument from method invocations at a specified index.
 */
@SuppressWarnings("PyUnresolvedReferences")
class DeleteMethodArgumentIntegTest implements RewriteTest {

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
    }

    @Test
    void deleteFirstArgumentFromStringSplit() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new DeleteMethodArgument("str split(..)", 0)),
          python(
            """
              parts = "hello world".split(" ")
              """,
            """
              parts = "hello world".split()
              """
          )
        );
    }

    @Test
    void deleteArgumentFromListAppend() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new DeleteMethodArgument("list append(..)", 0)),
          python(
            """
              items = [1, 2, 3]
              items.append(4)
              """,
            """
              items = [1, 2, 3]
              items.append()
              """
          )
        );
    }

    @Test
    void deleteSecondArgumentFromStringSplit() {
        // str.split has optional maxsplit as second argument
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new DeleteMethodArgument("str split(..)", 1)),
          python(
            """
              parts = "a-b-c".split("-", 1)
              """,
            """
              parts = "a-b-c".split("-")
              """
          )
        );
    }

    @Test
    void noChangeWhenNoMatch() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new DeleteMethodArgument("str nonexistent(..)", 0)),
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
            .recipe(new DeleteMethodArgument("str append(..)", 0)),
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
