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
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.python.Assertions.python;

/**
 * Integration tests for ChangeMethodName recipe running on Python source code.
 */
@SuppressWarnings("PyUnresolvedReferences")
class ChangeMethodNameIntegTest implements RewriteTest {

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
    }

    @Test
    void renameStringMethod() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeMethodName("str upper(..)", "toUpperCase", false, false)),
          python(
            """
              result = "hello".upper()
              """,
            """
              result = "hello".toUpperCase()
              """
          )
        );
    }

    @Test
    void renameListMethod() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeMethodName("list append(..)", "add", false, false)),
          python(
            """
              items = [1, 2, 3]
              items.append(4)
              """,
            """
              items = [1, 2, 3]
              items.add(4)
              """
          )
        );
    }

    @Test
    void renameBuiltinFunction() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeMethodName("*..* print(..)", "console_log", false, false)),
          python(
            """
              print("hello world")
              """,
            """
              console_log("hello world")
              """
          )
        );
    }

    @Test
    void renameMultipleOccurrences() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeMethodName("str split(..)", "tokenize", false, false)),
          python(
            """
              a = "hello".split()
              b = "world".split(",")
              """,
            """
              a = "hello".tokenize()
              b = "world".tokenize(",")
              """
          )
        );
    }

    @Test
    void noChangeWhenMethodNameDiffers() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeMethodName("str nonexistent(..)", "newName", false, false)),
          python(
            """
              result = "hello".upper()
              """
            // No change expected
          )
        );
    }

    @Test
    void noChangeWhenTypeDiffers() {
        // Verify type matching works - list methods shouldn't match str pattern
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeMethodName("str append(..)", "add", false, false)),
          python(
            """
              items = [1, 2, 3]
              items.append(4)
              """
            // No change expected - append is on list, not str
          )
        );
    }

    @Test
    void renameChainedMethod() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new ChangeMethodName("str split(..)", "tokenize", false, false)),
          python(
            """
              result = "hello".upper().split()
              """,
            """
              result = "hello".upper().tokenize()
              """
          )
        );
    }
}
