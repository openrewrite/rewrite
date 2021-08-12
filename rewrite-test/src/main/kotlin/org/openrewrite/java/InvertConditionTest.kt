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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.java.tree.J

interface InvertConditionTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitIf(iff: J.If, p: ExecutionContext): J.If {
                    return iff.withIfCondition(InvertCondition.invert(iff.ifCondition, cursor))
                }
            }
        }

    @Suppress("StatementWithEmptyBody", "ConstantConditions", "InfiniteRecursion")
    @Test
    fun invertCondition(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                boolean a;
                boolean b;
            
                boolean test() {
                    if(!b) {}
                    if(b || a) {}
                    if(1 < 2) {}
                    if(b) {}
                    if((b)) {}
                    if(test()) {}
                    if(this.test()) {}
                    return true;
                }
            }
        """,
        after = """
            class Test {
                boolean a;
                boolean b;
            
                boolean test() {
                    if(b) {}
                    if(!(b || a)) {}
                    if(1 >= 2) {}
                    if(!b) {}
                    if(!(b)) {}
                    if(!test()) {}
                    if(!this.test()) {}
                    return true;
                }
            }
        """,
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )
}
