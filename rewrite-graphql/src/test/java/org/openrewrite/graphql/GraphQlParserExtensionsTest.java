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

public class GraphQlParserExtensionsTest implements RewriteTest {

    @Test
    void parseSchemaExtension() {
        rewriteRun(
            graphQl(
                """
                schema {
                  query: Query
                  mutation: Mutation
                }
                
                extend schema {
                  subscription: Subscription
                }
                
                extend schema @link(url: "https://example.com/spec", import: ["@custom"])
                """
            )
        );
    }

    @Test
    void parseScalarExtension() {
        rewriteRun(
            graphQl(
                """
                scalar Date
                
                extend scalar Date @specifiedBy(url: "https://tools.ietf.org/html/rfc3339")
                
                scalar JSON
                
                extend scalar JSON @deprecated(reason: "Use structured types instead")
                """
            )
        );
    }

    @Test
    void parseObjectTypeExtension() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                }
                
                extend type User {
                  email: String!
                  phone: String
                }
                
                extend type User @auth(requires: ADMIN) {
                  internalId: String!
                  createdAt: DateTime!
                }
                
                extend type User implements Node & Timestamped {
                  updatedAt: DateTime!
                }
                """
            )
        );
    }

    @Test
    void parseInterfaceExtension() {
        rewriteRun(
            graphQl(
                """
                interface Node {
                  id: ID!
                }
                
                extend interface Node {
                  version: Int!
                }
                
                extend interface Node @deprecated(reason: "Use Entity interface instead") {
                  deprecated: Boolean!
                }
                
                interface Entity {
                  uuid: String!
                }
                
                extend interface Entity implements Node {
                  id: ID!
                  version: Int!
                }
                """
            )
        );
    }

    @Test
    void parseUnionExtension() {
        rewriteRun(
            graphQl(
                """
                union SearchResult = User | Post
                
                extend union SearchResult = Comment
                
                extend union SearchResult @deprecated(reason: "Use SearchResultV2") = Tag | Category
                """
            )
        );
    }

    @Test
    void parseEnumExtension() {
        rewriteRun(
            graphQl(
                """
                enum Role {
                  USER
                  ADMIN
                }
                
                extend enum Role {
                  MODERATOR
                  GUEST
                }
                
                extend enum Role @auth(requires: SYSTEM) {
                  SYSTEM
                  SERVICE_ACCOUNT
                }
                """
            )
        );
    }

    @Test
    void parseInputObjectExtension() {
        rewriteRun(
            graphQl(
                """
                input UserFilter {
                  name: String
                  email: String
                }
                
                extend input UserFilter {
                  role: Role
                  isActive: Boolean
                }
                
                extend input UserFilter @validate {
                  createdAfter: DateTime
                  createdBefore: DateTime
                }
                """
            )
        );
    }

    @Test
    void parseMultipleExtensionsOfSameType() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  user(id: ID!): User
                }
                
                extend type Query {
                  users: [User!]!
                }
                
                extend type Query {
                  userByEmail(email: String!): User
                }
                
                extend type Query @cacheControl(maxAge: 300) {
                  cachedUsers: [User!]!
                }
                
                extend type Query {
                  searchUsers(term: String!): [User!]!
                  countUsers: Int!
                }
                """
            )
        );
    }

    @Test
    void parseExtensionWithComplexFields() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                }
                
                extend type User {
                  posts(
                    first: Int = 10
                    after: String
                    orderBy: PostOrderBy = { field: CREATED_AT, direction: DESC }
                    filter: PostFilter
                  ): PostConnection! @complexity(value: 10, multipliers: ["first"])
                  
                  stats: UserStats! @cacheControl(maxAge: 600, scope: PRIVATE)
                  
                  "User's activity history"
                  activityHistory(
                    startDate: DateTime!
                    endDate: DateTime!
                    groupBy: TimeUnit = DAY
                  ): [ActivityPoint!]! @auth(requires: [USER, ADMIN])
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("GraphQL spec does not support extending directives")
    void parseExtensionWithDirectiveDefinitions() {
        rewriteRun(
            graphQl(
                """
                directive @auth(requires: [Role!]!) on FIELD_DEFINITION | OBJECT
                
                extend directive @auth on INTERFACE | UNION
                
                directive @validate on INPUT_OBJECT | ARGUMENT_DEFINITION
                
                extend directive @validate on INPUT_FIELD_DEFINITION
                """
            )
        );
    }

    @Test
    void parseComplexSchemaWithMultipleExtensions() {
        rewriteRun(
            graphQl(
                """
                # Original schema
                type User {
                  id: ID!
                  name: String!
                }
                
                type Post {
                  id: ID!
                  title: String!
                }
                
                type Query {
                  me: User
                }
                
                # First extension phase - add basic fields
                extend type User {
                  email: String!
                  posts: [Post!]!
                }
                
                extend type Post {
                  author: User!
                  content: String!
                }
                
                extend type Query {
                  user(id: ID!): User
                  post(id: ID!): Post
                }
                
                # Second extension phase - add interfaces
                interface Node {
                  id: ID!
                }
                
                extend type User implements Node
                extend type Post implements Node
                
                # Third extension phase - add timestamps
                interface Timestamped {
                  createdAt: DateTime!
                  updatedAt: DateTime!
                }
                
                extend type User implements Timestamped {
                  createdAt: DateTime!
                  updatedAt: DateTime!
                }
                
                extend type Post implements Timestamped {
                  createdAt: DateTime!
                  updatedAt: DateTime!
                  publishedAt: DateTime
                }
                
                # Fourth extension phase - add directives and metadata
                extend type User @key(fields: "id") @key(fields: "email") {
                  metadata: JSON @deprecated(reason: "Use specific fields instead")
                }
                
                extend type Post @cacheControl(maxAge: 3600) {
                  views: Int!
                  likes: Int!
                }
                """
            )
        );
    }

    @Test
    void parseExtensionWithRepeatableDirectives() {
        rewriteRun(
            graphQl(
                """
                directive @tag(name: String!) repeatable on OBJECT | FIELD_DEFINITION
                
                type User {
                  id: ID!
                }
                
                extend type User @tag(name: "entity") @tag(name: "authenticated") @tag(name: "v2") {
                  tags: [String!]! @tag(name: "metadata") @tag(name: "searchable")
                }
                """
            )
        );
    }

    @Test
    void parseExtensionOrderDependencies() {
        rewriteRun(
            graphQl(
                """
                # Define base types
                type User {
                  id: ID!
                }
                
                # Extend with interface that doesn't exist yet
                extend type User implements Profile
                
                # Define the interface later
                interface Profile {
                  displayName: String!
                  avatar: String
                }
                
                # Extend the interface
                extend interface Profile {
                  bio: String
                }
                
                # Further extend the type with the interface fields
                extend type User {
                  displayName: String!
                  avatar: String
                  bio: String
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Multiline directive locations not yet supported - known idempotency issue")
    void parseAllDirectiveLocations() {
        rewriteRun(
            graphQl(
                """
                directive @example on 
                  | QUERY
                  | MUTATION
                  | SUBSCRIPTION
                  | FIELD
                  | FRAGMENT_DEFINITION
                  | FRAGMENT_SPREAD
                  | INLINE_FRAGMENT
                  | VARIABLE_DEFINITION
                  | SCHEMA
                  | SCALAR
                  | OBJECT
                  | FIELD_DEFINITION
                  | ARGUMENT_DEFINITION
                  | INTERFACE
                  | UNION
                  | ENUM
                  | ENUM_VALUE
                  | INPUT_OBJECT
                  | INPUT_FIELD_DEFINITION
                
                # Use directive on various locations
                schema @example {
                  query: Query
                }
                
                scalar Date @example
                
                type User @example {
                  id: ID! @example
                  name(format: String @example): String!
                }
                
                interface Node @example {
                  id: ID!
                }
                
                union Result @example = User | Post
                
                enum Role @example {
                  ADMIN @example
                  USER
                }
                
                input Filter @example {
                  field: String @example
                }
                
                query GetUser($id: ID! @example) @example {
                  user(id: $id) @example {
                    ...UserFragment @example
                    ... @example {
                      email
                    }
                  }
                }
                
                fragment UserFragment on User @example {
                  id
                  name
                }
                """
            )
        );
    }
}