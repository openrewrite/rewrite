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
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.python.Assertions.python;

/**
 * Integration tests for Java recipes (FindMethods, ChangeMethodName, etc.)
 * running on Python source code via the RPC bridge.
 * <p>
 * These tests verify that type attribution works end-to-end:
 * 1. Python parser extracts type information (via ty LSP)
 * 2. Type information is serialized through RPC
 * 3. Java MethodMatcher can match methods based on type patterns
 * <p>
 * Type attribution capabilities (with ty LSP):
 * <ul>
 *   <li>Direct literals: "hello".upper() → type is str</li>
 *   <li>Container literals: [1,2,3].append() → type is list</li>
 *   <li>Variables: items = []; items.append() → type inferred as list</li>
 *   <li>Chained calls: "hello".upper().split() → return type inferred as str</li>
 * </ul>
 */
@SuppressWarnings("PyUnresolvedReferences")
class FindMethodsIntegTest implements RewriteTest {

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
    }

    @Test
    void findStringMethodByType() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("str upper(..)", false)),
          python(
            """
              result = "hello".upper()
              """,
            """
              result = /*~~>*/"hello".upper()
              """
          )
        );
    }

    @Test
    void findStringSplitWithArg() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("str split(..)", false)),
          python(
            """
              parts = "hello world".split(" ")
              """,
            """
              parts = /*~~>*/"hello world".split(" ")
              """
          )
        );
    }

    @Test
    void findBuiltinFunction() {
        // Builtin functions like len() don't have a declaring type from literals,
        // so we use the wildcard pattern
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("*..* len(..)", false)),
          python(
            """
              size = len("hello")
              """,
            """
              size = /*~~>*/len("hello")
              """
          )
        );
    }

    @Test
    void findMultipleOccurrences() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("str split(..)", false)),
          python(
            """
              a = "hello".split()
              b = "world".split(",")
              """,
            """
              a = /*~~>*/"hello".split()
              b = /*~~>*/"world".split(",")
              """
          )
        );
    }

    @Test
    void noMatchWhenMethodNameDiffers() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("str nonexistent(..)", false)),
          python(
            """
              result = "hello".upper()
              """
            // No change expected
          )
        );
    }

    @Test
    void noMatchWhenTypeDiffers() {
        // Verify type matching works - list methods shouldn't match str pattern
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("str append(..)", false)),
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
    void findMethodOnChainedCall() {
        // Chained calls use ty LSP to infer return types
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("str split(..)", false)),
          python(
            """
              result = "hello".upper().split()
              """,
            """
              result = /*~~>*/"hello".upper().split()
              """
          )
        );
    }

    @Test
    void findPrint() {
        // print() is a builtin function without a declaring type
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("*..* print(..)", false)),
          python(
            """
              print("hello world")
              """,
            """
              /*~~>*/print("hello world")
              """
          )
        );
    }

    @Test
    void findListMethodOnLiteral() {
        // Type attribution works when calling methods directly on list literals
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("list append(..)", false)),
          python(
            """
              [1, 2, 3].append(4)
              """,
            """
              /*~~>*/[1, 2, 3].append(4)
              """
          )
        );
    }

    @Test
    void findListMethodOnVariable() {
        // Variable type tracking uses ty LSP for type inference
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("list append(..)", false)),
          python(
            """
              items = [1, 2, 3]
              items.append(4)
              """,
            """
              items = [1, 2, 3]
              /*~~>*/items.append(4)
              """
          )
        );
    }

    @Test
    void findDictMethodOnLiteral() {
        // Type attribution works when calling methods directly on dict literals
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("dict get(..)", false)),
          python(
            """
              value = {"a": 1}.get("a")
              """,
            """
              value = /*~~>*/{"a": 1}.get("a")
              """
          )
        );
    }

    @Test
    void findDictMethodOnVariable() {
        // Variable type tracking uses ty LSP for type inference
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("dict get(..)", false)),
          python(
            """
              data = {"a": 1}
              value = data.get("a")
              """,
            """
              data = {"a": 1}
              value = /*~~>*/data.get("a")
              """
          )
        );
    }

    @Test
    void findMethodOnSubmodule() {
        // Submodule paths like collections.abc should be preserved as the FQN
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindMethods("os.path join(..)", false)),
          python(
            """
              import os.path
              result = os.path.join("a", "b")
              """,
            """
              import os.path
              result = /*~~>*/os.path.join("a", "b")
              """
          )
        );
    }
}
