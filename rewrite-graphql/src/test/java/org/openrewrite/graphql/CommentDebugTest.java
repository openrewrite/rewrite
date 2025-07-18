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
import org.openrewrite.graphql.tree.Space;

import static org.junit.jupiter.api.Assertions.*;

class CommentDebugTest {
    
    @Test
    void testSpaceFormatWithComments() {
        // Test with a comment followed by a newline
        String input1 = "# This is a comment\ntype User";
        Space space1 = Space.format(input1, 0, input1.indexOf("type"));
        
        assertEquals("", space1.getWhitespace());
        assertEquals(1, space1.getComments().size());
        assertEquals("# This is a comment", space1.getComments().get(0).getText());
        assertEquals("\n", space1.getComments().get(0).getSuffix());
        
        // Test with inline comment
        String input2 = " # Inline comment";
        Space space2 = Space.format(input2);
        
        assertEquals(" ", space2.getWhitespace());
        assertEquals(1, space2.getComments().size());
        assertEquals("# Inline comment", space2.getComments().get(0).getText());
        assertEquals("", space2.getComments().get(0).getSuffix());
    }
}