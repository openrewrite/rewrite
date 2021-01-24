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
package org.openrewrite.java.format

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.java.style.TabsAndIndentsStyle
import org.openrewrite.style.NamedStyles

interface TabsAndIndentsTest2 : RecipeTest {
    override val recipe: Recipe
        get() = TabsAndIndentsProcessor2<ExecutionContext>(IntelliJ.tabsAndIndents())
            .toRecipe()

    @Test
    fun methodWithAnnotation(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.build(),
        before = """
            class Test {
            @Nullable
            Consumer<Throwable> getOnError() {
            return onError;
            }
            }
        """,
        after = """
            class Test {
                @Nullable
                Consumer<Throwable> getOnError() {
                    return onError;
                }
            }
        """
    )
}
