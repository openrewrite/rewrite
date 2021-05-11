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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.cleanup.ConstantNamesAreCapitalizedWithUnderscores

interface ConstantNamesAreCapitalizedWithUnderscoresTest : JavaRecipeTest {
    override val recipe: ConstantNamesAreCapitalizedWithUnderscores
        get() = ConstantNamesAreCapitalizedWithUnderscores()

    @Issue("https://github.com/openrewrite/rewrite/issues/462")
    @Test
    fun renameFieldAccess() = assertChanged(
        before = """
            class Base {
                public static final int renameMe = 1;
            }

            class Test extends Base {
                int foo() {
                    final int renameMe = Base.renameMe;
                    return renameMe + 1;
                }
            }
        """,
        after = """
            class Base {
                public static final int RENAME_ME = 1;
            }

            class Test extends Base {
                int foo() {
                    final int renameMe = Base.RENAME_ME;
                    return renameMe + 1;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/462")
    @Test
    fun renameVariable() = assertChanged(
        before = """
            class Test extends Base {
                public static final int renameMe = 1;

                int foo() {
                    final int n = renameMe;
                    return n + 1;
                }
            }
        """,
        after = """
            class Test extends Base {
                public static final int RENAME_ME = 1;

                int foo() {
                    final int n = RENAME_ME;
                    return n + 1;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/462")
    @Test
    fun renameEnumValues() = assertChanged(
        before =  """
            enum TestVal {
                valA, val_b
            }

            public class Test {
                void foo() {
                    TestVal valA = TestVal.valA;
                    TestVal val_b = TestVal.val_b;
                    if (val_b == TestVal.val_b){}
                }
            }
        """,
        after = """
            enum TestVal {
                VAL_A, VAL_B
            }

            public class Test {
                void foo() {
                    TestVal valA = TestVal.VAL_A;
                    TestVal val_b = TestVal.VAL_B;
                    if (val_b == TestVal.VAL_B){}
                }
            }
        """
    )
}
