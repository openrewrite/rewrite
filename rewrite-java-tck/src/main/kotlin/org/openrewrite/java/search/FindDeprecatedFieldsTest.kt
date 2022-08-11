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

interface FindDeprecatedFieldsTest : JavaRecipeTest {
    val deprecations: Array<String>
        @Language("java") get() = arrayOf(
            """
                package org.old.types;
                public class D {
                    @Deprecated
                    public static final String FIELD = "test";
                }
            """.trimIndent()
        )

    @Test
    fun ignoreDeprecationsInDeprecatedMethod(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = FindDeprecatedFields("org.old.types..*", true),
        dependsOn = deprecations,
        before = """
            import org.old.types.D;
            class Test {
                @Deprecated
                void test(int n) {
                    System.out.println(D.FIELD);
                }
            }
        """
    )

    @Test
    fun ignoreDeprecationsInDeprecatedClass(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = FindDeprecatedFields("org.old.types..*", true),
        dependsOn = deprecations,
        before = """
            import org.old.types.D;

            @Deprecated
            class Test {
                void test(int n) {
                    System.out.println(D.FIELD);
                }
            }
        """
    )

    @Test
    fun findDeprecations(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = FindDeprecatedFields("org.old.types..*", false),
        dependsOn = deprecations,
        before = """
            import org.old.types.D;
            class Test {
                void test(int n) {
                    System.out.println(D.FIELD);
                }
            }
        """,
        after = """
            import org.old.types.D;
            class Test {
                void test(int n) {
                    System.out.println(D./*~~>*/FIELD);
                }
            }
        """
    )
}
