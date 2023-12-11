package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceUUIDRandomIdTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceUUIDRandomId("com.youorg.Test;"));
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
