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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class Java25EmptyTypeDeclarationTest implements RewriteTest {

    // A lone ';' (valid Java empty type declaration) between imports and a class declaration
    // ends up in a Space.whitespace field after parsing, violating the whitespace-only invariant.
    // This is a known structural limitation: J.CompilationUnit.classes only holds ClassDeclaration,
    // so there is nowhere to attach a J.Empty for the top-level ';'.
    // The class structure and print idempotency are both correct; only the whitespace validator
    // is suppressed here.
    @Test
    void emptySemicolonBetweenImportsAndClassIsIdempotent() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().allowNonWhitespaceInWhitespace(true).build()),
          java(
            """
              package com.example;

              import org.junit.jupiter.api.Test;

              ;

              /**
               * A test class.
               *
               * @author Test Author
               */
              public class MyTest {

                @Test
                void myMethod() {
                  // do something
                }
              }
              """
          )
        );
    }
}
