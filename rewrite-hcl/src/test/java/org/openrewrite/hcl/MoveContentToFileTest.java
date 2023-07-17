/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.hcl;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class MoveContentToFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void newFile() {
        rewriteRun(
          spec -> spec.recipe(new MoveContentToFile("$.before", "from.tf", "to.tf")),
          hcl(
            """
              after {
              }
              before {
              }
              """,
            """
              after {
              }
              """,
            spec -> spec.path("from.tf")
          ),
          hcl(
            doesNotExist(),
            """
              before {
              }
              """,
            spec -> spec.path("to.tf")
          )
        );
    }
}
