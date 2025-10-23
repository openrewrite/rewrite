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
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.graphql.Assertions.graphQl;

public class ArgumentFormattingTest implements RewriteTest {


    @Test
    void preserveNewlineSeparatedArguments() {
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
    void preserveCommaSeparatedArguments() {
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

    @Test
    @org.junit.jupiter.api.Disabled("Requires AST changes to properly preserve trailing commas - see task #30")
    void preserveMixedFormatting() {
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
                """
            )
        );
    }
}