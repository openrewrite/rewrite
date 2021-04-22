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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Issue("https://github.com/openrewrite/rewrite/issues/466")
interface ModifierOrderTest: JavaRecipeTest {
    override val recipe: Recipe?
        get() = ModifierOrder()

    @Test
    fun changeModifierOrder(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import org.openrewrite.internal.lang.Nullable;
            class Test {
                static /* comment */ public strictfp @Nullable transient Integer test() {
                }
            }
        """,
        after = """
            import org.openrewrite.internal.lang.Nullable;
            class Test {
                public /* comment */ static transient @Nullable strictfp Integer test() {
                }
            }
        """
    )

    @Test
    fun dontChangeOrderedModifiers(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                public static void main(String[] args) {
                }
            }
        """
    )
}
