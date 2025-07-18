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
import org.openrewrite.graphql.tree.Space;
import org.openrewrite.graphql.tree.Comment;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.graphql.Assertions.graphQl;

/**
 * Test to demonstrate the issue where newlines after comments are being 
 * incorrectly assigned as whitespace to the next element instead of 
 * being the comment's suffix.
 */
class CommentNewlineIssueTest implements RewriteTest {
    
    @Test
    void commentNewlineShouldBeInCommentSuffix() {
        // This test demonstrates the issue: the newline after the comment
        // should be part of the comment's suffix, not the whitespace of
        // the next element (type definition)
        String source = "# This is a comment\ntype User";
        
        // Parse the GraphQL document directly
        GraphQlParser parser = GraphQlParser.builder().build();
        var parsed = parser.parse(source).findFirst().orElseThrow();
        
        if (parsed instanceof org.openrewrite.tree.ParseError) {
            org.openrewrite.tree.ParseError error = (org.openrewrite.tree.ParseError) parsed;
            // The erroneous document is what we want to inspect
            if (error.getErroneous() != null && error.getErroneous() instanceof GraphQl.Document) {
                parsed = error.getErroneous();
            } else {
                fail("Failed to parse GraphQL: " + error.getText());
            }
        }
        
        GraphQl.Document doc = (GraphQl.Document) parsed;
        
        // The document should have one definition (the type)
        assertEquals(1, doc.getDefinitions().size());
        
        // Get the type definition
        GraphQl.Definition typeDef = doc.getDefinitions().get(0);
        assertTrue(typeDef instanceof GraphQl.TypeDefinition);
        
        GraphQl.TypeDefinition typeDefinition = (GraphQl.TypeDefinition) typeDef;
        
        // Inspect the prefix of the type definition
        Space typePrefix = typeDefinition.getPrefix();
        
        // Print the document to see how it's reconstructed
        String printed = doc.printAll();
        
        // THE ISSUE: The newline should be in the comment's suffix,
        // but it's likely in the type definition's whitespace instead
        
        // Expected behavior:
        // - Type definition whitespace should be empty ""
        // - Comment should have suffix "\n"
        
        // Actual behavior (the bug):
        // - Type definition whitespace contains "\n"  
        // - Comment suffix is empty ""
        
        if (!typePrefix.getComments().isEmpty()) {
            Comment firstComment = typePrefix.getComments().get(0);
            
            // This assertion will likely fail, demonstrating the issue
            assertEquals("\n", firstComment.getSuffix(), 
                "Comment suffix should contain the newline");
            
            // This assertion will also likely fail
            assertEquals("", typePrefix.getWhitespace(), 
                "Type definition whitespace should be empty");
        } else {
            fail("Expected to find a comment in the type definition prefix");
        }
    }
    
    @Test 
    void testSimpleCommentWithSpace() {
        // Also test the Space.format method directly as in CommentDebugTest
        String input = "# This is a comment\ntype User";
        Space space = Space.format(input, 0, input.indexOf("type"));
        
        
        // The issue should be visible here too
        if (!space.getComments().isEmpty()) {
            Comment firstComment = space.getComments().get(0);
            assertEquals("\n", firstComment.getSuffix(), 
                "Space.format: Comment suffix should contain the newline");
            assertEquals("", space.getWhitespace(), 
                "Space.format: Whitespace should be empty when comment is present");
        }
    }
    
    private String escapeWhitespace(String s) {
        return s.replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace(" ", "·");
    }
}