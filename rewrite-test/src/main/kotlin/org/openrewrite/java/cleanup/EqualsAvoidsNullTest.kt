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
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.EqualsAvoidsNullStyle
import org.openrewrite.style.NamedStyles

@Suppress("ClassInitializerMayBeStatic", "StatementWithEmptyBody", "ConstantConditions")
interface EqualsAvoidsNullTest: JavaRecipeTest {
    override val recipe: Recipe?
        get() = EqualsAvoidsNull()

    @Test
    fun invertConditional(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    String s = null;
                    if(s.equals("test")) {}
                    if(s.equalsIgnoreCase("test")) {}
                }
            }
        """,
        after = """
            public class A {
                {
                    String s = null;
                    if("test".equals(s)) {}
                    if("test".equalsIgnoreCase(s)) {}
                }
            }
        """
    )

    @Test
    fun ignoreEqualsIgnoreCase(jp: JavaParser.Builder<*,*>) = assertChanged(
        jp.styles(listOf(
                NamedStyles(randomId(), "test", "", "", emptySet(), listOf(
                    EqualsAvoidsNullStyle(
                        true
                    )
                ))
        )).build(),
        before = """
            public class A {
                {
                    String s = null;
                    if(s.equals("test")) {}
                    if(s.equalsIgnoreCase("test")) {}
                }
            }
        """,
        after = """
            public class A {
                {
                    String s = null;
                    if("test".equals(s)) {}
                    if(s.equalsIgnoreCase("test")) {}
                }
            }
        """
    )

    @Test
    fun removeUnnecessaryNullCheckAndParens(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    String s = null;
                    if((s != null && s.equals("test"))) {}
                    if(s != null && s.equals("test")) {}
                    if(null != s && s.equals("test")) {}
                }
            }
        """,
        after = """
            public class A {
                {
                    String s = null;
                    if("test".equals(s)) {}
                    if("test".equals(s)) {}
                    if("test".equals(s)) {}
                }
            }
        """
    )
}
