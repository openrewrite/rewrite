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
package org.openrewrite.java.format

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.java.style.TabsAndIndentsStyle
import org.openrewrite.style.NamedStyles

interface NormalizeTabsOrSpacesTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = NormalizeTabsOrSpaces()

    fun tabsAndIndents(with: TabsAndIndentsStyle.() -> TabsAndIndentsStyle = { this }) = listOf(
        NamedStyles(
            randomId(), "test", "test", "test", emptySet(), listOf(
                IntelliJ.tabsAndIndents().run { with(this) })
        )
    )

    @Test
    fun mixedToTabs(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
            public class Test {
            	public void test() {
                    int n = 1;
            	}
            }
        """,
        after = """
            public class Test {
            	public void test() {
            		int n = 1;
            	}
            }
        """
    )

    @Test
    fun mixedToSpaces(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                public void test() {
                	int n = 1;
                }
            }
        """,
        after = """
            public class Test {
                public void test() {
                    int n = 1;
                }
            }
        """
    )

    @Test
    fun tabsReplacedWithSpaces(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
            	int a;
             	int b;
              	int c;
               	int d;
            
                /*
            	 *
             	 *
              	 *
               	 *
                 */
            	
             	
              	
               	
                /**
            	 *
             	 *
              	 *
               	 *
                 */
                void test() {
                }
            }
        """,
        after = """
            public class Test {
                int a;
                int b;
                int c;
                int d;
            
                /*
                 *
                 *
                 *
                 *
                 */
                
                
                
                
                /**
                 *
                 *
                 *
                 *
                 */
                void test() {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/929")
    @Test
    fun doNotReplaceSpacesBeforeAsterisks(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
            /**
             * 
             */
            public class Test {
            	/*
            	 * Preserve `*`'s on
            	 * each new line.
            	 */
            	public class Inner {
            	}
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/928")
    @Test
    fun normalizeJavaDocSuffix(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
            	/** Test
            	 */
            	public class Inner {
            	}
            }
        """,
        after = """
            public class Test {
                /** Test
                 */
                public class Inner {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1237")
    @Test
    fun normalizeLastWhitespace(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
            public class Test {
            	public void test() {
            		int n = 1;
             }
            }
        """,
        after = """
            public class Test {
            	public void test() {
            		int n = 1;
            	}
            }
        """
    )
}
