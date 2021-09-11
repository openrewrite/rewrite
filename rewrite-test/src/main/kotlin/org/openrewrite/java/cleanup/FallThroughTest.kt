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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.FallThroughStyle
import org.openrewrite.style.NamedStyles

interface FallThroughTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = FallThrough()

    @Test
    fun addBreakWhenPreviousCaseHasCodeButLacksBreak(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                        i++;
                    case 99:
                        i++;
                    }
                }
            }
        """,
        after = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                        i++;
                        break;
                    case 99:
                        i++;
                    }
                }
            }
        """
    )

    @Test
    fun doNotAddBreakWhenPreviousCaseDoesNotContainCode(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                    case 99:
                        i++;
                    }
                }
            }
        """
    )

    @Test
    fun checkLastCaseGroupAddsBreakToLastCase(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(),
                    listOf(FallThroughStyle(true))
                )
            )
        ).build(),
        before = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                    case 99:
                        i++;
                    }
                }
            }
        """,
        after = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                    case 99:
                        i++;
                        break;
                    }
                }
            }
        """
    )

    @Test
    fun acceptableStatementsAreBreakOrReturnOrThrowOrContinue(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                        i++;
                        break;
                    case 1:
                        i++;
                        return;
                    case 2:
                        i++;
                        throw new Exception();
                    case 3:
                        i++;
                        continue;
                    }
                }
            }
        """
    )

    @Test
    fun reliefPatternExpectedMatchesVariations(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                        i++; // fall through
                    case 1:
                        i++; // falls through
                    case 2:
                        i++; // fallthrough
                    case 3:
                        i++; // fallthru
                    case 4:
                        i++; // fall-through
                    case 5:
                        i++; // fallthrough
                    case 99:
                        i++;
                        break;
                    }
                }
            }
        """
    )

    @Test
    fun handlesSwitchesWithOneOrNoneCases(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                public void noCase(int i) {
                    switch (i) {
                    }
                }
                
                public void oneCase(int i) {
                    switch (i) {
                        case 0:
                            i++;
                    }
                }
            }
        """
    )

    @Test
    fun addBreaksFallthroughCasesComprehensive(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                        i++; // fall through

                    case 1:
                        i++;
                        // falls through
                    case 2:
                    case 3: {{
                    }}
                    case 4: {
                        i++;
                    }
                    // fallthrough
                    case 5:
                        i++;
                    /* fallthru */case 6:
                        i++;
                        // fall-through
                    case 7:
                        i++;
                        break;
                    case 8: {
                        // fallthrough
                    }
                    case 9:
                        i++;
                    }
                }
            }
        """,
        after = """
            public class A {
                int i;
                {
                    switch (i) {
                    case 0:
                        i++; // fall through

                    case 1:
                        i++;
                        // falls through
                    case 2:
                    case 3: {{
                        break;
                    }}
                    case 4: {
                        i++;
                    }
                    // fallthrough
                    case 5:
                        i++;
                    /* fallthru */case 6:
                        i++;
                        // fall-through
                    case 7:
                        i++;
                        break;
                    case 8: {
                        // fallthrough
                    }
                    case 9:
                        i++;
                    }
                }
            }
        """
    )


}
