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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.graphql.tree.Space;

import static org.junit.jupiter.api.Assertions.*;

class InlineCommentDebugTest {
    
    @Disabled("Parser issue: Inline comment preservation")
    @Test
    void debugInlineComment() {
        // First test that Space.format properly handles inline comments
        String testSpace = " # This is an inline comment\n  next line";
        Space parsed = Space.format(testSpace, 0, testSpace.indexOf("next"));
        System.out.println("=== Space Format Test ===");
        System.out.println("Input: '" + testSpace.substring(0, testSpace.indexOf("next")).replace("\n", "\\n") + "'");
        System.out.println("Whitespace: '" + parsed.getWhitespace().replace("\n", "\\n") + "'");
        System.out.println("Comments: " + parsed.getComments().size());
        parsed.getComments().forEach(comment -> {
            System.out.println("  comment: '" + comment.getText() + "'");
            System.out.println("  suffix: '" + comment.getSuffix().replace("\n", "\\n") + "'");
        });
        
        // Now test GraphQL parsing
        String source = """
            type User {
              id: ID! # This is an inline comment
              name: String
            }
            """.trim();
            
        GraphQlParser parser = GraphQlParser.builder().build();
        Object result = parser.parse(source).findFirst().orElse(null);
        
        if (result == null) {
            System.err.println("Failed to parse source");
            fail("Failed to parse GraphQL source");
        }
        
        assertTrue(result instanceof GraphQl.Document, "Should parse successfully");
        
        GraphQl.Document doc = (GraphQl.Document) result;
        
        // Navigate to the field definitions
        GraphQl.ObjectTypeDefinition objectType = (GraphQl.ObjectTypeDefinition) doc.getDefinitions().get(0);
        GraphQl.FieldDefinition idField = objectType.getFields().get(0);
        GraphQl.FieldDefinition nameField = objectType.getFields().get(1);
        
        System.out.println("=== Inline Comment Debug ===");
        System.out.println("Source: '" + source + "'");
        System.out.println("\nID field:");
        System.out.println("  name: '" + idField.getName().getValue() + "'");
        System.out.println("  prefix: '" + idField.getPrefix().getWhitespace() + "'");
        
        System.out.println("\nName field:");
        System.out.println("  name: '" + nameField.getName().getValue() + "'");
        System.out.println("  prefix whitespace: '" + nameField.getPrefix().getWhitespace() + "'");
        System.out.println("  prefix comments: " + nameField.getPrefix().getComments().size());
        if (!nameField.getPrefix().getComments().isEmpty()) {
            nameField.getPrefix().getComments().forEach(comment -> {
                System.out.println("    comment: '" + comment.getText() + "'");
                System.out.println("    suffix: '" + comment.getSuffix() + "'");
            });
        }
    }
}