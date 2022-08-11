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
package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface MaybeUsesImportTest : JavaRecipeTest {
    @Test
    fun usesType(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            MaybeUsesImport<ExecutionContext>("java.util.Collections")
        },
        before = """
            import java.io.File;
            import java.util.Collections;
            import java.util.Collections.Inner;
            import java.util.List;
            import static java.util.*;
            import static java.util.Collections.Inner.something;
            import static java.util.Collections.singleton;
            import static java.util.Collections.*;
            import java.util.Map;
            
            class Test {
            }
        """,
        after = """
            import java.io.File;
            /*~~>*/import java.util.Collections;
            /*~~>*/import java.util.Collections.Inner;
            import java.util.List;
            /*~~>*/import static java.util.*;
            /*~~>*/import static java.util.Collections.Inner.something;
            /*~~>*/import static java.util.Collections.singleton;
            /*~~>*/import static java.util.Collections.*;
            import java.util.Map;
            
            class Test {
            }
        """
    )

    /**
     * Type wildcards are greedy.
     */
    @Test
    fun usesTypeWildcard(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            MaybeUsesImport<ExecutionContext>("java.util.*")
        },
        before = """
            import java.io.File;
            import java.util.Collections;
            import java.util.Collections.Inner;
            import java.util.List;
            import static java.util.*;
            import static java.util.Collections.Inner.something;
            import static java.util.Collections.singleton;
            import static java.util.Collections.*;
            import java.util.concurrent.ConcurrentHashMap;
            
            class Test {
            }
        """,
        after = """
            import java.io.File;
            /*~~>*/import java.util.Collections;
            /*~~>*/import java.util.Collections.Inner;
            /*~~>*/import java.util.List;
            /*~~>*/import static java.util.*;
            /*~~>*/import static java.util.Collections.Inner.something;
            /*~~>*/import static java.util.Collections.singleton;
            /*~~>*/import static java.util.Collections.*;
            /*~~>*/import java.util.concurrent.ConcurrentHashMap;
            
            class Test {
            }
        """
    )
}
