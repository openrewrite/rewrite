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
            
            import java.util.UUID;
            class Test {
                java.util.UUID uuid = UUID.randomUUID();
            }
            """,
            """
                package com.youorg;
                
                import java.util.UUID;
                class Test {
                    java.util.UUID uuid = Tree.randomId();
                }
        """)
        );
    }

    @Test
    void noRandomUUIDRandomId() {
        rewriteRun(
          java(
           """
           package com.youorg;
            
            import java.util.UUID;
            class Test {
                int number = 42;
                void print() {
                    System.out.println(number);
                }
            }
           """, """
           package com.youorg;
            
            import java.util.UUID;
            class Test {
                int number = 42;
                void print() {
                    System.out.println(number);
                }
            }
           """)
        );
    }

    @Test
    void noReferenceToRandomId() {
        rewriteRun(
          java(
            """
            package com.youorg;
            
            import java.util.UUID;
            class Test {
                java.util.UUID otherUuid = UUID.fromString("some-string");
            }
            """,
            """
            package com.youorg;
            
            import java.util.UUID;
            class Test {
                java.util.UUID otherUuid = UUID.fromString("some-string");
            }
            """
          )
        );
    }
}
