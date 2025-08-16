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

class InterfaceImplementsSpacingTest implements RewriteTest {

    @Test
    void singleSpaceBeforeImplements() {
        rewriteRun(
            graphQl(
                """
                type User implements Node {
                  id: ID!
                  name: String!
                }
                """
            )
        );
    }

    @Test
    void multipleSpacesBeforeImplements() {
        rewriteRun(
            graphQl(
                """
                type User    implements Node {
                  id: ID!
                  name: String!
                }
                """
            )
        );
    }

    @Test
    void tabBeforeImplements() {
        rewriteRun(
            graphQl(
                """
                type User\timplements Node {
                  id: ID!
                  name: String!
                }
                """
            )
        );
    }

    @Test
    void newlineBeforeImplements() {
        rewriteRun(
            graphQl(
                """
                type User
                  implements Node {
                  id: ID!
                  name: String!
                }
                """
            )
        );
    }

    @Test
    void multipleInterfacesWithSpacing() {
        rewriteRun(
            graphQl(
                """
                type User   implements   Node   &   Timestamped {
                  id: ID!
                  name: String!
                  createdAt: DateTime!
                }
                """
            )
        );
    }

    @Test
    void interfaceImplementingInterface() {
        rewriteRun(
            graphQl(
                """
                interface Entity  implements Node {
                  id: ID!
                }
                """
            )
        );
    }

    @Test
    void extendTypeImplements() {
        rewriteRun(
            graphQl(
                """
                extend type User    implements    Timestamped {
                  updatedAt: DateTime!
                }
                """
            )
        );
    }

    @Test
    void extendInterfaceImplements() {
        rewriteRun(
            graphQl(
                """
                extend interface Entity   implements   Auditable {
                  auditLog: [AuditEntry!]!
                }
                """
            )
        );
    }

    @Test
    void ampersandSpacing() {
        rewriteRun(
            graphQl(
                """
                type User implements Node&Timestamped&Auditable {
                  id: ID!
                  name: String!
                }
                """
            )
        );
    }

    @Test
    void ampersandWithAsymmetricSpacing() {
        rewriteRun(
            graphQl(
                """
                type User implements Node &Timestamped&  Auditable {
                  id: ID!
                  name: String!
                }
                """
            )
        );
    }
}