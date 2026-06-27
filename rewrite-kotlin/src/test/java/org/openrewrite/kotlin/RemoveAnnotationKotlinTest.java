/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RemoveAnnotationKotlinTest implements RewriteTest {

    /**
     * {@code RemoveAnnotation} gates its visitor behind a {@code UsesType} precondition. File-scope
     * annotations live on {@code K.CompilationUnit.annotations}, so this verifies the precondition
     * still considers their type "used" and does not skip the file before the Kotlin removal path
     * (see {@link RemoveAnnotationKotlinMixin}) runs.
     */
    @Test
    void removesFileScopeAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@kotlin.Suppress")),
          kotlin(
            """
              @file:Suppress("unused")

              class A
              """,
            """
              class A
              """
          )
        );
    }
}
