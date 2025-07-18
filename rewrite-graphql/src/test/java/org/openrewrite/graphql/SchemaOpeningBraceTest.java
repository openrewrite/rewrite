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

class SchemaOpeningBraceTest implements RewriteTest {

    @Test
    void singleSpaceBeforeBrace() {
        rewriteRun(
            graphQl(
                """
                schema {
                  query: Query
                }
                """
            )
        );
    }

    @Test
    void multipleSpacesBeforeBrace() {
        rewriteRun(
            graphQl(
                """
                schema    {
                  query: Query
                }
                """
            )
        );
    }

    @Test
    void tabBeforeBrace() {
        rewriteRun(
            graphQl(
                """
                schema\t{
                  query: Query
                }
                """
            )
        );
    }

    @Test
    void noSpaceBeforeBrace() {
        rewriteRun(
            graphQl(
                """
                schema{
                  query: Query
                }
                """
            )
        );
    }

    @Test
    void schemaWithDirectivesAndSpacing() {
        rewriteRun(
            graphQl(
                """
                schema @auth @cache   {
                  query: Query
                }
                """
            )
        );
    }

    @Test
    void extendSchemaSpacing() {
        rewriteRun(
            graphQl(
                """
                extend schema {
                  mutation: Mutation
                }
                """
            )
        );
    }

    @Test
    void extendSchemaMultipleSpaces() {
        rewriteRun(
            graphQl(
                """
                extend   schema   {
                  mutation: Mutation
                }
                """
            )
        );
    }

    @Test
    void extendSchemaWithDirectives() {
        rewriteRun(
            graphQl(
                """
                extend schema @deprecated    {
                  subscription: Subscription
                }
                """
            )
        );
    }
}