/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;

class ChangePackageInStringLiteralTest implements RewriteTest {

    @Test
    @DocumentExample
    void changePackageInLiteral() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("javax.type", "jakarta.type", true)),
          java(
            """
              class Test {
                  String ref = "javax.type.A";
                  String extendedRef = "there is a type reference here -> javax.type.A <- hopefully it only replaces that";
              }
              """,
            """
              class Test {
                  String ref = "jakarta.type.A";
                  String extendedRef = "there is a type reference here -> jakarta.type.A <- hopefully it only replaces that";
              }
              """
          )
        );
    }
}
