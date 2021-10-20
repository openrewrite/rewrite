/*
 * Copyright 2021 the original author or authors.
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