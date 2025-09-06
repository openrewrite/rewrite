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

public class GraphQlParserComplexFeaturesTest implements RewriteTest {

    @Test
    void parseMultipleOperationsInDocument() {
        rewriteRun(
            graphQl(
                """
                query GetUser($id: ID!) {
                  user(id: $id) {
                    name
                  }
                }
                
                query GetUsers {
                  users {
                    id
                    name
                  }
                }
                
                mutation CreateUser($input: CreateUserInput!) {
                  createUser(input: $input) {
                    id
                  }
                }
                
                subscription UserUpdates {
                  userUpdated {
                    id
                    name
                  }
                }
                """
            )
        );
    }

    @Test
    void parseAnonymousAndNamedOperations() {
        rewriteRun(
            graphQl(
                """
                query {
                  anonymousQuery {
                    id
                  }
                }
                
                query NamedQuery {
                  namedQuery {
                    id
                  }
                }
                
                {
                  shorthandQuery {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseVariableDefaultValuesWithComplexTypes() {
        rewriteRun(
            graphQl(
                """
                query ComplexDefaults(
                  $string: String = "default"
                  $int: Int = 42
                  $float: Float = 3.14
                  $boolean: Boolean = true
                  $null: String = null
                  $enum: Role = USER
                  $list: [String!] = ["a", "b", "c"]
                  $object: UserInput = {
                    name: "Default User"
                    age: 25
                    tags: ["default", "user"]
                    address: {
                      street: "123 Main St"
                      city: "Default City"
                    }
                  }
                  $nestedList: [[Int!]!] = [[1, 2], [3, 4]]
                ) {
                  test {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Fragment variables are an experimental GraphQL feature not supported by the grammar")
    void parseFragmentWithVariables() {
        // Fragment variable definitions are an experimental feature proposed in:
        // https://github.com/graphql/graphql-spec/issues/204
        // The syntax "fragment Name($var: Type) on Type" is not part of the 
        // official GraphQL specification and is not supported by our grammar.
        // The grammar expects: fragment NAME 'on' typeCondition
        // but gets: fragment NAME '(' instead of 'on'
        rewriteRun(
            graphQl(
                """
                fragment UserFragment($includeEmail: Boolean!, $includePhone: Boolean!) on User {
                  id
                  name
                  email @include(if: $includeEmail)
                  phone @include(if: $includePhone)
                }
                
                query GetUser($id: ID!, $includeEmail: Boolean!, $includePhone: Boolean!) {
                  user(id: $id) {
                    ...UserFragment
                  }
                }
                """
            )
        );
    }

    @Test
    void parseTypeConditionsOnUnionsAndInterfaces() {
        rewriteRun(
            graphQl(
                """
                query SearchWithTypeConditions($term: String!) {
                  search(term: $term) {
                    __typename
                    ... on User {
                      name
                      email
                    }
                    ... on Post {
                      title
                      content
                      author {
                        name
                      }
                    }
                    ... on Comment {
                      text
                      parent {
                        ... on Post {
                          title
                        }
                        ... on Comment {
                          text
                        }
                      }
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseCircularFragmentReferences() {
        rewriteRun(
            graphQl(
                """
                fragment UserFragment on User {
                  id
                  name
                  posts {
                    ...PostFragment
                  }
                }
                
                fragment PostFragment on Post {
                  id
                  title
                  author {
                    ...UserFragment
                  }
                }
                
                query {
                  users {
                    ...UserFragment
                  }
                }
                """
            )
        );
    }

    @Test
    void parseComplexArgumentStructures() {
        rewriteRun(
            graphQl(
                """
                query ComplexArguments {
                  search(
                    filters: {
                      and: [
                        { field: "status", operator: EQUALS, value: "active" }
                        {
                          or: [
                            { field: "type", operator: IN, values: ["A", "B", "C"] }
                            { field: "score", operator: GREATER_THAN, value: "80" }
                          ]
                        }
                      ]
                      not: { field: "deleted", operator: EQUALS, value: "true" }
                    }
                    pagination: {
                      page: 1
                      size: 20
                      sort: [
                        { field: "createdAt", direction: DESC }
                        { field: "name", direction: ASC }
                      ]
                    }
                  ) {
                    items {
                      id
                    }
                    totalCount
                  }
                }
                """
            )
        );
    }

    @Test
    void parseMultipleFragmentSpreads() {
        rewriteRun(
            graphQl(
                """
                fragment BasicFields on User {
                  id
                  name
                }
                
                fragment ContactFields on User {
                  email
                  phone
                }
                
                fragment SocialFields on User {
                  twitter
                  github
                }
                
                query {
                  user {
                    ...BasicFields
                    ...ContactFields
                    ...SocialFields
                    posts {
                      id
                      title
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseNestedFragments() {
        rewriteRun(
            graphQl(
                """
                fragment AddressFields on Address {
                  street
                  city
                  country
                }
                
                fragment UserFields on User {
                  id
                  name
                  address {
                    ...AddressFields
                  }
                }
                
                fragment PostFields on Post {
                  id
                  title
                  author {
                    ...UserFields
                  }
                }
                
                query {
                  posts {
                    ...PostFields
                  }
                }
                """
            )
        );
    }

    @Test
    void parseDirectivesWithComplexArguments() {
        rewriteRun(
            graphQl(
                """
                type User @auth(
                  requires: {
                    roles: [ADMIN, MODERATOR]
                    permissions: ["read:users", "write:users"]
                    conditions: {
                      or: [
                        { field: "owner", equals: "$currentUser" }
                        { field: "public", equals: true }
                      ]
                    }
                  }
                ) {
                  id: ID!
                  name: String! @transform(
                    operations: [
                      { type: TRIM }
                      { type: CAPITALIZE }
                      { type: MAX_LENGTH, value: 50 }
                    ]
                  )
                }
                """
            )
        );
    }

    @Test
    void parseInterfaceImplementationChain() {
        rewriteRun(
            graphQl(
                """
                interface Node {
                  id: ID!
                }
                
                interface Timestamped {
                  createdAt: DateTime!
                  updatedAt: DateTime!
                }
                
                interface Authored {
                  author: User!
                }
                
                interface Publishable implements Node & Timestamped {
                  id: ID!
                  createdAt: DateTime!
                  updatedAt: DateTime!
                  publishedAt: DateTime
                  status: PublishStatus!
                }
                
                type Post implements Node & Timestamped & Authored & Publishable {
                  id: ID!
                  createdAt: DateTime!
                  updatedAt: DateTime!
                  author: User!
                  publishedAt: DateTime
                  status: PublishStatus!
                  title: String!
                  content: String!
                }
                """
            )
        );
    }

    @Test
    void parseVariableUsageInNestedStructures() {
        rewriteRun(
            graphQl(
                """
                query ComplexVariableUsage(
                  $userId: ID!
                  $postFilter: PostFilter!
                  $includeComments: Boolean!
                  $commentLimit: Int = 5
                ) {
                  user(id: $userId) {
                    posts(filter: $postFilter) {
                      id
                      title
                      comments(first: $commentLimit) @include(if: $includeComments) {
                        edges {
                          node {
                            id
                            text
                            replies(
                              filter: {
                                authorId: $userId
                                minLikes: $commentLimit
                              }
                            ) {
                              id
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseIntrospectionQuery() {
        rewriteRun(
            graphQl(
                """
                query IntrospectionQuery {
                  __schema {
                    queryType {
                      name
                    }
                    mutationType {
                      name
                    }
                    subscriptionType {
                      name
                    }
                    types {
                      ...FullType
                    }
                    directives {
                      name
                      description
                      locations
                      args {
                        ...InputValue
                      }
                    }
                  }
                }
                
                fragment FullType on __Type {
                  kind
                  name
                  description
                  fields(includeDeprecated: true) {
                    name
                    description
                    args {
                      ...InputValue
                    }
                    type {
                      ...TypeRef
                    }
                    isDeprecated
                    deprecationReason
                  }
                  inputFields {
                    ...InputValue
                  }
                  interfaces {
                    ...TypeRef
                  }
                  enumValues(includeDeprecated: true) {
                    name
                    description
                    isDeprecated
                    deprecationReason
                  }
                  possibleTypes {
                    ...TypeRef
                  }
                }
                
                fragment InputValue on __InputValue {
                  name
                  description
                  type {
                    ...TypeRef
                  }
                  defaultValue
                }
                
                fragment TypeRef on __Type {
                  kind
                  name
                  ofType {
                    kind
                    name
                    ofType {
                      kind
                      name
                      ofType {
                        kind
                        name
                        ofType {
                          kind
                          name
                          ofType {
                            kind
                            name
                            ofType {
                              kind
                              name
                              ofType {
                                kind
                                name
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseRelayConnectionPattern() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  users(
                    first: Int
                    after: String
                    last: Int
                    before: String
                    orderBy: UserOrderBy
                  ): UserConnection!
                }
                
                type UserConnection {
                  edges: [UserEdge!]!
                  pageInfo: PageInfo!
                  totalCount: Int!
                }
                
                type UserEdge {
                  node: User!
                  cursor: String!
                }
                
                type PageInfo {
                  hasNextPage: Boolean!
                  hasPreviousPage: Boolean!
                  startCursor: String
                  endCursor: String
                }
                
                type User implements Node {
                  id: ID!
                  name: String!
                  posts(
                    first: Int
                    after: String
                  ): PostConnection!
                }
                
                interface Node {
                  id: ID!
                }
                
                input UserOrderBy {
                  field: UserOrderField!
                  direction: OrderDirection!
                }
                
                enum UserOrderField {
                  NAME
                  CREATED_AT
                  UPDATED_AT
                }
                
                enum OrderDirection {
                  ASC
                  DESC
                }
                """
            )
        );
    }

    @Test
    void parseApolloFederationDirectives() {
        rewriteRun(
            graphQl(
                """
                extend schema
                  @link(url: "https://specs.apollo.dev/federation/v2.0", import: ["@key", "@shareable", "@external", "@requires", "@provides"])
                
                type User @key(fields: "id") @key(fields: "email") {
                  id: ID!
                  email: String! @shareable
                  name: String!
                  profile: Profile @provides(fields: "bio")
                }
                
                type Profile @key(fields: "userId") {
                  userId: ID!
                  bio: String @external
                  avatar: String @requires(fields: "bio")
                }
                
                type Product @key(fields: "id") @key(fields: "sku package") {
                  id: ID!
                  sku: String!
                  package: String!
                  name: String!
                  price: Money!
                }
                
                type Money {
                  amount: Float!
                  currency: String!
                }
                """
            )
        );
    }
}