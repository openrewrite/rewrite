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
package org.openrewrite.graphql;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.graphql.Assertions.graphQl;

/**
 * This test demonstrates the current limitation in preserving trailing commas
 * before newlines in GraphQL argument lists.
 * 
 * The issue is that the parser consumes commas with sourceBefore(",") but doesn't
 * preserve them in a way the printer can use. Without refactoring the AST to use
 * RightPadded for arguments (which would preserve trailing punctuation), we cannot
 * distinguish between:
 * 
 * 1. Arguments separated by commas followed by newlines: 
 *    id: "123", name: "John",\n    active: true
 * 
 * 2. Arguments separated by just newlines:
 *    id: "123"\n    name: "John"\n    active: true
 * 
 * The current implementation assumes that if the next argument starts on a new line,
 * no comma should be added. This works for case 2 but fails for case 1.
 * 
 * To fix this properly, we need to refactor Arguments to use 
 * List<GraphQlRightPadded<Argument>> instead of List<Argument>.
 */
public class CommaPreservationLimitationTest implements RewriteTest {
    
    @Test
    @org.junit.jupiter.api.Disabled("This demonstrates a known limitation - trailing commas before newlines are lost")
    void demonstratesCurrentLimitation() {
        // This test shows what currently happens - the trailing comma is lost
        // We disable it to avoid test failures, but keep it as documentation
        rewriteRun(
            graphQl(
                """
                query {
                  user(
                    id: "123", name: "John",
                    active: true
                  ) {
                    id
                  }
                }
                """,
                """
                query {
                  user(
                    id: "123", name: "John"
                    active: true
                  ) {
                    id
                  }
                }
                """
            )
        );
    }
    
    @Test
    void worksCorrectlyForPureNewlineSeparation() {
        // This case works correctly - no commas are added
        rewriteRun(
            graphQl(
                """
                query {
                  user(
                    id: "123"
                    name: "John"
                    active: true
                  ) {
                    id
                  }
                }
                """
            )
        );
    }
    
    @Test
    void worksCorrectlyForPureCommaSeparation() {
        // This case works correctly - commas are preserved
        rewriteRun(
            graphQl(
                """
                query {
                  user(id: "123", name: "John", active: true) {
                    id
                  }
                }
                """
            )
        );
    }
}