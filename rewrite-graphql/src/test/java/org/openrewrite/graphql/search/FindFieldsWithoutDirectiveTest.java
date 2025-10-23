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
package org.openrewrite.graphql.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.graphql.Assertions.graphQl;

class FindFieldsWithoutDirectiveTest implements RewriteTest {

    @Test
    void findFieldsWithoutDeprecated() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("deprecated", null, null)),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String @deprecated(reason: "Use fullName")
                  email: String
                  age: Int
                }
                
                type Post {
                  id: ID!
                  title: String @deprecated
                  content: String
                }
                """,
                """
                type User {
                  ~~>id: ID!
                  name: String @deprecated(reason: "Use fullName")
                  ~~>email: String
                  ~~>age: Int
                }
                
                type Post {
                  ~~>id: ID!
                  title: String @deprecated
                  ~~>content: String
                }
                """
            )
        );
    }

    @Test
    void findFieldsWithoutAuth() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("auth", null, null)),
            graphQl(
                """
                type Query {
                  publicData: String
                  userData: User
                  adminData: Admin @auth(requires: "ADMIN")
                }
                
                type User {
                  id: ID!
                  email: String
                  profile: Profile @auth(requires: "USER")
                }
                
                type Profile {
                  bio: String
                }
                """,
                """
                type Query {
                  ~~>publicData: String
                  ~~>userData: User
                  adminData: Admin @auth(requires: "ADMIN")
                }
                
                type User {
                  ~~>id: ID!
                  ~~>email: String
                  profile: Profile @auth(requires: "USER")
                }
                
                type Profile {
                  ~~>bio: String
                }
                """
            )
        );
    }

    @Test
    void filterByTypeName() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("deprecated", ".*Query", null)),
            graphQl(
                """
                type Query {
                  user: User
                  posts: [Post]
                }
                
                type UserQuery {
                  findById(id: ID!): User
                  findByEmail(email: String): User @deprecated
                }
                
                type User {
                  id: ID!
                  name: String
                }
                """,
                """
                type Query {
                  ~~>user: User
                  ~~>posts: [Post]
                }
                
                type UserQuery {
                  ~~>findById(id: ID!): User
                  findByEmail(email: String): User @deprecated
                }
                
                type User {
                  id: ID!
                  name: String
                }
                """
            )
        );
    }

    @Test
    void filterByFieldName() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("deprecated", null, "get.*")),
            graphQl(
                """
                type Query {
                  getUser: User
                  getAllUsers: [User]
                  user: User
                  findUser: User
                }
                
                type User {
                  getName: String
                  name: String
                  email: String
                }
                """,
                """
                type Query {
                  ~~>getUser: User
                  ~~>getAllUsers: [User]
                  user: User
                  findUser: User
                }
                
                type User {
                  ~~>getName: String
                  name: String
                  email: String
                }
                """
            )
        );
    }

    @Test
    void filterByBothTypeAndFieldName() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("cost", "Query", "expensive.*")),
            graphQl(
                """
                type Query {
                  expensiveSearch(query: String): [Result]
                  expensiveAnalytics: Analytics
                  cheapQuery: String
                  expensiveCompute: Int @cost(complexity: 100)
                }
                
                type Mutation {
                  expensiveOperation: Boolean
                  cheapOperation: Boolean
                }
                """,
                """
                type Query {
                  ~~>expensiveSearch(query: String): [Result]
                  ~~>expensiveAnalytics: Analytics
                  cheapQuery: String
                  expensiveCompute: Int @cost(complexity: 100)
                }
                
                type Mutation {
                  expensiveOperation: Boolean
                  cheapOperation: Boolean
                }
                """
            )
        );
    }

    @Test
    void interfaceFields() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("deprecated", null, null)),
            graphQl(
                """
                interface Node {
                  id: ID!
                  createdAt: String @deprecated(reason: "Use createdTimestamp")
                }
                
                interface Entity {
                  name: String
                  description: String
                }
                """,
                """
                interface Node {
                  ~~>id: ID!
                  createdAt: String @deprecated(reason: "Use createdTimestamp")
                }
                
                interface Entity {
                  ~~>name: String
                  ~~>description: String
                }
                """
            )
        );
    }

    @Test
    void typeExtensions() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("experimental", null, null)),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String
                }
                
                extend type User {
                  newFeature: String
                  betaFeature: String @experimental
                }
                
                interface Node {
                  id: ID!
                }
                
                extend interface Node {
                  metadata: String
                  experimentalData: String @experimental
                }
                """,
                """
                type User {
                  ~~>id: ID!
                  ~~>name: String
                }
                
                extend type User {
                  ~~>newFeature: String
                  betaFeature: String @experimental
                }
                
                interface Node {
                  ~~>id: ID!
                }
                
                extend interface Node {
                  ~~>metadata: String
                  experimentalData: String @experimental
                }
                """
            )
        );
    }

    @Test
    void multipleDirectives() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("auth", null, null)),
            graphQl(
                """
                type User {
                  id: ID! @deprecated @cost(complexity: 1)
                  email: String @deprecated
                  profile: Profile @auth(requires: "USER") @cost(complexity: 10)
                  settings: Settings @cost(complexity: 5) @auth(requires: "USER")
                }
                """,
                """
                type User {
                  ~~>id: ID! @deprecated @cost(complexity: 1)
                  ~~>email: String @deprecated
                  profile: Profile @auth(requires: "USER") @cost(complexity: 10)
                  settings: Settings @cost(complexity: 5) @auth(requires: "USER")
                }
                """
            )
        );
    }

    @Test
    void noFieldsFound() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("required", null, null)),
            graphQl(
                """
                type User {
                  id: ID! @required
                  name: String @required
                  email: String @required
                }
                
                type Post {
                  id: ID! @required
                  title: String @required
                }
                """
            )
        );
    }

    @Test
    void emptyTypes() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("deprecated", null, null)),
            graphQl(
                """
                type EmptyType {}
                
                interface EmptyInterface {}
                
                type User {
                  id: ID!
                }
                """,
                """
                type EmptyType {}
                
                interface EmptyInterface {}
                
                type User {
                  ~~>id: ID!
                }
                """
            )
        );
    }

    @Test
    void caseSensitiveDirectiveName() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("Deprecated", null, null)),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String @deprecated
                  email: String @Deprecated
                }
                """,
                """
                type User {
                  ~~>id: ID!
                  ~~>name: String @deprecated
                  email: String @Deprecated
                }
                """
            )
        );
    }

    @Test
    void complexSchema() {
        rewriteRun(
            spec -> spec.recipe(new FindFieldsWithoutDirective("auth", null, ".*sensitive.*|.*private.*")),
            graphQl(
                """
                type Query {
                  publicData: String
                  sensitiveUserData: User
                  privateMessages: [Message]
                  publicMessages: [Message]
                }
                
                type User {
                  id: ID!
                  name: String
                  sensitiveInfo: String
                  privateEmail: String  
                  publicBio: String
                }
                
                type Message {
                  id: ID!
                  content: String
                  privateSender: User
                  publicTimestamp: String
                }
                """,
                """
                type Query {
                  publicData: String
                  ~~>sensitiveUserData: User
                  ~~>privateMessages: [Message]
                  publicMessages: [Message]
                }
                
                type User {
                  id: ID!
                  name: String
                  ~~>sensitiveInfo: String
                  ~~>privateEmail: String  
                  publicBio: String
                }
                
                type Message {
                  id: ID!
                  content: String
                  ~~>privateSender: User
                  publicTimestamp: String
                }
                """
            )
        );
    }
}