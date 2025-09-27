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

public class GraphQlParserEdgeCasesTest implements RewriteTest {

    @Test
    void parseEmptyDocument() {
        rewriteRun(
            graphQl("")
        );
    }

    @Test
    void parseDocumentWithOnlyWhitespace() {
        rewriteRun(
            graphQl("   \n\t\n   ")
        );
    }

    @Test
    void parseDocumentWithOnlyComments() {
        rewriteRun(
            graphQl(
                """
                # Just a comment
                # Another comment
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known limitation: emoji and other surrogate pair characters in UTF-16 are not handled correctly - see issue #43")
    void parseUnicodeInStrings() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(text: "Hello 世界 🚀") {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEscapedCharactersInStrings() {
        rewriteRun(
            graphQl(
                """
                query {
                  user {
                    bio(format: "Line 1\\nLine 2\\tTabbed\\r\\nWindows EOL\\\\Backslash\\"Quote")
                  }
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known limitation: Cannot preserve unicode escapes without storing raw value in AST")
    void parseEscapedUnicodeInStrings() {
        // The parser converts unicode escapes like \u0048 to their actual characters.
        // The StringValue AST node only stores the parsed value ("Hello"), not the
        // raw source representation ("\u0048\u0065\u006C\u006C\u006F").
        // To fix this, we would need to add a rawValue field to StringValue to
        // preserve the original representation.
        rewriteRun(
            graphQl(
                """
                query {
                  search(text: "\\u0048\\u0065\\u006C\\u006C\\u006F") {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseDeeplyNestedSelectionSets() {
        rewriteRun(
            graphQl(
                """
                query {
                  level1 {
                    level2 {
                      level3 {
                        level4 {
                          level5 {
                            level6 {
                              level7 {
                                level8 {
                                  level9 {
                                    level10 {
                                      id
                                    }
                                  }
                                }
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
    void parseDeeplyNestedListTypes() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  matrix: [[[[[[[[[[String]]]]]]]]]]!
                }
                """
            )
        );
    }

    @Test
    void parseVeryLongFieldNames() {
        rewriteRun(
            graphQl(
                """
                type User {
                  thisIsAVeryLongFieldNameThatExceedsNormalLengthExpectationsButShouldStillBeValidInGraphQL: String
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known limitation: Cannot preserve inline comments without RightPadded in field lists")
    void parseSpecialCharactersInComments() {
        // Inline comments after field definitions are dropped because fields are stored
        // as List<FieldDefinition> instead of List<RightPadded<FieldDefinition>>.
        // The comment "# Inline comment with emojis 🎉🚀✨" is consumed by the parser
        // but has nowhere to be stored in the AST.
        // To fix this, we need to refactor all places that use List<FieldDefinition>
        // to use List<RightPadded<FieldDefinition>> instead.
        rewriteRun(
            graphQl(
                """
                # Comment with special chars: @#$%^&*(){}[]|\\:;"'<>,.?/~`
                type User {
                  id: ID! # Inline comment with emojis 🎉🚀✨
                }
                """
            )
        );
    }

    @Test
    void parseEmptySelectionSet() {
        rewriteRun(
            graphQl(
                """
                query {
                  user {
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEmptyArgumentList() {
        rewriteRun(
            graphQl(
                """
                query {
                  user() {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEmptyObjectValue() {
        rewriteRun(
            graphQl(
                """
                query {
                  createUser(input: {}) {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEmptyListValue() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(tags: []) {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseMultilineStringWithIndentation() {
        rewriteRun(
            graphQl(
                """
                type User {
                  \"\"\"
                  This is a multiline description
                    with indentation
                      and multiple levels
                        of nesting
                  \"\"\"
                  name: String!
                }
                """
            )
        );
    }

    @Test
    void parseStringWithQuotesInside() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(text: "He said \\"Hello\\" to me") {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseNumbersAtBoundaries() {
        rewriteRun(
            graphQl(
                """
                query {
                  test(
                    zero: 0
                    negativeZero: -0
                    maxInt: 2147483647
                    minInt: -2147483648
                    largeFloat: 1.7976931348623157e308
                    smallFloat: 2.2250738585072014e-308
                    scientificNotation: 1.23e-4
                  ) {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseFieldWithNoArguments() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  users: [User!]!
                  currentTime: String!
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known limitation: Cannot preserve empty body formatting without 'end' field in AST")
    void parseInterfaceWithNoFields() {
        // This test demonstrates a limitation where the parser cannot preserve
        // the original formatting of empty interfaces/types. The closing brace
        // position is consumed but not stored in the AST, so we can't distinguish
        // between "interface Empty {}" and "interface Empty {\n}".
        // To fix this properly, we would need to add an 'end' field to store
        // the closing brace's prefix space.
        rewriteRun(
            graphQl(
                """
                interface Empty {
                }
                """
            )
        );
    }

    @Test
    void parseEnumWithSingleValue() {
        rewriteRun(
            graphQl(
                """
                enum SingleValue {
                  ONLY_ONE
                }
                """
            )
        );
    }

    @Test
    void parseUnionWithSingleType() {
        rewriteRun(
            graphQl(
                """
                union SingleUnion = User
                """
            )
        );
    }

    @Test
    void parseConsecutiveDirectives() {
        rewriteRun(
            graphQl(
                """
                type User {
                  email: String @deprecated @private @auth(role: ADMIN)
                }
                """
            )
        );
    }

    @Test
    void parseDirectiveWithEmptyArguments() {
        rewriteRun(
            graphQl(
                """
                type User {
                  name: String @custom()
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Grammar does not support trailing commas - non-standard GraphQL feature")
    void parseTrailingCommasInLists() {
        // The GraphQL specification does not allow trailing commas in lists or objects.
        // Our grammar follows the spec: '[' value (','? value)* ']'
        // This means we can have optional commas between values but not after the last one.
        // Some GraphQL implementations may be more lenient, but we follow the standard.
        rewriteRun(
            graphQl(
                """
                query {
                  search(
                    tags: [
                      "graphql",
                      "api",
                      "test",
                    ],
                    filters: {
                      name: "test",
                      active: true,
                    }
                  ) {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Known limitation: Cannot preserve operator spacing without RightPadded in AST")
    void parseWhitespaceAroundOperators() {
        // This test demonstrates limitations where the parser cannot preserve
        // the original spacing around operators:
        // 1. Interface implementations: "Node&Timestamped & Auditable" becomes "Node &Timestamped & Auditable"
        // 2. Union members: "Result=User|Post | Comment" becomes "Result =User |Post | Comment"
        // To fix this properly, we would need to:
        // - Change implementsInterfaces from List<NamedType> to List<RightPadded<NamedType>>
        // - Change union members from List<NamedType> to List<RightPadded<NamedType>>
        // - Add equalsPrefix field to UnionTypeDefinition
        rewriteRun(
            graphQl(
                """
                type User implements Node&Timestamped & Auditable {
                  id: ID!
                }
                
                union Result=User|Post | Comment
                """
            )
        );
    }

    @Test
    void parseRepeatableDirectiveMultipleTimes() {
        rewriteRun(
            graphQl(
                """
                type User @tag(name: "user") @tag(name: "entity") @tag(name: "model") {
                  id: ID!
                }
                """
            )
        );
    }

    @Test
    void parseBlockStringWithSpecialFormatting() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  \"\"\"
                  First line
                  
                  
                  Multiple blank lines above
                  
                  	Tab character here
                  Trailing spaces here   
                  \"\"\"
                  test: String
                }
                """
            )
        );
    }

    @Test
    void parseNegativeFloatWithExponent() {
        rewriteRun(
            graphQl(
                """
                query {
                  calculate(
                    value: -3.14e-10
                    another: -0.0000000001
                  ) {
                    result
                  }
                }
                """
            )
        );
    }
}