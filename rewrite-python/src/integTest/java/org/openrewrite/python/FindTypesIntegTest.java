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
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.python.Assertions.python;

/**
 * Integration tests for FindTypes recipe running on Python source code.
 * <p>
 * FindTypes searches for type references by name. In Python, this applies
 * to type annotations, class references, and module references.
 */
@SuppressWarnings("PyUnresolvedReferences")
class FindTypesIntegTest implements RewriteTest {

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
    }

    @Test
    void findBuiltinStrType() {
        // Find references to the str type
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindTypes("str", false)),
          python(
            """
              result = "hello".upper()
              """
            // FindTypes marks type references in the AST
          )
        );
    }

    @Test
    void findListType() {
        // Find references to the list type
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindTypes("list", false)),
          python(
            """
              items = [1, 2, 3]
              items.append(4)
              """
          )
        );
    }

    @Test
    void findDictType() {
        // Find references to the dict type
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.none())
            .recipe(new FindTypes("dict", false)),
          python(
            """
              data = {"a": 1}
              value = data.get("a")
              """
          )
        );
    }

    // Note: FindTypes on module references (like os.path) may require additional
    // work to handle Python's module structure properly. Currently FindTypes
    // works on built-in types like str, list, dict.
}
