package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress("CStyleArrayDeclaration")
interface ExplicitInitializationTest: JavaRecipeTest {
    override val recipe: Recipe?
        get() = ExplicitInitialization()

    @Test
    fun removeExplicitInitialization() = assertChanged(
        before = """
            class Test {
                private int a = 0;
                private long b = 0L;
                private short c = 0;
                private int d = 1;
                private long e = 2L;
                private int f;
                private char g = '\0';

                private boolean h = false;
                private boolean i = true;

                private Object j = new Object();
                private Object k = null;

                int l[] = null;
                int m[] = new int[0];
                
                private final Long n = null;
            }
        """,
        after = """
            class Test {
                private int a;
                private long b;
                private short c;
                private int d = 1;
                private long e = 2L;
                private int f;
                private char g;
            
                private boolean h;
                private boolean i = true;
            
                private Object j = new Object();
                private Object k;
            
                int l[];
                int m[] = new int[0];
                
                private final Long n = null;
            }
        """
    )
}
