/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceUUIDRandomIdTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceUUIDRandomId("org.openrewrite.java.tree.J.Literal <constructor>(..)"));
    }

    @Test
    void replacesUUIDRandomId() {
        rewriteRun(
          java(
            """
            package com.youorg;
            
            import org.openrewrite.java.tree.J;
            import java.util.UUID;
            
            class Test {
                J.Literal literal = new J.Literal(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
            }
            """,
            """
                package com.youorg;
                
                import org.openrewrite.Tree;import org.openrewrite.java.tree.J;
                import java.util.UUID;
                
                class Test {
                    J.Literal literal = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
                }
                """)
        );
    }

    @Test
    void notReplaceUUIDRandomID() {
        rewriteRun(
          java(
            """
            package com.youorg;
            
            import java.util.UUID;
            
            class Test {
                UUID uuid = UUID.randomUUID();
            }
            """,
            """
                package com.youorg;
                
                import java.util.UUID;
                
                class Test {
                    UUID uuid = UUID.randomUUID();
                }
                """)
        );
    }
}
