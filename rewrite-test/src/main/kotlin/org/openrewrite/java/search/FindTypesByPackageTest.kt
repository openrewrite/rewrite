package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaRecipeTest

interface FindTypesByPackageTest : JavaRecipeTest {
    @Test
    fun findConcurrent() = assertChanged(
        recipe = FindTypesByPackage("java.util.concurrent"),
        before = """
            import java.util.Map;
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.atomic.AtomicBoolean;
            
            public class T {
                void doSomething(AtomicBoolean[] booleans) {
                    AtomicBoolean b = new AtomicBoolean(true);
                    Map<String, String> m = new ConcurrentHashMap<>();
                    m.put("a", "b");
                    booleans[1].set(true);
                }
            }
        """,
        after = """
            import java.util.Map;
            import java.util.concurrent./*~~>*/ConcurrentHashMap;
            import java.util.concurrent.atomic./*~~>*/AtomicBoolean;
            
            public class T {
                void doSomething(/*~~>*/AtomicBoolean[] booleans) {
                    /*~~>*/AtomicBoolean /*~~>*/b = new /*~~>*/AtomicBoolean(true);
                    Map<String, String> m = new /*~~>*/ConcurrentHashMap<>();
                    m.put("a", "b");
                    /*~~>*/booleans[1].set(true);
                }
            }
        """
    )

    @Test
    fun findAtomic() = assertChanged(
        recipe = FindTypesByPackage("java.util.concurrent.atomic"),
        before = """
            import java.util.Map;
            import java.util.List;
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.atomic.AtomicBoolean;
            
            public class T {
                Map<String, String> m = new ConcurrentHashMap<>();
                void doSomething(List<AtomicBoolean> booleans) {
                    booleans.forEach(b -> b.set(true));
                }
            }
        """,
        after = """
            import java.util.Map;
            import java.util.List;
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.atomic./*~~>*/AtomicBoolean;
            
            public class T {
                Map<String, String> m = new ConcurrentHashMap<>();
                void doSomething(List</*~~>*/AtomicBoolean> booleans) {
                    booleans.forEach(/*~~>*/b -> /*~~>*/b.set(true));
                }
            }
        """
    )
}