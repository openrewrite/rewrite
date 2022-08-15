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

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface FindDeprecatedClassesTest : JavaRecipeTest {
    val deprecations: Array<String>
        @Language("java") get() = arrayOf(
            """
                package org.old.types;
                @Deprecated public class D {}
            """.trimIndent(),
            """
                package org.old.types;
                public class E extends D {}
            """.trimIndent()
        )

    @Test
    fun ignoreDeprecationsInDeprecatedMethod(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = FindDeprecatedClasses("org.old..*", false, true),
        dependsOn = deprecations,
        before = """
            import org.old.types.D;
            class Test {
                @Deprecated
                void test(int n) {
                    new D();
                }
            }
        """
    )

    @Test
    fun ignoreDeprecationsInDeprecatedClass(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = FindDeprecatedClasses("org.old..*", false, true),
        dependsOn = deprecations,
        before = """
            import org.old.types.D;
            @Deprecated
            class Test {
                D d;
            }
        """
    )

    @Test
    fun findDeprecations(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = FindDeprecatedClasses("org.old..*", false, true),
        dependsOn = deprecations,
        before = """
            import org.old.types.D;
            class Test {
                D d;
            }
        """,
        after = """
            import org.old.types.D;
            class Test {
                /*~~>*/D d;
            }
        """
    )

    @Test
    fun findTypesInheritingFromDeprecations(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = FindDeprecatedClasses("org.old..*", true, null),
        dependsOn = deprecations,
        before = """
            import org.old.types.D;
            class Test {
                D d;
            }
        """,
        after = """
            import org.old.types.D;
            class Test {
                /*~~>*/D d;
            }
        """
    )
}
